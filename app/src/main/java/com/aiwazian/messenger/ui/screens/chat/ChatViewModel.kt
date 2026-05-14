/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

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
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.usecase.JoinChannelUseCase
import com.aiwazian.messenger.usecase.JoinGroupUseCase
import com.aiwazian.messenger.usecase.LeaveChatUseCase
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val searchRepository: SearchRepository,
    private val clipboardService: ClipboardService,
    private val webSocketClient: WebSocketClient,
    private val downloaderManager: DownloaderManager,
    private val vibrationManager: VibrationManager,
    private val fileHandler: FileHandler,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val joinGroupUseCase: JoinGroupUseCase,
    private val leaveChatUseCase: LeaveChatUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChatUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private var isFirstLoadDone = false
    private val limitFlow = MutableStateFlow(30)
    
    private var isInit = false
    
    fun init(chatId: Long, chatName: String? = null) {
        if (isInit) return
        isInit = true
        
        _uiState.update {
            it.copy(chatId = chatId, chatName = UiText.DynamicString(chatName.orEmpty()))
        }
        isFirstLoadDone = false
        limitFlow.value = 50
        
        webSocketClient.emitEvent("chat_open", mapOf("chatId" to chatId.toString()))
        
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
                                                    R.string.clear_history,
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
                                                    R.string.leave_channel,
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
                                                        R.string.clear_history,
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
                                                        R.string.leave_group,
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
                                    chatName = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = UiText.DynamicString("в сети недавно"),
                                    avatarUri = user.avatars.firstOrNull()?.uri,
                                    topBarActions = listOf(
                                        TopBarAction(
                                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                                DropdownMenuAction(
                                                    Icons.Outlined.CleaningServices,
                                                    R.string.clear_history,
                                                    ::showClearHistoryDialog
                                                ),
                                                DropdownMenuAction(
                                                    Icons.Rounded.DeleteOutline,
                                                    R.string.delete_chat,
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
                        userRepository.getById(_uiState.value.chatId).collectLatest { user ->
                            _uiState.update {
                                it.copy(
                                    chatName = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = UiText.DynamicString("в сети недавно"),
                                    avatarUri = user.avatars.firstOrNull()?.uri,
                                    topBarActions = listOf(
                                        TopBarAction(
                                            icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                                                DropdownMenuAction(
                                                    Icons.Outlined.CleaningServices,
                                                    R.string.clear_history,
                                                    ::showClearHistoryDialog
                                                ),
                                                DropdownMenuAction(
                                                    Icons.Rounded.DeleteOutline,
                                                    R.string.delete_chat,
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
                            _uiEffect.emit(ChatUiEffect.ScrollToBottom(_uiState.value.chatItems.lastIndex))
                        } else if (messages.isEmpty() && !isFirstLoadDone) {
                            isFirstLoadDone = true
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
                Log.e("ChatVM", "Error fetching fresh messages", it)
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
                Log.e("ChatVM", "Error loading more messages", it)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
    
    private fun updateChatItems(messages: List<Message>) {
        val myId = _uiState.value.myId
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
            val chatType = ChatType.fromId(_uiState.value.chatId)
            
            val actions = mutableListOf<DropdownMenuAction>()
            if (!message.text.isNullOrBlank()) {
                actions.add(
                    DropdownMenuAction(
                        Icons.Rounded.ContentCopy,
                        R.string.copy,
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
                        Icons.Rounded.DeleteOutline, R.string.delete, onClick = {
                            showDeleteMessageDialog()
                            selectMessage(message)
                        }, isDestructive = true
                    )
                )
            }
            
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
    
    fun onBackClicked() = viewModelScope.launch {
        _uiEffect.emit(ChatUiEffect.NavigateBack)
    }
    
    fun cancelUpload(tempMessageId: Long) {
        viewModelScope.launch {
            val tempMessage = getRawMessages().find { it.id == tempMessageId }
            tempMessage?.attachments?.forEach { attachment ->
                downloaderManager.cancel(attachment.fileId)
            }
            
            val updatedMessages = getRawMessages().filter { it.id != tempMessageId }
            updateChatItems(updatedMessages)
        }
    }
    
    fun onFileAction(
        message: Message, file: MessageAttachment, action: FileAction
    ) {
        when (action) {
            FileAction.DOWNLOAD -> {
                viewModelScope.launch {
                    chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
                        ?.let { url ->
                            downloaderManager.download(
                                url = url,
                                fileName = file.name,
                                fileId = file.fileId
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
        
        val code = match.groupValues[1]
        
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
    
    fun clearMediaUrl() {
        _uiState.update { it.copy(currentMediaUrl = null) }
    }
}
