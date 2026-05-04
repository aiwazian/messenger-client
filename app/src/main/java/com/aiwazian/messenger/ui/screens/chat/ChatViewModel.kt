/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DownloadItem
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.profile.Profile
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val clipboardService: ClipboardService,
    private val webSocketClient: WebSocketClient,
    private val downloaderManager: DownloaderManager,
    private val vibrationManager: VibrationManager,
    private val fileHandler: FileHandler,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChatUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val _selectedMessageId = MutableStateFlow<Long?>(null)
    
    private var isFirstLoadDone = false
    private val limitFlow = MutableStateFlow(50)
    
    private val uploadJobs = mutableMapOf<Long, Job>()
    private val autoResumedFileIds = mutableSetOf<String>()
    
    fun init(chatId: Long, chatName: String? = null) {
        _uiState.update {
            it.copy(chatId = chatId, chatName = UiText.DynamicString(chatName.orEmpty()))
        }
        isFirstLoadDone = false
        limitFlow.value = 50
        autoResumedFileIds.clear()
        
        setupUserObserver()
        loadChatData()
    }
    
    private fun setupUserObserver() {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
                updateUiContent()
            }
        }
        viewModelScope.launch {
            downloaderManager.downloads.collect { downloads ->
                updateDownloadsInUi(downloads)
            }
        }
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state == ConnectionState.CONNECTED) }
            }
        }
    }
    
    private fun updateDownloadsInUi(downloads: List<DownloadItem>) {
        _uiState.value.chatItems.forEach { item ->
            if (item is ChatItem.MessageItem) {
                item.message.attachments.forEach { file ->
                    downloads.findLast { it.fileId == file.fileId }?.let { download ->
                        viewModelScope.launch {
                            chatRepository.updateFileStatus(
                                file.fileId,
                                download.status,
                                download.localUri
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun getRawMessages(): List<Message> {
        return _uiState.value.chatItems.filterIsInstance<ChatItem.MessageItem>().map { it.message }
    }
    
    private fun loadChatData() {
        _uiState.update { it.copy(isLoading = true, profile = null) }
        
        when (ChatType.fromId(_uiState.value.chatId)) {
            ChatType.CHANNEL -> {
                viewModelScope.launch {
                    channelRepository.getById(_uiState.value.chatId).collectLatest { channel ->
                        val profile = Profile.Channel(
                            id = channel.id,
                            ownerId = channel.ownerId,
                            name = channel.name,
                            bio = channel.bio,
                            subscribers = channel.subscribers,
                            removedUser = channel.removedUser,
                            channelType = channel.channelType,
                            username = channel.username,
                            isSubscribed = channel.isSubscribed
                        )
                        _uiState.update {
                            it.copy(
                                profile = profile, isJoined = channel.isSubscribed
                            )
                        }
                        updateUiContent()
                    }
                }
            }
            
            ChatType.GROUP -> {
                viewModelScope.launch {
                    groupRepository.getById(_uiState.value.chatId).collectLatest { group ->
                        group.let {
                            val profile = Profile.Group(
                                id = group.id,
                                ownerId = group.ownerId,
                                name = group.name,
                                bio = group.bio,
                                members = group.members
                            )
                            _uiState.update { it.copy(profile = profile) }
                            updateUiContent()
                        }
                    }
                }
            }
            
            ChatType.PRIVATE -> {
                viewModelScope.launch {
                    if (_uiState.value.chatId == _uiState.value.currentUserId) {
                        userRepository.getMe().collectLatest { user ->
                            val profile = Profile.User(
                                id = user.id,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth,
                                lastSeen = user.lastSeen
                            )
                            _uiState.update { it.copy(profile = profile) }
                            updateUiContent()
                        }
                    } else {
                        userRepository.getById(_uiState.value.chatId).collectLatest { user ->
                            val profile = Profile.User(
                                id = user.id,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth,
                                lastSeen = user.lastSeen
                            )
                            _uiState.update { it.copy(profile = profile) }
                            updateUiContent()
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
                            _uiEffect.emit(ChatUiEffect.ScrollToBottom(_uiState.value.chatItems.lastIndex))
                        } else if (messages.isEmpty() && !isFirstLoadDone) {
                            isFirstLoadDone = true
                        }
                    }
            }
        }
        
        viewModelScope.launch {
            try {
                val freshMessages = chatRepository.getMessages(
                    chatId = _uiState.value.chatId, limit = 50, offset = 0
                )
                if (freshMessages.size < 50) {
                    _uiState.update { it.copy(hasMoreMessages = false) }
                }
            } catch (e: Exception) {
                Log.e(
                    "ChatVM", "Error fetching fresh messages", e
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
            updateUiContent()
        }
    }
    
    fun loadMoreMessages() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreMessages || state.isLoading) return
        
        _uiState.update { it.copy(isLoadingMore = true) }
        
        viewModelScope.launch {
            try {
                val offset = limitFlow.value
                val moreMessages = chatRepository.getMessages(
                    _uiState.value.chatId, limit = 50, offset = offset
                )
                
                if (moreMessages.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false, hasMoreMessages = false
                        )
                    }
                } else {
                    if (moreMessages.size < 50) {
                        _uiState.update { it.copy(hasMoreMessages = false) }
                    }
                    limitFlow.value += moreMessages.size
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Error loading more messages", e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
    
    private fun updateUiContent() {
        val state = _uiState.value
        val profile = state.profile
        val myId = state.currentUserId
        var chatName: UiText
        var subTitle: UiText
        var actions = listOf<TopBarAction>()
        
        when (profile) {
            is Profile.User -> {
                chatName = if (profile.id == myId) {
                    UiText.StringResource(R.string.saved_messages)
                } else {
                    UiText.DynamicString("${profile.firstName} ${profile.lastName.orEmpty()}".trim())
                }
                
                subTitle = if (profile.id != myId) {
                    if (profile.lastSeen != null) {
                        val isOnline = abs(System.currentTimeMillis() - profile.lastSeen) <= 10_000
                        if (isOnline) {
                            UiText.DynamicString("в сети")
                        } else {
                            UiText.DynamicString(
                                "был(а) в " + profile.lastSeen.toInstance().toPrettyTime()
                            )
                        }
                    } else UiText.DynamicString("")
                } else UiText.DynamicString("")
                
                
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                            DropdownMenuAction(
                                Icons.Rounded.DeleteOutline,
                                R.string.clear_history,
                                ::showClearHistoryDialog
                            )
                        )
                    )
                )
            }
            
            is Profile.Channel -> {
                chatName = UiText.DynamicString(profile.name)
                
                subTitle = UiText.PluralResource(
                    R.plurals.subscribers_count,
                    profile.subscribers,
                    profile.subscribers
                )
                
                actions = if (profile.ownerId == myId) {
                    listOf(
                        TopBarAction(
                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                DropdownMenuAction(
                                    Icons.Rounded.DeleteOutline,
                                    R.string.clear_history,
                                    ::showClearHistoryDialog
                                )
                            )
                        )
                    )
                } else {
                    emptyList()
                }
            }
            
            is Profile.Group -> {
                chatName = UiText.DynamicString(profile.name)
                
                subTitle = UiText.PluralResource(
                    R.plurals.members_count,
                    profile.members,
                    profile.members
                )
                
                if (profile.ownerId == myId) {
                    actions = listOf(
                        TopBarAction(
                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                DropdownMenuAction(
                                    Icons.Rounded.DeleteOutline,
                                    R.string.clear_history,
                                    ::showClearHistoryDialog
                                )
                            )
                        )
                    )
                } else {
                    actions = listOf(
                        TopBarAction(
                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                DropdownMenuAction(
                                    Icons.AutoMirrored.Rounded.Logout,
                                    R.string.leave_group,
                                    ::showLeaveDialog
                                )
                            )
                        )
                    )
                }
            }
            
            else -> {
                chatName = state.chatName
                subTitle = UiText.DynamicString("")
            }
        }
        
        _uiState.update {
            it.copy(
                chatName = chatName,
                subTitle = subTitle,
                topBarActions = actions,
            )
        }
    }
    
    private fun updateChatItems(messages: List<Message>) {
        val myId = _uiState.value.currentUserId
        val chatItems = mutableListOf<ChatItem>()
        var lastDate: java.time.LocalDate? = null
        var lastSenderId: Long? = null
        
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
            
            val actions = mutableListOf<DropdownMenuAction>()
            if (!message.text.isNullOrBlank()) {
                actions.add(
                    DropdownMenuAction(
                        Icons.Rounded.ContentCopy,
                        R.string.copy,
                        onClick = { copyToClipboard(message.text) })
                )
            }
            actions.add(
                DropdownMenuAction(
                    Icons.Rounded.DeleteOutline, R.string.delete, onClick = {
                        showDeleteMessageDialog(message.id)
                        selectMessage(message)
                    }, isDestructive = true
                )
            )
            
            chatItems.add(
                ChatItem.MessageItem(
                    message = message,
                    time = message.sendTime.toInstance().toPrettyTime(),
                    isMine = isMine,
                    isRead = if (isMine) message.isRead else null,
                    senderName = if (!isMine && ChatType.fromId(message.chatId) == ChatType.GROUP) {
                        _uiState.value.userNamesCache[message.senderId].also {
                            if (it == null) loadUserName(message.senderId)
                        }
                    } else null,
                    isFirstInGroup = isFirstInGroup,
                    isSingleEmoji = isSingleEmoji,
                    dropdownActions = actions))
            
            lastSenderId = message.senderId
        }
        _uiState.update { it.copy(chatItems = chatItems) }
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
                Log.e("ChatVM", "Error sending message", e)
            }
        }
    }
    
    fun onJoinClicked() {
        viewModelScope.launch {
            channelRepository.join(_uiState.value.chatId).onSuccess {
                _uiState.update { it.copy(isJoined = true) }
            }
        }
    }
    
    fun onLeaveClicked() {
        viewModelScope.launch {
            val chatId = _uiState.value.chatId
            val success = when (_uiState.value.profile) {
                is Profile.Channel -> channelRepository.leave(chatId).isSuccess
                is Profile.Group -> groupRepository.leave(chatId).isSuccess
                else -> false
            }
            if (success) {
                hideLeaveDialog()
                _uiEffect.emit(ChatUiEffect.NavigateToMain)
            }
        }
    }
    
    fun showDeleteChatDialog() = _uiState.update { it.copy(showDeleteChatDialog = true) }
    
    fun hideDeleteChatDialog() = _uiState.update { it.copy(showDeleteChatDialog = false) }
    
    fun showClearHistoryDialog() = _uiState.update { it.copy(showClearHistoryDialog = true) }
    
    fun hideClearHistoryDialog() = _uiState.update { it.copy(showClearHistoryDialog = false) }
    
    fun showDeleteMessageDialog(messageId: Long) {
        _selectedMessageId.value = messageId
        _uiState.update { it.copy(showDeleteMessageDialog = true) }
    }
    
    fun hideDeleteMessageDialog() {
        _selectedMessageId.value = null
        _uiState.update { it.copy(showDeleteMessageDialog = false) }
    }
    
    fun showLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = true) }
    
    fun hideLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = false) }
    
    fun vibrate() {
        vibrationManager.vibrate(VibrationPattern.Error)
    }
    
    fun onDeleteChatConfirmed() {
        viewModelScope.launch {
            if (chatRepository.deleteChat(_uiState.value.chatId)) {
                hideDeleteChatDialog()
                _uiEffect.emit(ChatUiEffect.NavigateToMain)
            }
        }
    }
    
    fun onDeleteMessagesConfirmed() {
        viewModelScope.launch {
            if (chatRepository.deleteChatMessages(_uiState.value.chatId)) {
                hideClearHistoryDialog()
            }
        }
    }
    
    fun onDeleteMessageConfirmed() {
        viewModelScope.launch {
            _uiState.value.selectedMessages.forEach { message ->
                chatRepository.deleteMessage(_uiState.value.chatId, message.id)
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
    
    fun onBackClicked() = viewModelScope.launch {
        _uiEffect.emit(ChatUiEffect.NavigateBack)
    }
    
    fun cancelUpload(tempMessageId: Long) {
        uploadJobs[tempMessageId]?.cancel()
        uploadJobs.remove(tempMessageId)
        downloaderManager.cancel(tempMessageId.toInt())
        
        val updatedMessages = getRawMessages().filter { it.id != tempMessageId }
        updateChatItems(updatedMessages)
    }
    
    fun onFileAction(
        message: Message, file: MessageAttachment, action: FileAction
    ) {
        when (action) {
            FileAction.DOWNLOAD -> {
                viewModelScope.launch {
                    try {
                        chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
                            ?.let { url ->
                                downloaderManager.download(
                                    url = url,
                                    fileName = file.name,
                                    fileId = file.fileId
                                )
                            }
                    } catch (e: Exception) {
                        Log.e("ChatVM", "Error getting download URL", e)
                    }
                }
            }
            
            FileAction.PAUSE -> {
                downloaderManager.downloads.value.find { it.fileId == file.fileId }?.let {
                    downloaderManager.pause(it.id)
                }
            }
            
            FileAction.RESUME -> {
                downloaderManager.downloads.value.find { it.fileId == file.fileId }?.let {
                    downloaderManager.resume(it.id)
                }
            }
            
            FileAction.CANCEL -> {
                downloaderManager.downloads.value.find { it.fileId == file.fileId }?.let {
                    downloaderManager.cancel(it.id)
                }
            }
            
            FileAction.OPEN -> {
                if (file.type == AttachmentType.IMAGE) {
                    _uiState.update { it.copy(currentMediaUrl = file.localUri.toString()) }
                } else {
                    viewModelScope.launch {
                        fileHandler.openFile(path = file.localUri.toString())
                    }
                }
            }
        }
    }
    
    fun sendFiles(uris: List<Uri>) {
        viewModelScope.launch {
            sendMessageWithFilesUseCase(_uiState.value.chatId, uris, null)
        }
    }
    
    fun markAsReadMessage(message: Message) {
        if (message.senderId == _uiState.value.currentUserId || message.isRead) return
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
                Log.e("ChatVM", "Error loading user name", e)
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
        val inviteLinkRegex = Regex("(?:https?://)?aiwazian\\.ru/\\+([a-f0-9]+)")
        val match = inviteLinkRegex.find(url)
        
        if (match == null) {
            val normalizedUrl =
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            viewModelScope.launch {
                _uiEffect.emit(ChatUiEffect.OpenUrl(normalizedUrl))
            }
            return
        }
        
        val code = match.groupValues[1]
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            
            inviteLinkRepository.getInviteLinkInfo(code).onSuccess { linkInfo ->
                if (_uiState.value.chatId == linkInfo.chatId) {
                    _uiState.update { it.copy(isProcessingInvite = false) }
                    _uiEffect.emit(ChatUiEffect.ShowSnackbar("Вы уже в этом чате"))
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
                _uiEffect.emit(ChatUiEffect.ShowSnackbar("Ссылка недействительна"))
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
            
            val result = inviteLinkRepository.joinViaInviteCode(code)
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
                _uiEffect.emit(ChatUiEffect.ShowSnackbar("Ошибка при вступлении"))
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
    
    fun clearMediaUrl() {
        _uiState.update { it.copy(currentMediaUrl = null) }
    }
}
