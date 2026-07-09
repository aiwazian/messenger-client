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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.push.NotificationHelper
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.RealtimeEventSyncService
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
    private val onlineUsersTracker: OnlineUsersTracker,
    private val realtimeEventSyncService: RealtimeEventSyncService,
    private val notificationHelper: NotificationHelper
) : ViewModel() {
    
    private val audioRecorderManager = AudioRecorderManager(context)
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    
    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    private val sendingJobs = mutableMapOf<Long, kotlinx.coroutines.Job>()
    
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
        loadSettings()
        observeVoicePlayer()
        observeQueueUpdates()
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
        
        notificationHelper.clearChatNotifications(chatId)
        webSocketClient.emitEvent("chat_open", mapOf("chatId" to chatId.toString()))
        
        setupUserObserver()
        loadChatInfo()
        observeRealtimeEvents()
        observeMessages()
    }
    
    // region Initialization & Settings
    private fun loadSettings() {
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
    }
    
    private fun observeVoicePlayer() {
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
    }
    
    private fun observeQueueUpdates() {
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
    // endregion
    
    // region Chat Data Loading
    private fun loadChatInfo() {
        val chatId = _uiState.value.chatId
        viewModelScope.launch {
            when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> loadChannelInfo(chatId)
                ChatType.GROUP -> loadGroupInfo(chatId)
                ChatType.PRIVATE -> loadUserInfo(chatId)
                else -> {}
            }
        }
    }
    
    private suspend fun loadChannelInfo(chatId: Long) {
        viewModelScope.launch {
            channelRepository.fetchById(chatId)
        }
        channelRepository.getById(chatId).collectLatest { channel ->
            _uiState.update {
                it.copy(
                    chatName = UiText.DynamicString(channel.name),
                    subTitle = UiText.PluralResource(
                        R.plurals.subscribers_count,
                        channel.subscribers,
                        channel.subscribers
                    ),
                    isJoined = channel.isSubscribed,
                    isOwner = channel.ownerId == it.myId,
                    avatarUri = channel.avatars.firstOrNull()?.uri,
                    topBarActions = createTopBarActions(
                        channel.ownerId == it.myId,
                        channel.isSubscribed,
                        ChatType.CHANNEL
                    )
                )
            }
        }
    }
    
    private suspend fun loadGroupInfo(chatId: Long) {
        viewModelScope.launch {
            groupRepository.fetchById(chatId)
            chatRepository.markAllAsRead(chatId)
        }
        groupRepository.getById(chatId).collectLatest { group ->
            _uiState.update {
                it.copy(
                    chatName = UiText.DynamicString(group.name),
                    subTitle = UiText.PluralResource(
                        R.plurals.members_count,
                        group.members,
                        group.members
                    ),
                    isJoined = group.isMember,
                    isOwner = group.ownerId == it.myId,
                    avatarUri = group.avatars.firstOrNull()?.uri,
                    topBarActions = createTopBarActions(
                        group.ownerId == it.myId,
                        group.isMember,
                        ChatType.GROUP
                    )
                )
            }
        }
    }
    
    private suspend fun loadUserInfo(chatId: Long) {
        viewModelScope.launch {
            userRepository.fetchById(chatId)
            if (chatId != _uiState.value.myId) {
                chatRepository.markAllAsRead(chatId)
            }
        }
        
        if (chatId == _uiState.value.myId) {
            userRepository.getMe().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        chatName = UiText.StringResource(R.string.saved_messages),
                        subTitle = UiText.DynamicString(""),
                        avatarUri = user.avatars.firstOrNull()?.uri,
                        topBarActions = createTopBarActions(
                            true,
                            true,
                            ChatType.PRIVATE,
                            isMe = true
                        )
                    )
                }
            }
        } else {
            combine(
                userRepository.getById(chatId),
                onlineUsersTracker.onlineUsers
            ) { user, onlineUsers ->
                user to onlineUsers.contains(user.id)
            }.collectLatest { (user, isOnline) ->
                val subTitle = LastSeenHelper.getSubtitle(context, isOnline, user.lastSeen)
                _uiState.update {
                    it.copy(
                        chatName = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                        subTitle = subTitle,
                        avatarUri = user.avatars.firstOrNull()?.uri,
                        topBarActions = createTopBarActions(false, true, ChatType.PRIVATE),
                        isBlocked = user.isBlocked,
                        isBlockedByThem = user.isBlockedByThem
                    )
                }
            }
        }
    }
    
    private fun createTopBarActions(
        isOwner: Boolean,
        isJoined: Boolean,
        type: ChatType,
        isMe: Boolean = false
    ): List<TopBarAction> {
        val actions = mutableListOf<DropdownMenuAction>()
        if (isOwner || isMe) {
            actions.add(
                DropdownMenuAction(
                    Icons.Outlined.CleaningServices,
                    UiText.StringResource(R.string.clear_history),
                    ::showClearHistoryDialog
                )
            )
        }
        
        if (type == ChatType.PRIVATE) {
            actions.add(
                DropdownMenuAction(
                    Icons.Rounded.DeleteOutline,
                    UiText.StringResource(R.string.delete_chat),
                    ::showDeleteChatDialog,
                    isDestructive = true
                )
            )
        } else if (isJoined && !isOwner) {
            val leaveRes =
                if (type == ChatType.CHANNEL) R.string.leave_channel else R.string.leave_group
            actions.add(
                DropdownMenuAction(
                    Icons.AutoMirrored.Rounded.Logout,
                    UiText.StringResource(leaveRes),
                    ::showLeaveDialog
                )
            )
        }
        
        return if (actions.isNotEmpty()) {
            listOf(TopBarAction(icon = Icons.Rounded.MoreVert, dropdownActions = actions))
        } else emptyList()
    }
    
    private fun observeMessages() {
        _uiState.update { it.copy(isLoading = true) }
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
            chatRepository.getMessages(_uiState.value.chatId, limit = 50, offset = 0)
                .onSuccess { freshMessages ->
                    if (freshMessages.size < 50) _uiState.update { it.copy(hasMoreMessages = false) }
                }
        }
    }
    
    private fun observeRealtimeEvents() {
        viewModelScope.launch {
            realtimeEventSyncService.groupReadEvents.collect { payload ->
                if (payload.chatId == _uiState.value.chatId && payload.userId != _uiState.value.myId) {
                    val readerInfo =
                        MessageReadInfo(
                            userId = payload.userId,
                            firstName = "",
                            lastName = null,
                            readAt = payload.time
                        )
                    val current = _uiState.value.groupReadInfo
                    val existing = current[payload.messageId].orEmpty()
                    if (existing.none { it.userId == payload.userId }) {
                        _uiState.update { it.copy(groupReadInfo = current + (payload.messageId to (existing + readerInfo))) }
                    }
                    loadUserName(payload.userId)
                }
            }
        }
        
        viewModelScope.launch {
            realtimeEventSyncService.chatRemovedEvents.collect { removedChatId ->
                if (removedChatId == _uiState.value.chatId) _uiEffect.emit(ChatUiEffect.NavigateToMain)
            }
        }
    }
    
    private fun updateChatItems(messages: List<Message>) {
        val mapper = ChatItemMapper(
            context = context,
            myId = _uiState.value.myId,
            chatId = _uiState.value.chatId,
            isOwner = _uiState.value.isOwner,
            userNamesCache = _uiState.value.userNamesCache,
            groupReadInfo = _uiState.value.groupReadInfo,
            onCopyText = ::copyToClipboard,
            onEditMessage = ::startEditing,
            onDeleteMessage = {
                showDeleteMessageDialog()
                selectMessage(it)
            },
            onRetrySendMessage = ::retrySendMessage,
            onCancelSendMessage = ::cancelSendMessage,
            onLoadUserName = ::loadUserName
        )
        
        val chatItems = mapper.map(messages)
        val newMediaItems = messages.flatMap { it.attachments }
            .filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO || it.type == AttachmentType.GIF }
        
        _uiState.update { it.copy(chatItems = chatItems, mediaItems = newMediaItems) }
        
        // Auto-download logic
        if (autoDownloadMedia) {
            messages.forEach { msg ->
                msg.attachments.forEach { attachment ->
                    if (attachment.status == DownloadStatus.IDLE || attachment.status == DownloadStatus.UPLOADED) {
                        val shouldDownload = when (attachment.type) {
                            AttachmentType.VOICE -> true
                            AttachmentType.IMAGE, AttachmentType.GIF -> autoDownloadPhotos
                            AttachmentType.VIDEO -> autoDownloadVideos && attachment.size <= 10 * 1024 * 1024
                            AttachmentType.FILE -> autoDownloadFiles && attachment.size <= 10 * 1024 * 1024
                        }
                        if (shouldDownload) onFileAction(msg, attachment, FileAction.DOWNLOAD)
                    }
                }
            }
        }
    }
    
    fun loadMoreMessages() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreMessages || state.isLoading) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val offset = limitFlow.value
            chatRepository.getMessages(state.chatId, limit = 50, offset = offset)
                .onSuccess { moreMessages ->
                    if (moreMessages.isEmpty()) {
                        _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = false) }
                    } else {
                        if (moreMessages.size < 50) _uiState.update { it.copy(hasMoreMessages = false) }
                        limitFlow.value += moreMessages.size
                    }
                    _uiState.update { it.copy(isLoadingMore = false) }
                }.onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }
    // endregion
    
    // region Message Actions
    fun changeText(newText: String) {
        _uiState.update { it.copy(messageText = newText) }
    }
    
    fun onSendMessageClicked() {
        val editingId = _uiState.value.editingMessageId
        if (editingId != null) {
            viewModelScope.launch { handleEditMessage(editingId) }
            return
        }
        
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return
        
        changeText("")
        onSendMessageInternal(text)
    }
    
    private fun onSendMessageInternal(text: String) {
        val tempId = -System.currentTimeMillis()
        val job = viewModelScope.launch {
            try {
                sendMessageUseCase(_uiState.value.chatId, text, tempId)?.let {
                    _uiEffect.emit(ChatUiEffect.ScrollToBottom(_uiState.value.chatItems.lastIndex))
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
            } finally {
                sendingJobs.remove(tempId)
            }
        }
        sendingJobs[tempId] = job
    }
    
    private suspend fun handleEditMessage(editingId: Long) {
        val newText = _uiState.value.messageText.trim()
        val originalText = _uiState.value.editingOriginalText
        if (newText.isEmpty() || newText == originalText) {
            cancelEditing()
            return
        }
        cancelEditing()
        chatRepository.editMessage(_uiState.value.chatId, editingId, newText)
            .onSuccess { editedMessage ->
                chatRepository.updateLocalMessage(
                    editingId,
                    editedMessage.text,
                    editedMessage.editedAt
                )
            }
    }
    
    fun retrySendMessage(message: Message) {
        viewModelScope.launch {
            if (message.attachments.isNotEmpty()) {
                // For files, we might need a more complex retry logic depending on where it failed.
                // For now, let's just re-trigger the file sending logic.
                // We'd need the original URIs which we don't have here.
                // This is a known limitation.
            } else {
                message.text?.let { text ->
                    chatRepository.deleteLocalMessage(message.id)
                    onSendMessageInternal(text)
                }
            }
        }
    }
    
    fun cancelSendMessage(message: Message) {
        sendingJobs[message.id]?.cancel()
        sendingJobs.remove(message.id)
        viewModelScope.launch {
            chatRepository.deleteLocalMessage(message.id)
        }
    }
    
    fun startEditing(message: Message) {
        val now = System.currentTimeMillis()
        if (now - message.sendTime > 24 * 60 * 60 * 1000L) {
            viewModelScope.launch {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.edit_time_limit_error)))
            }
            return
        }
        if (message.text.isNullOrBlank()) return
        _uiState.update {
            it.copy(
                editingMessageId = message.id,
                editingOriginalText = message.text,
                messageText = message.text
            )
        }
    }
    
    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingMessageId = null,
                editingOriginalText = null,
                messageText = ""
            )
        }
    }
    
    fun markAsReadMessage(message: Message) {
        if (message.senderId == _uiState.value.myId || message.isRead) return
        viewModelScope.launch {
            if (chatRepository.makeAsRead(_uiState.value.chatId, message.id)) {
                chatRepository.markMessageAsRead(_uiState.value.chatId, message.id)
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
    // endregion
    
    // region Join / Leave / Delete
    fun onJoinClicked() {
        viewModelScope.launch {
            val chatId = _uiState.value.chatId
            when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> joinChannelUseCase(chatId).onSuccess {
                    _uiState.update {
                        it.copy(
                            isJoined = true
                        )
                    }
                }
                
                ChatType.GROUP -> joinGroupUseCase(chatId).onSuccess {
                    _uiState.update {
                        it.copy(
                            isJoined = true
                        )
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
    
    fun onDeleteChatConfirmed() {
        viewModelScope.launch {
            if (chatRepository.deleteChat(
                    _uiState.value.chatId,
                    _uiState.value.deleteForRecipient
                )
            ) {
                hideDeleteChatDialog()
                _uiEffect.emit(ChatUiEffect.NavigateToMain)
            }
        }
    }
    
    fun onDeleteMessagesConfirmed() {
        viewModelScope.launch {
            if (chatRepository.deleteChatMessages(
                    _uiState.value.chatId,
                    _uiState.value.deleteForRecipient
                )
            ) {
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
    // endregion
    
    // region Dialog Management
    fun showDeleteChatDialog() =
        _uiState.update { it.copy(showDeleteChatDialog = true, deleteForRecipient = false) }
    
    fun hideDeleteChatDialog() =
        _uiState.update { it.copy(showDeleteChatDialog = false, deleteForRecipient = false) }
    
    fun showClearHistoryDialog() =
        _uiState.update { it.copy(showClearHistoryDialog = true, deleteForRecipient = false) }
    
    fun hideClearHistoryDialog() =
        _uiState.update { it.copy(showClearHistoryDialog = false, deleteForRecipient = false) }
    
    fun showDeleteMessageDialog() =
        _uiState.update { it.copy(showDeleteMessageDialog = true, deleteForRecipient = false) }
    
    fun hideDeleteMessageDialog() =
        _uiState.update { it.copy(showDeleteMessageDialog = false, deleteForRecipient = false) }
    
    fun showLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = true) }
    fun hideLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = false) }
    fun setDeleteForRecipient(delete: Boolean) =
        _uiState.update { it.copy(deleteForRecipient = delete) }
    // endregion
    
    // region File & Media Actions
    fun onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
        when (action) {
            FileAction.DOWNLOAD -> downloadFile(message, file)
            FileAction.PAUSE -> viewModelScope.launch { downloaderManager.pause(file.fileId) }
            FileAction.RESUME -> viewModelScope.launch { downloaderManager.resume(file.fileId) }
            FileAction.CANCEL -> viewModelScope.launch { downloaderManager.cancel(file.fileId) }
            FileAction.OPEN -> handleOpenFile(file)
            FileAction.PLAY -> handlePlayVoice(file)
        }
    }
    
    private fun downloadFile(message: Message, file: MessageAttachment) {
        viewModelScope.launch {
            chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
                .onSuccess { url -> downloaderManager.download(url, file.name, file.fileId) }
        }
    }
    
    private fun handleOpenFile(file: MessageAttachment) {
        if (file.type == AttachmentType.IMAGE || file.type == AttachmentType.VIDEO || file.type == AttachmentType.GIF) {
            val index = _uiState.value.mediaItems.indexOfFirst { it.fileId == file.fileId }
            _uiState.update {
                it.copy(
                    showFullScreenViewer = true,
                    initialMediaIndex = index.coerceAtLeast(0)
                )
            }
        } else {
            viewModelScope.launch { fileHandler.openFile(file.localUri.toString()) }
        }
    }
    
    private fun handlePlayVoice(file: MessageAttachment) {
        if (file.type != AttachmentType.VOICE || file.localUri == null) return
        if (_uiState.value.currentPlayingVoiceFileId == file.fileId) {
            voicePlayerManager.togglePlayPause()
        } else {
            val startPos = pendingVoiceStartPositions.remove(file.fileId) ?: 0
            playVoice(file.fileId, startPos)
        }
    }
    
    private fun playVoice(fileId: String, startPositionMs: Int = 0) {
        val queue = buildVoiceQueue(_uiState.value.chatItems, _uiState.value.chatName)
        if (queue.none { it.fileId == fileId }) return
        voicePlayerManager.play(queue, fileId, startPositionMs)
    }
    
    private fun buildVoiceQueue(items: List<ChatItem>, chatName: UiText): List<VoiceQueueItem> {
        val title = chatName.asString(context).ifBlank { context.getString(R.string.voice_message) }
        return items.filterIsInstance<ChatItem.MessageItem>()
            .flatMap { it.message.attachments }
            .filter { it.type == AttachmentType.VOICE }
            .map { attachment ->
                val isReady =
                    attachment.localUri != null && (attachment.status == DownloadStatus.COMPLETED || attachment.status == DownloadStatus.UPLOADED)
                VoiceQueueItem(
                    uri = if (isReady) attachment.localUri else null,
                    fileId = attachment.fileId,
                    title = title,
                    subtitle = context.getString(R.string.voice_message),
                    artworkUri = _uiState.value.avatarUri
                )
            }
    }
    
    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) {
        if (_uiState.value.currentPlayingVoiceFileId != file.fileId) {
            pendingVoiceStartPositions[file.fileId] = positionMs
        } else {
            voicePlayerManager.seekTo(positionMs)
        }
    }
    
    fun sendFiles(uris: List<Uri>) {
        val tempId = -System.currentTimeMillis()
        val job = viewModelScope.launch {
            try {
                sendMessageWithFilesUseCase(_uiState.value.chatId, uris, null, tempId)
            } finally {
                sendingJobs.remove(tempId)
            }
        }
        sendingJobs[tempId] = job
    }
    
    fun cancelUpload(tempMessageId: Long) {
        sendingJobs[tempMessageId]?.cancel()
        sendingJobs.remove(tempMessageId)
        viewModelScope.launch {
            val tempMessage = _uiState.value.chatItems.filterIsInstance<ChatItem.MessageItem>()
                .find { it.message.id == tempMessageId }?.message
            tempMessage?.attachments?.forEach { uploadManager.cancel(it.fileId) }
            chatRepository.deleteLocalMessage(tempMessageId)
        }
    }
    
    fun saveToGallery(uri: Uri) {
        viewModelScope.launch {
            if (fileHandler.saveToGallery(uri.path ?: uri.toString())) {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.successfully_saved_to_gallery)))
            } else {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_to_gallery)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun saveAttachmentsToDownloads(message: Message) {
        viewModelScope.launch {
            val downloaded =
                message.attachments.filter { it.localUri != null && (it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.UPLOADED) }
            if (downloaded.isEmpty()) return@launch
            
            var successCount = 0
            downloaded.forEach {
                if (fileHandler.saveToDownloads(
                        it.localUri?.path ?: it.localUri.toString(),
                        it.name
                    )
                ) successCount++
            }
            
            val res =
                if (successCount > 0) R.string.saved_to_downloads else R.string.failed_to_save_to_downloads
            _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(res)))
            if (successCount == 0) vibrationManager.vibrate(VibrationPattern.Error)
        }
    }
    // endregion
    
    // region Utilities
    fun copyToClipboard(text: String?) = text?.let { clipboardService.copy(it) }
    fun vibrate() = vibrationManager.vibrate(VibrationPattern.Error)
    fun selectMessage(message: Message) =
        _uiState.update { it.copy(selectedMessages = it.selectedMessages + message) }
    
    fun clearMediaUrl() = _uiState.update { it.copy(showFullScreenViewer = false) }
    fun setVideoLooping(isLooping: Boolean) =
        viewModelScope.launch { dataStoreManager.saveVideoLooping(isLooping) }
    
    fun setVideoPlaybackSpeed(speed: Float) =
        viewModelScope.launch { dataStoreManager.saveVideoPlaybackSpeed(speed) }
    
    fun dismissBannedDialog() = _uiState.update { it.copy(showBannedDialog = false) }
    fun onMicrophonePermissionDenied() =
        _uiState.update { it.copy(showMicrophonePermissionSheet = true) }
    
    fun dismissMicrophonePermissionSheet() =
        _uiState.update { it.copy(showMicrophonePermissionSheet = false) }
    
    fun dismissInviteBottomSheet() = _uiState.update {
        it.copy(
            showInviteBottomSheet = false,
            inviteLinkInfo = null,
            inviteLinkCode = null,
            isProcessingInvite = false
        )
    }
    // endregion
    
    // region Invite Links
    fun onLinkClicked(url: String) {
        val match = RegexPatterns.INVITE_LINK.find(url)
        if (match == null) {
            val normalized = if (url.startsWith("http")) url else "https://$url"
            viewModelScope.launch { _uiEffect.emit(ChatUiEffect.OpenUrl(normalized)) }
            return
        }
        
        val code = match.groupValues[2]
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            inviteLinkRepository.getInviteLinkInfo(code).onSuccess { linkInfo ->
                _uiState.update { it.copy(isProcessingInvite = false) }
                when {
                    _uiState.value.chatId == linkInfo.chatId -> {
                        _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.you_are_already_in_this_chat)))
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                    
                    linkInfo.isJoined != null -> _uiEffect.emit(ChatUiEffect.NavigateToChat(linkInfo.chatId))
                    linkInfo.isBanned != null -> {
                        _uiState.update { s -> s.copy(showBannedDialog = true) }
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                    
                    else -> _uiState.update { s ->
                        s.copy(
                            inviteLinkInfo = linkInfo,
                            inviteLinkCode = code,
                            showInviteBottomSheet = true
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
            searchRepository.resolveUsername(username.removePrefix("@")).onSuccess { result ->
                if (result == null) {
                    _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.chat_not_found)))
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else if (result.isBanned) {
                    _uiState.update { it.copy(showBannedDialog = true) }
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else {
                    _uiEffect.emit(ChatUiEffect.NavigateToChat(result.chatId))
                }
            }
        }
    }
    
    fun onSubscribeViaInviteLink() {
        val info = _uiState.value.inviteLinkInfo ?: return
        val code = _uiState.value.inviteLinkCode ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            joinViaInviteLinkUseCase(code, info.chatId).onSuccess {
                dismissInviteBottomSheet()
                _uiEffect.emit(ChatUiEffect.NavigateToChat(info.chatId))
            }.onFailure {
                _uiState.update { it.copy(isProcessingInvite = false) }
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_join)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    // endregion
    
    // region Voice Recording
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
                    val amplitude =
                        (audioRecorderManager.getMaxAmplitude() / 32767f).coerceIn(0f, 1f)
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
            viewModelScope.launch { _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.DynamicString("Не удалось начать запись аудио"))) }
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
        if (file != null) sendFiles(listOf(Uri.fromFile(file)))
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
    // endregion
    
    fun showBlockDialog() {
        _uiState.update { it.copy(showBlockDialog = true) }
    }
    
    fun dismissBlockDialog() {
        _uiState.update { it.copy(showBlockDialog = false) }
    }
    
    fun unblockUser() {
        viewModelScope.launch {
            userRepository.unblockUser(_uiState.value.chatId).onSuccess {
                dismissBlockDialog()
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.user_unblocked)))
            }.onFailure {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
                dismissBlockDialog()
            }
        }
    }
}
