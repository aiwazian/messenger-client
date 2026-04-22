/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
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
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.profile.Profile
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.ProgressRequestBody
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.random.Random

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
    private val okHttpClient: OkHttpClient,
    private val vibrationManager: VibrationManager,
    private val fileHandler: FileHandler,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChatUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val _selectedMessageId = MutableStateFlow<Int?>(null)
    
    private var profileCollectionJob: Job? = null
    private var messagesCollectionJob: Job? = null
    private var isFirstLoadDone = false
    private val limitFlow = MutableStateFlow(50)
    
    private val uploadJobs = mutableMapOf<Long, Job>()
    
    fun init(chatId: Long, chatName: String? = null) {
        _uiState.update { it.copy(chatId = chatId, chatName = chatName.orEmpty()) }
        isFirstLoadDone = false
        limitFlow.value = 50
        
        setupUserObserver()
        setupWebSocketListeners()
        loadChatData()
    }
    
    private fun setupUserObserver() {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
                updateUiContent()
            }
        }
    }
    
    private fun setupWebSocketListeners() {
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
        val currentItems = _uiState.value.chatItems.map { item ->
            if (item is ChatItem.MessageItem) {
                val updatedAttachments = item.message.attachments.map { file ->
                    val download =
                        downloads.findLast { it.fileId == file.id || (it.messageId == item.message.id && it.name == file.name) }
                    if (download != null) {
                        if (download.status == DownloadStatus.COMPLETED && file.status != DownloadStatus.COMPLETED) {
                            val updatedFile = file.copy(
                                status = download.status,
                                progress = 100,
                                localUri = download.localUri
                            )
                            viewModelScope.launch {
                                val messageToUpdate = item.message
                                val finalAttachments = messageToUpdate.attachments.map {
                                    if (it.id == updatedFile.id) updatedFile else it
                                }
                                chatRepository.saveMessage(messageToUpdate.copy(attachments = finalAttachments))
                            }
                            updatedFile
                        } else {
                            file.copy(
                                status = download.status,
                                progress = download.progress,
                                localUri = download.localUri ?: file.localUri
                            )
                        }
                    } else if (downloaderManager.isDownloaded(
                            file.id, file.extension
                        )
                    ) {
                        file.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 100,
                            localUri = downloaderManager.getFile(
                                file.id, file.extension
                            ).absolutePath
                        )
                    } else file
                }
                item.copy(message = item.message.copy(attachments = updatedAttachments))
            } else item
        }
        _uiState.update { it.copy(chatItems = currentItems) }
    }
    
    override fun onCleared() {
        super.onCleared()
        profileCollectionJob?.cancel()
        messagesCollectionJob?.cancel()
    }
    
    private fun getRawMessages(): List<Message> {
        return _uiState.value.chatItems.filterIsInstance<ChatItem.MessageItem>().map { it.message }
    }
    
    private fun loadChatData() {
        _uiState.update {
            it.copy(
                isLoading = true, profile = null
            )
        }
        
        val chatType = ChatType.fromId(_uiState.value.chatId)
        
        profileCollectionJob = when (chatType) {
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
                    if (_uiState.value.chatId == userRepository.getMe().first().id) {
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
                            _uiState.update {
                                it.copy(
                                    profile = profile
                                )
                            }
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
                            val lastSeenText = if (user.lastSeen != null) {
                                val isOnline =
                                    abs(System.currentTimeMillis() - user.lastSeen) <= 10_000
                                if (isOnline) {
                                    "в сети"
                                } else {
                                    "был(а) в " + user.lastSeen.toInstance().toPrettyTime()
                                }
                            } else ""
                            
                            _uiState.update {
                                it.copy(
                                    profile = profile, subTitle = lastSeenText
                                )
                            }
                            updateUiContent()
                        }
                    }
                }
            }
            
            else -> null
        }
        
        messagesCollectionJob = viewModelScope.launch {
            val userId = userRepository.getMe().first().id
            limitFlow.collectLatest { limit ->
                chatRepository.getMessagesFlow(userId, _uiState.value.chatId, limit, 0)
                    .collect { messages ->
                        updateChatItems(messages)
                        _uiState.update {
                            it.copy(
                                isLoading = false, isLoadingMore = false
                            )
                        }
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
                Log.e(
                    "ChatVM", "Error loading more messages", e
                )
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
    
    private fun updateUiContent() {
        val state = _uiState.value
        val profile = state.profile
        val chatId = state.chatId
        val myId = state.currentUserId
        
        val isSavedMessages = chatId == myId
        val chatName = when {
            isSavedMessages -> "Избранное"
            profile is Profile.User -> "${profile.firstName} ${profile.lastName.orEmpty()}".trim()
            profile is Profile.Channel -> profile.name
            profile is Profile.Group -> profile.name
            else -> state.chatName
        }
        
        var subCount: Int? = null
        var memCount: Int? = null
        var actions = listOf<TopBarAction>()
        var isOwner = false
        
        when (ChatType.fromId(chatId)) {
            ChatType.PRIVATE -> {
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
                isOwner = true
            }
            
            ChatType.CHANNEL -> {
                if (profile is Profile.Channel) {
                    subCount = profile.subscribers
                    isOwner = profile.ownerId == myId
                    
                    if (isOwner) {
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
                    } else if (profile.isSubscribed) {
                        actions = emptyList()
                    } else {
                        actions = emptyList()
                    }
                }
            }
            
            ChatType.GROUP -> {
                if (profile is Profile.Group) {
                    memCount = profile.members
                    isOwner = profile.ownerId == myId
                    
                    if (isOwner) {
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
            }
            
            else -> {}
        }
        
        _uiState.update {
            it.copy(
                chatName = chatName,
                isSavedMessages = isSavedMessages,
                subscriberCount = subCount,
                memberCount = memCount,
                topBarActions = actions,
                isOwner = isOwner
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
                        text = UiText.StringResource(resId = textResId), sendTime = message.sendTime
                    )
                )
                return@forEach
            }
            
            val isMine =
                message.senderId == myId && ChatType.fromId(_uiState.value.chatId) != ChatType.CHANNEL
            val isSingleEmoji = isSingleEmoji(message.text ?: "")
            val isFirstInGroup = message.senderId != lastSenderId
            
            val updatedAttachments = message.attachments.map { file ->
                val localFile =
                    if (file.id.isNotBlank()) downloaderManager.getFile(
                        file.id,
                        file.extension
                    ) else null
                
                if (file.status == DownloadStatus.UPLOADED) {
                    file
                } else if (file.localUri != null && File(file.localUri).exists()) {
                    file.copy(
                        status = DownloadStatus.COMPLETED, progress = 100, localUri = file.localUri
                    )
                } else if (localFile != null && localFile.isFile && localFile.length() > 0) {
                    file.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 100,
                        localUri = localFile.absolutePath
                    )
                } else {
                    val download =
                        downloaderManager.downloads.value.findLast { it.fileId == file.id }
                    if (download != null) {
                        file.copy(
                            status = download.status,
                            progress = download.progress,
                            localUri = if (download.status == DownloadStatus.COMPLETED) download.localUri else file.localUri
                        )
                    } else {
                        file
                    }
                }
            }
            val processedMessage = message.copy(attachments = updatedAttachments)
            
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
                    message = processedMessage,
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
            val success = when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> channelRepository.leave(chatId).isSuccess
                ChatType.GROUP -> groupRepository.leave(chatId).isSuccess
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
    
    fun showDeleteMessageDialog(messageId: Int) {
        _selectedMessageId.value = messageId
        _uiState.update { it.copy(showDeleteMessageDialog = true) }
    }
    
    fun hideDeleteMessageDialog() {
        _selectedMessageId.value = null
        _uiState.update { it.copy(showDeleteMessageDialog = false) }
    }
    
    fun showLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = true) }
    
    fun hideLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = false) }
    
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
    
    fun cancelUpload(tempMessageId: Int) {
        val uploadId = tempMessageId.toLong()
        uploadJobs[uploadId]?.cancel()
        uploadJobs.remove(uploadId)
        downloaderManager.cancel(uploadId.toInt())
        
        val updatedMessages = getRawMessages().filter { it.id != tempMessageId }
        updateChatItems(updatedMessages)
    }
    
    fun onFileAction(
        message: Message, file: MessageFile, action: FileAction
    ) {
        when (action) {
            FileAction.DOWNLOAD -> {
                viewModelScope.launch {
                    try {
                        chatRepository.getDownloadUrl(message.chatId, message.id, file.id)?.let {
                            downloaderManager.download(
                                url = it.downloadUrl,
                                fileName = file.name,
                                chatId = message.chatId,
                                messageId = message.id,
                                fileId = file.id,
                                size = file.size
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "ChatVM", "Error getting download URL", e
                        )
                    }
                }
            }
            
            FileAction.PAUSE -> {
                downloaderManager.downloads.value.find { it.fileId == file.id }?.let {
                    downloaderManager.pause(it.id)
                }
            }
            
            FileAction.RESUME -> {
                downloaderManager.downloads.value.find { it.fileId == file.id }?.let {
                    downloaderManager.resume(it.id)
                }
            }
            
            FileAction.CANCEL -> {
                downloaderManager.downloads.value.find { it.fileId == file.id }?.let {
                    downloaderManager.cancel(it.id)
                }
            }
            
            FileAction.OPEN -> {
                viewModelScope.launch {
                    fileHandler.openFile(
                        chatId = message.chatId,
                        messageId = message.id,
                        fileId = file.id,
                        fileName = file.name,
                        fileSize = file.size,
                        localUri = file.localUri
                    )
                }
            }
        }
    }
    
    fun uploadFiles(
        uris: List<Uri>, context: Context
    ) {
        viewModelScope.launch {
            uris.forEach { uri ->
                val fileName = getFileName(
                    context, uri
                ) ?: "file"
                val fileSize = getFileSize(
                    context, uri
                )
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                
                val tempId = Random.nextInt(1000000, Int.MAX_VALUE)
                val tempMessage = Message(
                    id = tempId,
                    text = null,
                    senderId = _uiState.value.currentUserId,
                    chatId = _uiState.value.chatId,
                    sendTime = System.currentTimeMillis(),
                    isRead = false,
                    messageType = MessageType.TEXT,
                    systemMessageEventType = null,
                    attachments = listOf(
                        MessageFile(
                            id = "temp_${tempId}",
                            name = fileName,
                            size = fileSize,
                            extension = fileName.substringAfterLast('.', ""),
                            status = DownloadStatus.UPLOADING,
                            progress = 0,
                            localUri = null
                        )
                    )
                )
                
                updateChatItems(getRawMessages() + tempMessage)
                
                downloaderManager.registerUpload(
                    tempId, fileName, fileSize
                )
                
                val initResponse = chatRepository.initFileUpload(
                    _uiState.value.chatId, FileInitRequestDto(
                        name = fileName, size = fileSize, mimeType = mimeType
                    )
                )
                
                try {
                    if (initResponse != null) {
                        performUpload(
                            uri, initResponse.signedUrl, tempId, context
                        ) {
                            viewModelScope.launch {
                                val confirmedMessage = chatRepository.confirmFileUpload(
                                    _uiState.value.chatId, FileConfirmRequestDto(
                                        fileId = initResponse.fileId, text = null
                                    )
                                )
                                if (confirmedMessage != null) {
                                    // Принудительно устанавливаем UPLOADED, чтобы не конфликтовать с COMPLETED (скачанным)
                                    val messageWithUploadedStatus = confirmedMessage.copy(
                                        attachments = confirmedMessage.attachments.map {
                                            it.copy(
                                                status = DownloadStatus.UPLOADED
                                            )
                                        }
                                    )
                                    downloaderManager.completeUpload(tempId)
                                    val updated =
                                        getRawMessages().map { if (it.id == tempId) messageWithUploadedStatus else it }
                                    updateChatItems(updated)
                                }
                            }
                        }
                    } else {
                        handleUploadError(tempId)
                    }
                } catch (_: Exception) {
                    handleUploadError(tempId)
                }
            }
        }
    }
    
    private fun handleUploadError(id: Int) {
        downloaderManager.failUpload(id)
    }
    
    private fun performUpload(
        uri: Uri, url: String, id: Int, context: Context, onSuccess: () -> Unit
    ) {
        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileSize = getFileSize(context, uri)
                val mimeType = context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                
                val requestBody = ProgressRequestBody(
                    mimeType, fileSize, { progress ->
                        downloaderManager.updateUploadProgress(
                            id, progress
                        )
                    }) {
                    context.contentResolver.openInputStream(uri)
                        ?: throw java.io.IOException("Unable to open input stream from $uri")
                }
                
                val request = Request.Builder().url(url).put(requestBody).build()
                
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    downloaderManager.completeUpload(id)
                    onSuccess()
                } else {
                    withContext(Dispatchers.Main) {
                        handleUploadError(id)
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "ChatViewModel", e.message, e
                )
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        handleUploadError(id)
                    }
                }
            } finally {
                uploadJobs.remove(id.toLong())
            }
        }
        uploadJobs[id.toLong()] = job
    }
    
    private fun getFileName(
        context: Context, uri: Uri
    ): String? {
        var name: String? = null
        context.contentResolver.query(
            uri, null, null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name =
                    cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }
    
    private fun getFileSize(
        context: Context, uri: Uri
    ): Long {
        var size: Long = 0
        context.contentResolver.query(
            uri, null, null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
            }
        }
        return size
    }
    
    fun markAsReadMessage(message: Message) {
        if (message.senderId == _uiState.value.currentUserId || message.isRead) return
        viewModelScope.launch {
            if (chatRepository.makeAsRead(
                    _uiState.value.chatId, message.id
                )
            ) {
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
                    updateChatItems(getRawMessages())
                }
            } catch (e: Exception) {
                Log.e(
                    "ChatVM", "Error loading user name", e
                )
            }
        }
    }
    
    private fun readMessage(id: Int) {
        val messages = getRawMessages().map { if (it.id == id) it.copy(isRead = true) else it }
        updateChatItems(messages)
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
                    _uiState.update { it.copy(showBannedDialog = true, isProcessingInvite = false) }
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
}
