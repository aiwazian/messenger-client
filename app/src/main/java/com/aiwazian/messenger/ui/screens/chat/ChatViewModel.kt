/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.usecase.JoinChannelUseCase
import com.aiwazian.messenger.usecase.JoinGroupUseCase
import com.aiwazian.messenger.usecase.JoinViaInviteLinkUseCase
import com.aiwazian.messenger.usecase.LeaveChatUseCase
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import com.aiwazian.messenger.utils.AudioRecorderManager
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.LastSeenHelper
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.UploadManager
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val searchRepository: SearchRepository,
    private val clipboardService: ClipboardService,
    private val webSocketClient: WebSocketClient,
    private val downloaderManager: DownloaderManager,
    private val uploadManager: UploadManager,
    private val vibrationManager: VibrationManager,
    private val fileHandler: FileHandler,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val joinGroupUseCase: JoinGroupUseCase,
    private val joinViaInviteLinkUseCase: JoinViaInviteLinkUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val dataStoreManager: DataStoreManager,
    private val voicePlayerManager: VoicePlayerManager,
    private val onlineUsersTracker: OnlineUsersTracker
) : ViewModel() {
    
    private val audioRecorderManager = AudioRecorderManager(context)
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    
    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChatUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private var isFirstLoadDone = false
    private val limitFlow = MutableStateFlow(30)
    
    private var isInit = false
    private var autoDownloadMedia = false
    private var autoDownloadPhotos = true
    private var autoDownloadVideos = true
    private var autoDownloadFiles = true
    
    init {
        viewModelScope.launch {
            autoDownloadMedia = dataStoreManager.getAutoDownloadMedia().firstOrNull() ?: false
            autoDownloadPhotos = dataStoreManager.getAutoDownloadPhotos().firstOrNull() ?: true
            autoDownloadVideos = dataStoreManager.getAutoDownloadVideos().firstOrNull() ?: true
            autoDownloadFiles = dataStoreManager.getAutoDownloadFiles().firstOrNull() ?: true
        }
        viewModelScope.launch {
            dataStoreManager.getVideoLooping().collect { isLooping ->
                _uiState.update { it.copy(isVideoLooping = isLooping) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getVideoPlaybackSpeed().collect { speed ->
                _uiState.update { it.copy(videoPlaybackSpeed = speed) }
            }
        }
        
        voicePlayerManager.connect()
        viewModelScope.launch {
            voicePlayerManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        currentPlayingVoiceFileId = state.currentFileId,
                        isVoicePlaying = state.isPlaying,
                        voicePositionMs = state.positionMs,
                        voiceDurationMs = state.durationMs
                    )
                }
            }
        }
        viewModelScope.launch {
            _uiState.collect { state ->
                val playingId = state.currentPlayingVoiceFileId ?: return@collect
                val queue = buildVoiceQueue(state.chatItems, state.chatName)
                if (queue.any { it.fileId == playingId }) {
                    voicePlayerManager.updateQueue(queue)
                }
            }
        }
    }
    
    fun init(chatId: Long, chatName: String? = null, avatarUri: Uri? = null) {
        if (isInit) return
        isInit = true
        
        _uiState.update {
            it.copy(
                chatId = chatId,
                chatName = UiText.DynamicString(chatName.orEmpty()),
                avatarUri = avatarUri
            )
        }
        isFirstLoadDone = false
        limitFlow.value = 50
        
        webSocketClient.emitEvent("chat_open", mapOf("chatId" to chatId.toString()))
        
        viewModelScope.launch {
            when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> channelRepository.fetchById(chatId)
                ChatType.GROUP -> groupRepository.fetchById(chatId)
                ChatType.PRIVATE -> userRepository.fetchById(chatId)
                else -> {}
            }
        }
        
        setupUserObserver()
        loadChatData()
    }
    
    private fun setupUserObserver() {
        viewModelScope.launch {
            userRepository.getMe().firstOrNull()?.let { user ->
                _uiState.update { it.copy(myId = user.id) }
            }
        }
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state == ConnectionState.CONNECTED) }
            }
        }
    }
    
    private fun getRawMessages(): List<Message> {
        return _uiState.value.chatItems.filterIsInstance<ChatItem.MessageItem>().map { it.message }
    }
    
    private fun loadChatData() {
        _uiState.update { it.copy(isLoading = true) }
        
        when (ChatType.fromId(_uiState.value.chatId)) {
            ChatType.CHANNEL -> {
                viewModelScope.launch {
                    channelRepository.getById(_uiState.value.chatId).collectLatest { channel ->
                        _uiState.update {
                            it.copy(
                                chatName = UiText.DynamicString(channel.name),
                                subTitle = UiText.PluralResource(
                                    R.plurals.subscribers_count,
                                    channel.subscribers,
                                    channel.subscribers
                                ),
                                isJoined = channel.isSubscribed,
                                isOwner = channel.ownerId == _uiState.value.myId,
                                avatarUri = channel.avatars.firstOrNull()?.uri,
                                topBarActions = if (channel.ownerId == _uiState.value.myId) {
                                    listOf(
                                        TopBarAction(
                                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                                DropdownMenuAction(
                                                    Icons.Outlined.CleaningServices,
                                                    UiText.StringResource(R.string.clear_history),
                                                    ::showClearHistoryDialog
                                                )
                                            )
                                        )
                                    )
                                } else if (channel.isSubscribed) {
                                    listOf(
                                        TopBarAction(
                                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                                DropdownMenuAction(
                                                    Icons.AutoMirrored.Rounded.Logout,
                                                    UiText.StringResource(R.string.leave_channel),
                                                    ::showLeaveDialog
                                                )
                                            )
                                        )
                                    )
                                } else {
                                    emptyList()
                                }
                            )
                        }
                    }
                }
            }
            
            ChatType.GROUP -> {
                viewModelScope.launch {
                    groupRepository.getById(_uiState.value.chatId).collectLatest { group ->
                        group.let {
                            _uiState.update {
                                it.copy(
                                    chatName = UiText.DynamicString(group.name),
                                    subTitle = UiText.PluralResource(
                                        R.plurals.members_count,
                                        group.members,
                                        group.members
                                    ),
                                    isJoined = group.isMember,
                                    isOwner = group.ownerId == _uiState.value.myId,
                                    avatarUri = group.avatars.firstOrNull()?.uri,
                                    topBarActions = if (group.ownerId == _uiState.value.myId) {
                                        listOf(
                                            TopBarAction(
                                                icon = Icons.Rounded.MoreVert,
                                                dropdownActions = listOf(
                                                    DropdownMenuAction(
                                                        Icons.Outlined.CleaningServices,
                                                        UiText.StringResource(R.string.clear_history),
                                                        ::showClearHistoryDialog
                                                    )
                                                )
                                            )
                                        )
                                    } else {
                                        listOf(
                                            TopBarAction(
                                                icon = Icons.Rounded.MoreVert,
                                                dropdownActions = listOf(
                                                    DropdownMenuAction(
                                                        Icons.AutoMirrored.Rounded.Logout,
                                                        UiText.StringResource(R.string.leave_group),
                                                        ::showLeaveDialog
                                                    )
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            ChatType.PRIVATE -> {
                viewModelScope.launch {
                    if (_uiState.value.chatId == _uiState.value.myId) {
                        userRepository.getMe().collectLatest { user ->
                            _uiState.update {
                                it.copy(
                                    chatName = UiText.StringResource(R.string.saved_messages),
                                    subTitle = UiText.DynamicString(""),
                                    avatarUri = user.avatars.firstOrNull()?.uri,
                                    topBarActions = listOf(
                                        TopBarAction(
                                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                                DropdownMenuAction(
                                                    Icons.Outlined.CleaningServices,
                                                    UiText.StringResource(R.string.clear_history),
                                                    ::showClearHistoryDialog
                                                ),
                                                DropdownMenuAction(
                                                    Icons.Rounded.DeleteOutline,
                                                    UiText.StringResource(R.string.delete_chat),
                                                    ::showDeleteChatDialog,
                                                    isDestructive = true
                                                )
                                            )
                                        )
                                    )
                                )
                            }
                        }
                    } else {
                        combine(
                            userRepository.getById(_uiState.value.chatId),
                            onlineUsersTracker.onlineUsers
                        ) { user, onlineUsers ->
                            user to onlineUsers.contains(user.id)
                        }.collectLatest { (user, isOnline) ->
                            val subTitle =
                                LastSeenHelper.getSubtitle(context, isOnline, user.lastSeen)
                            _uiState.update {
                                it.copy(
                                    chatName = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = subTitle,
                                    avatarUri = user.avatars.firstOrNull()?.uri,
                                    topBarActions = listOf(
                                        TopBarAction(
                                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                                DropdownMenuAction(
                                                    Icons.Outlined.CleaningServices,
                                                    UiText.StringResource(R.string.clear_history),
                                                    ::showClearHistoryDialog
                                                ),
                                                DropdownMenuAction(
                                                    Icons.Rounded.DeleteOutline,
                                                    UiText.StringResource(R.string.delete_chat),
                                                    ::showDeleteChatDialog,
                                                    isDestructive = true
                                                )
                                            )
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            else -> {}
        }
        
        viewModelScope.launch {
            val userId = userRepository.getMe().first().id
            limitFlow.collectLatest { limit ->
                chatRepository.getMessagesFlow(userId, _uiState.value.chatId, limit, 0)
                    .collect { messages ->
                        updateChatItems(messages)
                        _uiState.update { it.copy(isLoading = false, isLoadingMore = false) }
                        if (messages.isNotEmpty() && !isFirstLoadDone) {
                            isFirstLoadDone = true
                            _uiState.update { it.copy(isFirstLoadDone = true) }
                            _uiEffect.emit(ChatUiEffect.ScrollToBottom(_uiState.value.chatItems.lastIndex))
                        } else if (messages.isEmpty() && !isFirstLoadDone) {
                            isFirstLoadDone = true
                            _uiState.update { it.copy(isFirstLoadDone = true) }
                        }
                    }
            }
        }
        
        viewModelScope.launch {
            chatRepository.getMessages(
                chatId = _uiState.value.chatId, limit = 50, offset = 0
            ).onSuccess { freshMessages ->
                if (freshMessages.size < 50) {
                    _uiState.update { it.copy(hasMoreMessages = false) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure {
                Log.e("ChatViewModel", "Error fetching fresh messages", it)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun loadMoreMessages() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreMessages || state.isLoading) return
        
        _uiState.update { it.copy(isLoadingMore = true) }
        
        viewModelScope.launch {
            val offset = limitFlow.value
            chatRepository.getMessages(
                _uiState.value.chatId, limit = 50, offset = offset
            ).onSuccess { moreMessages ->
                if (moreMessages.isEmpty()) {
                    _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = false) }
                } else {
                    if (moreMessages.size < 50) {
                        _uiState.update { it.copy(hasMoreMessages = false) }
                    }
                    limitFlow.value += moreMessages.size
                }
                _uiState.update { it.copy(isLoadingMore = false) }
            }.onFailure {
                Log.e("ChatViewModel", "Error loading more messages", it)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
    
    private fun updateChatItems(messages: List<Message>) {
        val myId = _uiState.value.myId
        val chatItems = mutableListOf<ChatItem>()
        var lastDate: java.time.LocalDate? = null
        var lastSenderId: Long? = null
        val newMediaItems = mutableListOf<MessageAttachment>()
        
        messages.forEach { message ->
            val messageDate =
                message.sendTime.toInstance().atZone(ZoneId.systemDefault()).toLocalDate()
            
            if (lastDate == null || !messageDate.isEqual(lastDate)) {
                val monthName = messageDate.month.getDisplayName(
                    TextStyle.FULL, Locale.getDefault()
                )
                val capitalizedMonthName = monthName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                chatItems.add(ChatItem.DateSeparator("${messageDate.dayOfMonth} $capitalizedMonthName"))
                lastDate = messageDate
            }
            
            if (message.messageType == MessageType.SYSTEM && message.systemMessageEventType != null) {
                val textResId = when (message.systemMessageEventType) {
                    SystemMessageEventType.CHANNEL_CREATED -> R.string.channel_created
                    SystemMessageEventType.GROUP_CREATED -> R.string.group_created
                    SystemMessageEventType.HISTORY_CLEARED -> R.string.history_cleared
                }
                chatItems.add(
                    ChatItem.SystemMessage(
                        text = UiText.StringResource(resId = textResId),
                        sendTime = message.sendTime
                    )
                )
                return@forEach
            }
            
            val isMine =
                message.senderId == myId && ChatType.fromId(_uiState.value.chatId) != ChatType.CHANNEL
            val isSingleEmoji = isSingleEmoji(message.text ?: "")
            val isFirstInGroup = message.senderId != lastSenderId
            val chatType = ChatType.fromId(_uiState.value.chatId)
            
            val actions = mutableListOf<DropdownMenuAction>()
            if (!message.text.isNullOrBlank()) {
                actions.add(
                    DropdownMenuAction(
                        Icons.Rounded.ContentCopy,
                        UiText.StringResource(R.string.copy),
                        onClick = { copyToClipboard(message.text) })
                )
            }
            
            val canDelete = when (chatType) {
                ChatType.PRIVATE -> true
                ChatType.CHANNEL, ChatType.GROUP -> _uiState.value.isOwner
                else -> false
            }
            
            if (canDelete) {
                actions.add(
                    DropdownMenuAction(
                        Icons.Rounded.DeleteOutline,
                        UiText.StringResource(R.string.delete),
                        onClick = {
                            showDeleteMessageDialog()
                            selectMessage(message)
                        },
                        isDestructive = true
                    )
                )
            }
            
            val updatedAttachments = message.attachments.map { attachment ->
                if (attachment.localUri != null) {
                    val mimeType = attachment.localUri.getFileType(context)
                    val newType = when {
                        mimeType == "image/gif" -> AttachmentType.GIF
                        mimeType.startsWith("image/") -> AttachmentType.IMAGE
                        mimeType.startsWith("video/") -> AttachmentType.VIDEO
                        mimeType.startsWith("audio/") -> AttachmentType.VOICE
                        else -> attachment.type
                    }
                    attachment.copy(type = newType)
                } else {
                    attachment
                }
            }
            
            val updatedMessage = message.copy(attachments = updatedAttachments)
            
            updatedMessage.attachments.sortedBy { it.sortOrder }.forEach { attachment ->
                if (attachment.type == AttachmentType.IMAGE || attachment.type == AttachmentType.VIDEO || attachment.type == AttachmentType.GIF) {
                    newMediaItems.add(attachment)
                }
                
                if ((attachment.status == DownloadStatus.IDLE || attachment.status == DownloadStatus.UPLOADED) && autoDownloadMedia) {
                    val shouldDownload = when (attachment.type) {
                        AttachmentType.VOICE -> true
                        AttachmentType.IMAGE, AttachmentType.GIF -> autoDownloadPhotos
                        AttachmentType.VIDEO -> autoDownloadVideos && attachment.size <= 10 * 1024 * 1024
                        AttachmentType.FILE -> autoDownloadFiles && attachment.size <= 10 * 1024 * 1024
                    }
                    
                    if (shouldDownload) {
                        onFileAction(updatedMessage, attachment, FileAction.DOWNLOAD)
                    }
                }
            }
            
            chatItems.add(
                ChatItem.MessageItem(
                    message = updatedMessage,
                    time = updatedMessage.sendTime.toInstance().toPrettyTime(),
                    isMine = isMine,
                    isRead = if (isMine) updatedMessage.isRead else null,
                    senderName = if (!isMine && ChatType.fromId(updatedMessage.chatId) == ChatType.GROUP) {
                        _uiState.value.userNamesCache[updatedMessage.senderId].also {
                            if (it == null) loadUserName(updatedMessage.senderId)
                        }
                    } else null,
                    isFirstInGroup = isFirstInGroup,
                    isSingleEmoji = isSingleEmoji,
                    dropdownActions = actions))
            
            lastSenderId = message.senderId
        }
        _uiState.update { it.copy(chatItems = chatItems, mediaItems = newMediaItems) }
    }
    
    fun changeText(newText: String) {
        _uiState.update { it.copy(messageText = newText) }
    }
    
    fun onSendMessageClicked() {
        viewModelScope.launch {
            val text = _uiState.value.messageText
            if (text.isBlank()) return@launch
            
            val validText = text.trim()
            
            changeText("")
            
            try {
                sendMessageUseCase(_uiState.value.chatId, validText)?.let {
                    _uiEffect.emit(ChatUiEffect.ScrollToBottom(_uiState.value.chatItems.lastIndex))
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
            }
        }
    }
    
    fun onJoinClicked() {
        viewModelScope.launch {
            val chatId = _uiState.value.chatId
            when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> {
                    joinChannelUseCase(chatId).onSuccess {
                        _uiState.update { it.copy(isJoined = true) }
                    }
                }
                
                ChatType.GROUP -> {
                    joinGroupUseCase(chatId).onSuccess {
                        _uiState.update { it.copy(isJoined = true) }
                    }
                }
                
                else -> {}
            }
        }
    }
    
    fun onLeaveClicked() {
        viewModelScope.launch {
            leaveChatUseCase(_uiState.value.chatId).onSuccess {
                hideLeaveDialog()
                _uiEffect.emit(ChatUiEffect.NavigateToMain)
            }
        }
    }
    
    fun showDeleteChatDialog() =
        _uiState.update { it.copy(showDeleteChatDialog = true, deleteForRecipient = false) }
    
    fun hideDeleteChatDialog() =
        _uiState.update { it.copy(showDeleteChatDialog = false, deleteForRecipient = false) }
    
    fun showClearHistoryDialog() =
        _uiState.update { it.copy(showClearHistoryDialog = true, deleteForRecipient = false) }
    
    fun hideClearHistoryDialog() =
        _uiState.update { it.copy(showClearHistoryDialog = false, deleteForRecipient = false) }
    
    fun showDeleteMessageDialog() {
        _uiState.update { it.copy(showDeleteMessageDialog = true, deleteForRecipient = false) }
    }
    
    fun hideDeleteMessageDialog() {
        _uiState.update { it.copy(showDeleteMessageDialog = false, deleteForRecipient = false) }
    }
    
    fun setDeleteForRecipient(delete: Boolean) {
        _uiState.update { it.copy(deleteForRecipient = delete) }
    }
    
    fun showLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = true) }
    
    fun hideLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = false) }
    
    fun vibrate() {
        vibrationManager.vibrate(VibrationPattern.Error)
    }
    
    fun onDeleteChatConfirmed() {
        viewModelScope.launch {
            val deleteForRecipient = _uiState.value.deleteForRecipient
            if (chatRepository.deleteChat(_uiState.value.chatId, deleteForRecipient)) {
                hideDeleteChatDialog()
                _uiEffect.emit(ChatUiEffect.NavigateToMain)
            }
        }
    }
    
    fun onDeleteMessagesConfirmed() {
        viewModelScope.launch {
            val deleteForRecipient = _uiState.value.deleteForRecipient
            if (chatRepository.deleteChatMessages(_uiState.value.chatId, deleteForRecipient)) {
                hideClearHistoryDialog()
            }
        }
    }
    
    fun onDeleteMessageConfirmed() {
        viewModelScope.launch {
            val deleteForRecipient = _uiState.value.deleteForRecipient
            _uiState.value.selectedMessages.forEach { message ->
                chatRepository.deleteMessage(_uiState.value.chatId, message.id, deleteForRecipient)
            }
            _uiState.update { it.copy(selectedMessages = emptySet()) }
            hideDeleteMessageDialog()
        }
    }
    
    fun selectMessage(message: Message) =
        _uiState.update { it.copy(selectedMessages = it.selectedMessages + message) }
    
    fun unselectMessage(message: Message) =
        _uiState.update { it.copy(selectedMessages = it.selectedMessages - message) }
    
    fun copyToClipboard(text: String?) = text?.let { clipboardService.copy(it) }
    
    fun cancelUpload(tempMessageId: Long) {
        viewModelScope.launch {
            val tempMessage = getRawMessages().find { it.id == tempMessageId }
            tempMessage?.attachments?.forEach { attachment ->
                uploadManager.cancel(attachment.fileId)
            }
            
            chatRepository.deleteLocalMessage(messageId = tempMessageId)
        }
    }
    
    fun onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
        when (action) {
            FileAction.DOWNLOAD -> {
                viewModelScope.launch {
                    chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
                        .onSuccess { url ->
                            downloaderManager.download(
                                url = url,
                                fileName = file.name,
                                fileId = file.fileId
                            )
                        }
                        .onFailure {
                            Log.e(
                                "ChatViewModel",
                                "Download URL is null for file: ${file.fileId}, message: ${message.id}, chat: ${message.chatId}"
                            )
                        }
                }
            }
            
            FileAction.PAUSE -> {
                viewModelScope.launch {
                    downloaderManager.pause(file.fileId)
                }
            }
            
            FileAction.RESUME -> {
                viewModelScope.launch {
                    downloaderManager.resume(file.fileId)
                }
            }
            
            FileAction.CANCEL -> {
                viewModelScope.launch {
                    downloaderManager.cancel(file.fileId)
                }
            }
            
            FileAction.OPEN -> {
                if (file.type == AttachmentType.IMAGE || file.type == AttachmentType.VIDEO || file.type == AttachmentType.GIF) {
                    val index = _uiState.value.mediaItems.indexOfFirst { it.fileId == file.fileId }
                    _uiState.update {
                        it.copy(
                            showFullScreenViewer = true,
                            initialMediaIndex = if (index >= 0) index else 0
                        )
                    }
                } else {
                    viewModelScope.launch {
                        fileHandler.openFile(path = file.localUri.toString())
                    }
                }
            }
            
            FileAction.PLAY -> {
                if (file.type != AttachmentType.VOICE) return
                file.localUri ?: return
                
                val currentPlayingId = _uiState.value.currentPlayingVoiceFileId
                if (currentPlayingId == file.fileId) {
                    voicePlayerManager.togglePlayPause()
                } else {
                    val startPos = pendingVoiceStartPositions.remove(file.fileId) ?: 0
                    playVoice(file.fileId, startPos)
                }
            }
        }
    }
    
    private fun playVoice(fileId: String, startPositionMs: Int = 0) {
        val state = _uiState.value
        val queue = buildVoiceQueue(state.chatItems, state.chatName)
        if (queue.none { it.fileId == fileId }) return
        voicePlayerManager.play(queue, fileId, startPositionMs)
    }
    
    private fun buildVoiceQueue(items: List<ChatItem>, chatName: UiText): List<VoiceQueueItem> {
        val title = chatName.asString(context).ifBlank { "Voice message" }
        val artworkUri = _uiState.value.avatarUri
        return items.filterIsInstance<ChatItem.MessageItem>()
            .flatMap { it.message.attachments }
            .filter { it.type == AttachmentType.VOICE }
            .map { attachment ->
                val isReady = attachment.localUri != null &&
                        (attachment.status == DownloadStatus.COMPLETED || attachment.status == DownloadStatus.UPLOADED)
                VoiceQueueItem(
                    uri = if (isReady) attachment.localUri else null,
                    fileId = attachment.fileId,
                    title = title,
                    subtitle = context.getString(R.string.voice_message),
                    artworkUri = artworkUri
                )
            }
    }
    
    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) {
        val currentPlayingId = _uiState.value.currentPlayingVoiceFileId
        
        if (currentPlayingId != file.fileId) {
            pendingVoiceStartPositions[file.fileId] = positionMs
            return
        }
        
        voicePlayerManager.seekTo(positionMs)
    }
    
    fun sendFiles(uris: List<Uri>) {
        viewModelScope.launch {
            sendMessageWithFilesUseCase(_uiState.value.chatId, uris, null)
        }
    }
    
    fun markAsReadMessage(message: Message) {
        if (message.senderId == _uiState.value.myId || message.isRead) return
        viewModelScope.launch {
            if (chatRepository.makeAsRead(_uiState.value.chatId, message.id)) {
                readMessage(message.id)
            }
        }
    }
    
    fun loadUserName(userId: Long) {
        if (_uiState.value.userNamesCache.containsKey(userId)) return
        viewModelScope.launch {
            try {
                userRepository.getById(userId).collect { user ->
                    val name = "${user.firstName} ${user.lastName.orEmpty()}".trim()
                    _uiState.update { it.copy(userNamesCache = it.userNamesCache + (userId to name)) }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading user name", e)
            }
        }
    }
    
    private fun readMessage(id: Long) {
        // TODO
    }
    
    private fun isSingleEmoji(text: String): Boolean {
        val emojiRegex =
            Regex("^[\\p{So}\\p{Cntrl}\\p{InEmoticons}\\p{InMiscellaneousSymbolsAndPictographs}\\p{InSupplementalSymbolsAndPictographs}\\uD83C\\uDFF0-\\uD83D\\uDFFF]+$")
        return emojiRegex.matches(text.trim())
    }
    
    fun onLinkClicked(url: String) {
        val inviteLinkRegex = RegexPatterns.INVITE_LINK
        val match = inviteLinkRegex.find(url)
        
        if (match == null) {
            val normalizedUrl =
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            viewModelScope.launch {
                _uiEffect.emit(ChatUiEffect.OpenUrl(normalizedUrl))
            }
            return
        }
        
        val code = match.groupValues[2]
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            
            inviteLinkRepository.getInviteLinkInfo(code).onSuccess { linkInfo ->
                if (_uiState.value.chatId == linkInfo.chatId) {
                    _uiState.update { it.copy(isProcessingInvite = false) }
                    _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.you_are_already_in_this_chat)))
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else if (linkInfo.isJoined != null) {
                    _uiState.update { it.copy(isProcessingInvite = false) }
                    _uiEffect.emit(ChatUiEffect.NavigateToChat(linkInfo.chatId))
                } else if (linkInfo.isBanned != null) {
                    _uiState.update {
                        it.copy(
                            showBannedDialog = true,
                            isProcessingInvite = false
                        )
                    }
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else {
                    _uiState.update {
                        it.copy(
                            inviteLinkInfo = linkInfo,
                            inviteLinkCode = code,
                            showInviteBottomSheet = true,
                            isProcessingInvite = false
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isProcessingInvite = false) }
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.invalid_link)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun onUsernameClicked(username: String) {
        viewModelScope.launch {
            val cleanUsername = username.removePrefix("@")
            searchRepository.resolveUsername(cleanUsername).onSuccess { result ->
                if (result == null) {
                    _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.chat_not_found)))
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else if (result.isBanned) {
                    _uiState.update { it.copy(showBannedDialog = true) }
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else {
                    _uiEffect.emit(ChatUiEffect.NavigateToChat(result.chatId))
                }
            }.onFailure {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.error_searching_for_chat)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun dismissBannedDialog() {
        _uiState.update { it.copy(showBannedDialog = false) }
    }
    
    fun onSubscribeViaInviteLink() {
        val info = _uiState.value.inviteLinkInfo ?: return
        val code = _uiState.value.inviteLinkCode ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            
            val result = joinViaInviteLinkUseCase(code, info.chatId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isProcessingInvite = false,
                        showInviteBottomSheet = false,
                        inviteLinkInfo = null,
                        inviteLinkCode = null
                    )
                }
                _uiEffect.emit(ChatUiEffect.NavigateToChat(info.chatId))
            } else {
                _uiState.update { it.copy(isProcessingInvite = false) }
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_join)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun dismissInviteBottomSheet() {
        _uiState.update {
            it.copy(
                showInviteBottomSheet = false,
                inviteLinkInfo = null,
                inviteLinkCode = null,
                isProcessingInvite = false
            )
        }
    }
    
    fun onMicrophonePermissionDenied() {
        _uiState.update { it.copy(showMicrophonePermissionSheet = true) }
    }
    
    fun dismissMicrophonePermissionSheet() {
        _uiState.update { it.copy(showMicrophonePermissionSheet = false) }
    }
    
    fun clearMediaUrl() {
        _uiState.update { it.copy(showFullScreenViewer = false) }
    }
    
    fun setVideoLooping(isLooping: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveVideoLooping(isLooping)
        }
    }
    
    fun setVideoPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            dataStoreManager.saveVideoPlaybackSpeed(speed)
        }
    }
    
    fun saveToGallery(uri: Uri) {
        viewModelScope.launch {
            val path = uri.path ?: uri.toString()
            val success = fileHandler.saveToGallery(path)
            if (success) {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.successfully_saved_to_gallery)))
            } else {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_to_gallery)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun startRecording() {
        if (_uiState.value.isRecording) return
        val file = audioRecorderManager.startRecording()
        if (file != null) {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    isRecordingLocked = false,
                    recordingDurationMs = 0L,
                    recordingAmplitude = 0f
                )
            }
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (true) {
                    delay(100.milliseconds)
                    val maxAmplitude = audioRecorderManager.getMaxAmplitude()
                    val amplitude = (maxAmplitude / 32767f).coerceIn(0f, 1f)
                    _uiState.update {
                        it.copy(
                            recordingDurationMs = System.currentTimeMillis() - startTime,
                            recordingAmplitude = amplitude
                        )
                    }
                }
            }
            vibrationManager.vibrate(VibrationPattern.TactileResponse)
        } else {
            viewModelScope.launch {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.DynamicString("Не удалось начать запись аудио")))
            }
        }
    }
    
    fun lockRecording() {
        if (_uiState.value.isRecording) {
            _uiState.update { it.copy(isRecordingLocked = true) }
            vibrationManager.vibrate(VibrationPattern.TactileResponse)
        }
    }
    
    fun stopRecordingAndSend() {
        if (!_uiState.value.isRecording) return
        val file = audioRecorderManager.stopRecording()
        recordingTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isRecording = false,
                isRecordingLocked = false,
                recordingDurationMs = 0L
            )
        }
        if (file != null) {
            sendFiles(listOf(Uri.fromFile(file)))
        }
    }
    
    fun cancelRecording() {
        if (!_uiState.value.isRecording) return
        audioRecorderManager.cancelRecording()
        recordingTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isRecording = false,
                isRecordingLocked = false,
                recordingDurationMs = 0L
            )
        }
        vibrationManager.vibrate(VibrationPattern.TactileResponse)
    }
}
