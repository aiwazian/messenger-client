/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.MessageSearchHit
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.push.NotificationHelper
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.ReplyDraftCache
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.OutgoingSocketEvent
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.chat.paging.MessageWindowPager
import com.aiwazian.messenger.ui.screens.chat.paging.ScrollTarget
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val channelAdminsRepository: ChannelAdminsRepository,
    private val groupAdminsRepository: GroupAdminsRepository,
    private val userRepository: UserRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val searchRepository: SearchRepository,
    private val replyDraftCache: ReplyDraftCache,
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
    private var recordingTimerJob: Job? = null
    private var draftSaveJob: Job? = null

    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    private val sendingJobs = mutableMapOf<Long, Job>()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var isFirstLoadDone = false

    private var messagePager: MessageWindowPager? = null
    private var windowObserverJob: Job? = null
    private var highlightJob: Job? = null
    private var searchJob: Job? = null
    private var searchCursorId: Long? = null
    private var lastMessages: List<Message> = emptyList()

    private val returnStack = ArrayDeque<Long>()

    private var isViewportAtBottom = true

    private var unreadAnchorMessageId: Long? = null

    private var reportedReadUpToId = 0L

    private var lastKnownNewestId = 0L

    private var isInit = false
    private var autoDownloadMedia = false
    private var autoDownloadPhotos = true
    private var autoDownloadVideos = true
    private var autoDownloadFiles = true
    
    private val copyPolicy: ChatCopyPolicy
        get() = _uiState.value.copyPolicy

    init {
        loadSettings()
        observeVoicePlayer()
        observeQueueUpdates()
        setupSocketConnectionObserver()
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

        notificationHelper.clearChatNotifications(chatId)
        webSocketClient.emitEvent(
            OutgoingSocketEvent.CHAT_OPEN,
            mapOf("chatId" to chatId.toString())
        )

        loadDraft(chatId)
        loadChatInfo()
        observeRealtimeEvents()
        observeMessages()
    }

    private fun loadDraft(chatId: Long) {
        viewModelScope.launch {
            val myId = userRepository.getMe().first().id
            val draft = chatRepository.getDraftFlow(myId, chatId).firstOrNull()
            if (!draft.isNullOrBlank()) {
                _uiState.update { it.copy(messageText = draft) }
            }

            val reply = replyDraftCache.get(myId, chatId)
            if (reply != null) {
                _uiState.update { it.copy(replyToMessage = reply) }
            }
        }
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

    private fun setupSocketConnectionObserver() {
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
            val me = userRepository.getMe().first()
            val myId = me.id
            _uiState.update {
                it.copy(
                    myId = myId,
                    myName = "${me.firstName} ${me.lastName.orEmpty()}".trim()
                )
            }
            when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> loadChannelInfo(chatId, myId)
                ChatType.GROUP -> loadGroupInfo(chatId, myId)
                ChatType.PRIVATE -> loadUserInfo(chatId, myId)
                else -> {}
            }
        }
    }

    private suspend fun loadChannelInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            channelRepository.fetchById(chatId)
        }
        loadMyChannelPermissions(chatId)
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
                    isOwner = channel.ownerId == myId,
                    avatarUri = channel.avatars.firstOrNull()?.uri,
                    noCopy = channel.noCopy,
                    topBarActions = createTopBarActions(
                        channel.ownerId == myId,
                        channel.isSubscribed,
                        ChatType.CHANNEL
                    )
                )
            }
            if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
        }
    }

    private suspend fun loadGroupInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            groupRepository.fetchById(chatId)
        }
        loadMyGroupPermissions(chatId)
        loadMemberTags(chatId)
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
                    isOwner = group.ownerId == myId,
                    avatarUri = group.avatars.firstOrNull()?.uri,
                    noCopy = group.noCopy,
                    topBarActions = createTopBarActions(
                        group.ownerId == myId,
                        group.isMember,
                        ChatType.GROUP
                    )
                )
            }
            if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
        }
    }

    private fun loadMemberTags(chatId: Long) {
        viewModelScope.launch {
            groupAdminsRepository.getMemberTags(chatId).onSuccess { tags ->
                if (tags.isEmpty() && _uiState.value.memberTagsCache.isEmpty()) return@onSuccess
                _uiState.update { it.copy(memberTagsCache = tags) }
                if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
            }
        }
    }

    private fun loadMyGroupPermissions(chatId: Long) {
        viewModelScope.launch {
            groupAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
                _uiState.update { it.copy(myPermissions = permissions) }
            }
        }
    }

    private fun loadMyChannelPermissions(chatId: Long) {
        viewModelScope.launch {
            channelAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
                _uiState.update { it.copy(myPermissions = permissions) }
            }
        }
    }

    private suspend fun loadUserInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            userRepository.fetchById(chatId)
        }

        if (chatId == myId) {
            userRepository.getMe().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        chatName = UiText.StringResource(R.string.saved_messages),
                        subTitle = UiText.DynamicString(""),
                        avatarUri = user.avatars.firstOrNull()?.uri,
                        topBarActions = createTopBarActions(
                            isOwner = true,
                            isJoined = true,
                            type = ChatType.PRIVATE
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
                        topBarActions = createTopBarActions(
                            isOwner = false,
                            isJoined = true,
                            type = ChatType.PRIVATE
                        ),
                        isBlocked = user.isBlocked,
                        isBlockedByThem = user.isBlockedByThem
                    )
                }
            }
        }
    }

    /**
     * Пункты троеточия в шапке чата.
     *
     * «Очистить историю» здесь больше нет: в канале и группе она живёт в
     * «Управлении каналом» и «Управлении группой» рядом с удалением, где видно,
     * что чистится вся история и сразу для всех участников.
     */
    private fun createTopBarActions(
        isOwner: Boolean,
        isJoined: Boolean,
        type: ChatType
    ): List<TopBarAction> {
        val actions = mutableListOf<DropdownMenuAction>()

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

        /*
         * Троеточие показываем всегда, даже с пустым списком действий: «Медиа»,
         * поиск и уведомления живут внутри этого же меню, и у владельца канала
         * без него не осталось бы точки входа ни туда, ни туда.
         */
        return listOf(TopBarAction(icon = Icons.Rounded.MoreVert, dropdownActions = actions))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMessages() {
        _uiState.update { it.copy(isLoading = true) }
        val chatId = _uiState.value.chatId

        windowObserverJob?.cancel()
        windowObserverJob = viewModelScope.launch {
            val userId = userRepository.getMe().first().id

            val pager = MessageWindowPager(
                chatRepository = chatRepository,
                userId = userId,
                chatId = chatId,
                pageSize = PAGE_SIZE,
                aroundRadius = AROUND_RADIUS,
                maxWindowMessages = MAX_WINDOW_MESSAGES
            )
            messagePager = pager

            launch {
                pager.state.collect { window ->
                    _uiState.update {
                        it.copy(
                            isLoadingOlder = window.isLoadingBefore,
                            isLoadingNewer = window.isLoadingAfter,
                            isLoadingMore = window.isLoadingBefore || window.isLoadingAfter,
                            hasMoreMessages = window.hasMoreBefore,
                            hasMoreNewerMessages = window.hasMoreAfter,
                            isRelocating = window.isRelocating,
                            isAtLiveEdge = window.isAtLive
                        )
                    }
                }
            }

            launch {
                pager.state
                    .map { it.bounds }
                    .distinctUntilChanged()
                    .flatMapLatest { bounds ->
                        chatRepository.getMessagesWindowFlow(userId, chatId, bounds)
                    }
                    .collect { messages ->
                        updateChatItems(messages)
                        _uiState.update { it.copy(isLoading = false) }

                        if (!isFirstLoadDone && pager.state.value.isInitialized) {
                            isFirstLoadDone = true
                            _uiState.update { it.copy(isFirstLoadDone = true) }
                            val hasUnreadTarget = _uiState.value.firstUnreadMessageId != null
                            if (messages.isNotEmpty() && !hasUnreadTarget) {
                                requestScrollTo(
                                    messageId = null,
                                    highlight = false,
                                    animate = false
                                )
                            }
                        }

                        handleNewestMessage(messages, pager.state.value.isAtLive)
                    }
            }

            val firstUnreadId = pager.openAtFirstUnread()
            _uiState.update { it.copy(firstUnreadMessageId = firstUnreadId) }
            if (firstUnreadId != null) {
                unreadAnchorMessageId = firstUnreadId
                updateChatItems(lastMessages)

                requestScrollTo(
                    messageId = firstUnreadId,
                    highlight = false,
                    animate = false,
                    viewportFraction = UNREAD_VIEWPORT_FRACTION
                )
            }
            if (!isFirstLoadDone) {
                isFirstLoadDone = true
                _uiState.update { it.copy(isFirstLoadDone = true, isLoading = false) }
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
        lastMessages = messages
        val mapper = ChatItemMapper(
            context = context,
            myId = _uiState.value.myId,
            chatId = _uiState.value.chatId,
            isOwner = _uiState.value.isOwner,
            isJoined = _uiState.value.isJoined,
            userNamesCache = _uiState.value.userNamesCache,
            memberTagsCache = _uiState.value.memberTagsCache,
            groupReadInfo = _uiState.value.groupReadInfo,
            highlightedMessageId = _uiState.value.highlightedMessageId,
            unreadAnchorMessageId = unreadAnchorMessageId,
            copyPolicy = copyPolicy,
            onCopyText = ::copyToClipboard,
            onEditMessage = ::startEditing,
            onDeleteMessage = {
                showDeleteMessageDialog()
                selectMessage(it)
            },
            onRetrySendMessage = ::retrySendMessage,
            onCancelSendMessage = ::cancelSendMessage,
            onReplyMessage = ::startReply,
            onForwardMessage = ::startForward,
            onLoadUserName = ::loadUserName
        )

        val chatItems = mapper.map(messages).asReversed()
        val newMediaItems = messages.flatMap { it.attachments }
            .filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO || it.type == AttachmentType.GIF }

        _uiState.update { it.copy(chatItems = chatItems, mediaItems = newMediaItems) }

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

    // region Прокрутка к сообщению и догрузка окна
    fun loadOlderMessages() {
        val pager = messagePager ?: return
        viewModelScope.launch { pager.loadBefore() }
    }

    fun loadNewerMessages() {
        val pager = messagePager ?: return
        viewModelScope.launch { pager.loadAfter() }
    }

    fun loadMoreMessages() = loadOlderMessages()

    fun jumpToMessage(messageId: Long, returnToMessageId: Long? = null) {
        val pager = messagePager ?: return
        viewModelScope.launch {
            if (returnToMessageId != null) {
                returnStack.addLast(returnToMessageId)
                _uiState.update { it.copy(canJumpBack = true) }
            }

            if (!pager.containsMessage(messageId)) {
                val loaded = pager.jumpTo(messageId)
                if (!loaded) {
                    _uiEffect.emit(
                        ChatUiEffect.ShowSnackbar(
                            UiText.DynamicString("Не удалось перейти к сообщению")
                        )
                    )
                    vibrationManager.vibrate(VibrationPattern.Error)
                    return@launch
                }
            }

            requestScrollTo(messageId = messageId, highlight = true, animate = false)
        }
    }

    fun jumpToMessageWhenReady(messageId: Long) {
        viewModelScope.launch {
            var attempts = 0
            while (messagePager == null && attempts < 50) {
                delay(100.milliseconds)
                attempts++
            }
            if (messagePager == null) return@launch
            jumpToMessage(messageId)
        }
    }

    fun jumpBack() {
        val messageId = returnStack.removeLastOrNull() ?: return
        _uiState.update { it.copy(canJumpBack = returnStack.isNotEmpty()) }
        jumpToMessage(messageId)
    }

    fun jumpToLatest() {
        viewModelScope.launch { jumpToLatestInternal() }
    }

    private suspend fun jumpToLatestInternal() {
        val pager = messagePager ?: return
        val wasAtLive = pager.state.value.isAtLive
        if (!wasAtLive) pager.openAtLatest()
        returnStack.clear()
        _uiState.update { it.copy(canJumpBack = false) }
        requestScrollTo(messageId = null, highlight = false, animate = wasAtLive)

        _uiState.update { it.copy(unreadCount = 0, firstUnreadMessageId = null) }
        chatRepository.markAllAsRead(_uiState.value.chatId)
    }

    private fun requestScrollTo(
        messageId: Long?,
        highlight: Boolean,
        animate: Boolean,
        viewportFraction: Float = 1f / 3f
    ) {
        _uiState.update {
            it.copy(
                scrollTarget = ScrollTarget(
                    messageId = messageId,
                    highlight = highlight,
                    animate = animate,
                    viewportFraction = viewportFraction
                )
            )
        }
    }

    fun onScrollTargetHandled(requestId: Long) {
        val target = _uiState.value.scrollTarget ?: return
        if (target.requestId != requestId) return
        _uiState.update { it.copy(scrollTarget = null) }
        val messageId = target.messageId
        if (target.highlight && messageId != null) highlightMessage(messageId)
    }

    private fun highlightMessage(messageId: Long) {
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            _uiState.update { it.copy(highlightedMessageId = messageId) }
            updateChatItems(lastMessages)
            delay(2000.milliseconds)
            _uiState.update { it.copy(highlightedMessageId = null) }
            updateChatItems(lastMessages)
        }
    }
    // endregion

    // region Поиск сообщений в чате
    fun startMessageSearch() = _uiState.update {
        /* Поиск всегда открывается в режиме «В чате»: список — это уже выбор пользователя. */
        it.copy(isMessageSearchActive = true, isMessageSearchListMode = false)
    }

    fun stopMessageSearch() {
        searchJob?.cancel()
        searchCursorId = null
        _uiState.update {
            it.copy(
                isMessageSearchActive = false,
                isMessageSearchListMode = false,
                messageSearchQuery = "",
                messageSearchResults = emptyList(),
                isSearchingMessages = false,
                hasMoreSearchResults = false,
                messageSearchTotal = 0,
                isMessageSearchTotalExact = true,
                messageSearchIndex = -1,
                messageSearchSenders = emptyMap()
            )
        }
    }

    fun changeMessageSearchQuery(query: String) {
        _uiState.update { it.copy(messageSearchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            clearMessageSearchResults()
            return
        }

        searchJob = viewModelScope.launch {
            delay(350.milliseconds)
            searchCursorId = null
            runMessageSearch(query, reset = true)
        }
    }

    /**
     * Крестик в поле поиска.
     *
     * Гасит и запрос, и счётчик результатов снизу, но сам режим поиска оставляет
     * включённым: закрывает его только кнопка «назад».
     */
    fun clearMessageSearchQuery() {
        searchJob?.cancel()
        _uiState.update { it.copy(messageSearchQuery = "") }
        clearMessageSearchResults()
    }

    private fun clearMessageSearchResults() {
        searchCursorId = null
        _uiState.update {
            it.copy(
                messageSearchResults = emptyList(),
                hasMoreSearchResults = false,
                isSearchingMessages = false,
                messageSearchTotal = 0,
                isMessageSearchTotalExact = true,
                messageSearchIndex = -1
            )
        }
    }

    /** Переключает «Списком» и «В чате». */
    fun toggleMessageSearchDisplayMode() {
        val goingToChat = _uiState.value.isMessageSearchListMode
        _uiState.update { it.copy(isMessageSearchListMode = !goingToChat) }

        /*
         * Возврат в чат без выбранного результата бесполезен: показываем то же
         * самое новое совпадение, что и сразу после ввода запроса.
         */
        if (goingToChat && _uiState.value.messageSearchIndex < 0) selectSearchResult(0)
    }

    fun loadMoreSearchResults() {
        val query = _uiState.value.messageSearchQuery
        if (query.isBlank() || searchCursorId == null || _uiState.value.isSearchingMessages) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runMessageSearch(query, reset = false) }
    }

    /**
     * Стрелка «вверх»: к более старому совпадению, то есть выше по чату.
     *
     * Если загруженная страница закончилась, сначала догружаем следующую и только
     * потом прыгаем — иначе на границе страницы кнопка молча ничего не делала бы.
     */
    fun goToOlderSearchResult() {
        val state = _uiState.value
        val target = state.messageSearchIndex + 1

        if (target < state.messageSearchResults.size) {
            selectSearchResult(target)
            return
        }

        if (!state.hasMoreSearchResults || state.isSearchingMessages) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runMessageSearch(state.messageSearchQuery, reset = false)
            if (target < _uiState.value.messageSearchResults.size) selectSearchResult(target)
        }
    }

    /** Стрелка «вниз»: к более новому совпадению, то есть ниже по чату. */
    fun goToNewerSearchResult() {
        val target = _uiState.value.messageSearchIndex - 1
        if (target < 0) return
        selectSearchResult(target)
    }

    fun onSearchResultClicked(hit: MessageSearchHit) {
        val index = _uiState.value.messageSearchResults.indexOfFirst { it.id == hit.id }
        if (index < 0) {
            _uiState.update { it.copy(isMessageSearchListMode = false) }
            jumpToMessage(hit.id)
            return
        }
        selectSearchResult(index)
    }

    /**
     * Переход к совпадению по его позиции.
     *
     * Список результатов закрывается, а сообщение подсвечивается теми же двумя
     * секундами, что и при переходе по ответу.
     */
    private fun selectSearchResult(index: Int) {
        val hit = _uiState.value.messageSearchResults.getOrNull(index) ?: return
        _uiState.update { it.copy(messageSearchIndex = index, isMessageSearchListMode = false) }
        jumpToMessage(hit.id)
    }

    private suspend fun runMessageSearch(query: String, reset: Boolean) {
        _uiState.update { it.copy(isSearchingMessages = true) }
        chatRepository.searchMessages(
            chatId = _uiState.value.chatId,
            query = query,
            cursorId = if (reset) null else searchCursorId,
            limit = SEARCH_PAGE_SIZE
        ).onSuccess { page ->
            searchCursorId = page.nextCursorId
            _uiState.update {
                it.copy(
                    messageSearchResults = if (reset) page.items else it.messageSearchResults + page.items,
                    hasMoreSearchResults = page.nextCursorId != null,
                    isSearchingMessages = false,
                    /* Со второй страницей total не приходит: сервер считает его один раз. */
                    messageSearchTotal = page.total ?: it.messageSearchTotal,
                    isMessageSearchTotalExact = if (reset) page.totalIsExact
                    else it.isMessageSearchTotalExact,
                    messageSearchIndex = if (reset) -1 else it.messageSearchIndex
                )
            }
            loadSearchSenders(page.items)

            /* Сразу показываем самое новое совпадение, как при переходе по ответу. */
            if (reset && page.items.isNotEmpty()) {
                if (_uiState.value.isMessageSearchListMode) {
                    _uiState.update { it.copy(messageSearchIndex = 0) }
                } else {
                    selectSearchResult(0)
                }
            }
        }.onFailure {
            _uiState.update { it.copy(isSearchingMessages = false) }
        }
    }

    /**
     * Подтягивает имена и аватарки отправителей найденных сообщений.
     *
     * В канале автор любого поста — сам канал, поэтому карточка результата берёт
     * имя и аватарку чата, а ходить в userRepository не за чем.
     */
    private fun loadSearchSenders(hits: List<MessageSearchHit>) {
        if (ChatType.fromId(_uiState.value.chatId) == ChatType.CHANNEL) return

        hits.map { it.senderId }
            .distinct()
            .filter { !_uiState.value.messageSearchSenders.containsKey(it) }
            .forEach { senderId ->
                viewModelScope.launch {
                    try {
                        userRepository.getById(senderId).collect { user ->
                            val sender = MessageSearchSender(
                                name = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                                avatarUri = user.avatars.firstOrNull()?.uri
                            )
                            _uiState.update {
                                it.copy(
                                    messageSearchSenders = it.messageSearchSenders + (senderId to sender)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error loading search sender", e)
                    }
                }
            }
    }
    // endregion

    private companion object {
        const val PAGE_SIZE = 50
        const val AROUND_RADIUS = 25
        const val MAX_WINDOW_MESSAGES = 400
        const val UNREAD_VIEWPORT_FRACTION = 0.08f

        /** Размер страницы результатов поиска: больше 50 сервер за раз не отдаёт. */
        const val SEARCH_PAGE_SIZE = 50
    }

    // region Ответ на сообщение
    fun startReply(message: Message) {
        if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

        if (_uiState.value.editingMessageId != null) cancelEditing()

        val state = _uiState.value
        val chatType = ChatType.fromId(state.chatId)

        val senderName = when {
            message.senderId == state.myId -> state.myName
            else -> state.userNamesCache[message.senderId]
                ?: state.chatName.asString(context)
        }

        val preview = MessageReplyPreview(
            messageId = message.id,
            chatId = state.chatId,
            senderId = message.senderId,
            senderName = senderName,
            chatName = if (chatType == ChatType.PRIVATE) null
            else state.chatName.asString(context),
            text = message.text,
            attachmentTypes = message.attachments.map { it.type }
        )

        if (chatType != ChatType.PRIVATE) loadUserName(message.senderId)

        _uiState.update { it.copy(replyToMessage = preview) }
        replyDraftCache.save(state.myId, state.chatId, preview)
    }

    fun onReplyPanelClicked() {
        val preview = _uiState.value.replyToMessage ?: return
        jumpToMessage(preview.messageId)
    }

    fun cancelReply() {
        clearReply()
    }

    private fun clearReply() {
        replyDraftCache.clear(_uiState.value.myId, _uiState.value.chatId)
        if (_uiState.value.replyToMessage == null) return
        _uiState.update { it.copy(replyToMessage = null) }
    }

    fun onReplyPreviewClicked(message: Message) {
        val preview = message.replyTo ?: return

        if (isInCurrentChat(preview)) {
            jumpToMessage(preview.messageId, returnToMessageId = message.id)
        } else {
            val targetChatId = preview.chatId ?: return
            viewModelScope.launch {
                _uiEffect.emit(
                    ChatUiEffect.NavigateToChat(
                        chatId = targetChatId,
                        scrollToMessageId = preview.messageId
                    )
                )
            }
        }
    }

    private fun isInCurrentChat(preview: MessageReplyPreview): Boolean {
        val state = _uiState.value
        val originChatId = preview.chatId ?: return true
        if (originChatId == state.chatId) return true
        return ChatType.fromId(state.chatId) == ChatType.PRIVATE && originChatId == state.myId
    }
    // endregion

    // region Пересылка сообщения
    fun startForward(message: Message) {
        if (!copyPolicy.canForward) return
        if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

        viewModelScope.launch {
            val myId = _uiState.value.myId
            val chats = chatRepository.getAllChats().firstOrNull().orEmpty()

            val candidates = chats.filter { chat ->
                when (ChatType.fromId(chat.id)) {
                    ChatType.CHANNEL ->
                        channelRepository.getByIdOrNull(chat.id)
                            .firstOrNull()?.ownerId == myId

                    ChatType.UNKNOWN -> false
                    else -> true
                }
            }

            _uiState.update {
                it.copy(
                    forwardingMessage = message,
                    forwardCandidates = candidates,
                    selectedForwardChatIds = emptySet(),
                    isForwarding = false,
                    isForwardSheetVisible = true
                )
            }
        }
    }

    fun toggleForwardTarget(chatId: Long) {
        _uiState.update { state ->
            val selected = state.selectedForwardChatIds
            state.copy(
                selectedForwardChatIds = if (chatId in selected) selected - chatId
                else selected + chatId
            )
        }
    }

    fun dismissForwardSheet() {
        _uiState.update {
            it.copy(
                isForwardSheetVisible = false,
                forwardingMessage = null,
                forwardCandidates = emptyList(),
                selectedForwardChatIds = emptySet(),
                isForwarding = false
            )
        }
    }

    fun confirmForward() {
        if (!copyPolicy.canForward) return
        val state = _uiState.value
        val message = state.forwardingMessage ?: return
        val targets = state.selectedForwardChatIds.toList()
        if (targets.isEmpty() || state.isForwarding) return

        _uiState.update { it.copy(isForwarding = true) }

        viewModelScope.launch {
            chatRepository.forwardMessage(state.chatId, message.id, targets)
                .onSuccess {
                    dismissForwardSheet()
                    if (targets.contains(state.chatId)) {
                        if (!_uiState.value.isAtLiveEdge) jumpToLatestInternal()
                        requestScrollTo(messageId = null, highlight = false, animate = true)
                    }
                    _uiEffect.emit(
                        ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_forwarded))
                    )
                }
                .onFailure { error ->
                    Log.e("ChatViewModel", "Error forwarding message", error)
                    _uiState.update { it.copy(isForwarding = false) }
                    _uiEffect.emit(
                        ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.forward_failed))
                    )
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
        }
    }

    fun onForwardedFromClicked(message: Message) {
        val forwardedFrom = message.forwardedFrom ?: return
        if (forwardedFrom.access != ForwardSourceAccess.OPEN) return
        if (forwardedFrom.chatId == _uiState.value.chatId) return

        viewModelScope.launch {
            _uiEffect.emit(ChatUiEffect.NavigateToChat(chatId = forwardedFrom.chatId))
        }
    }
    // endregion

    // region Message Actions
    fun changeText(newText: String) {
        _uiState.update { it.copy(messageText = newText) }

        draftSaveJob?.cancel()
        draftSaveJob = if (newText.isNotBlank()) {
            viewModelScope.launch {
                delay(500.milliseconds)
                chatRepository.saveDraft(_uiState.value.chatId, newText)
            }
        } else {
            viewModelScope.launch {
                chatRepository.deleteDraft(_uiState.value.chatId)
            }
        }
    }

    fun onSendMessageClicked() {
        val editingId = _uiState.value.editingMessageId
        if (editingId != null) {
            viewModelScope.launch { handleEditMessage(editingId) }
            return
        }

        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return

        val replyTo = _uiState.value.replyToMessage
        changeText("")
        clearReply()
        onSendMessageInternal(text, replyTo)
    }

    private fun onSendMessageInternal(
        text: String,
        replyTo: MessageReplyPreview? = null
    ) {
        val tempId = -System.currentTimeMillis()
        val job = viewModelScope.launch {
            try {
                if (!_uiState.value.isAtLiveEdge) jumpToLatestInternal()
                sendMessageUseCase(
                    _uiState.value.chatId,
                    message = text,
                    tempId = tempId,
                    replyTo = replyTo
                )
                requestScrollTo(messageId = null, highlight = false, animate = true)
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
                    onSendMessageInternal(text, message.replyTo)
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
        if (message.forwardedFrom != null) return

        val now = System.currentTimeMillis()
        if (now - message.sendTime > 24 * 60 * 60 * 1000L) {
            viewModelScope.launch {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.edit_time_limit_error)))
            }
            return
        }
        if (message.text.isNullOrBlank()) return

        clearReply()

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

    fun onMessagesSeen(messageId: Long) {
        if (messageId <= reportedReadUpToId) return
        reportedReadUpToId = messageId
        viewModelScope.launch {
            chatRepository.markReadUpTo(_uiState.value.chatId, messageId)
        }
    }

    fun onViewportAtBottomChanged(atBottom: Boolean) {
        isViewportAtBottom = atBottom
    }

    private fun handleNewestMessage(messages: List<Message>, isAtLive: Boolean) {
        val newest = messages.lastOrNull() ?: return
        val newestId = newest.id
        if (newestId <= lastKnownNewestId) return

        val isFirstFill = lastKnownNewestId == 0L
        lastKnownNewestId = newestId
        if (isFirstFill) return

        val isMine = newest.senderId == _uiState.value.myId
        if (isMine || (isViewportAtBottom && isAtLive)) {
            requestScrollTo(messageId = null, highlight = false, animate = true)
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
        return items.asReversed().filterIsInstance<ChatItem.MessageItem>()
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
        val replyTo = _uiState.value.replyToMessage
        clearReply()
        val job = viewModelScope.launch {
            try {
                sendMessageWithFilesUseCase(
                    _uiState.value.chatId,
                    uris,
                    null,
                    tempId,
                    replyTo
                )
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
        if (!copyPolicy.canSaveMedia) return
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
        if (!copyPolicy.canSaveMedia) return
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
    fun copyToClipboard(text: String?) {
        if (!copyPolicy.canCopyText) return
        text?.let { clipboardService.copy(it) }
    }

    fun vibrate() = vibrationManager.vibrate(VibrationPattern.Error)

    fun vibrateTactile() = vibrationManager.vibrate(VibrationPattern.TactileResponse)

    fun selectMessage(message: Message) =
        _uiState.update { it.copy(selectedMessages = it.selectedMessages + message) }

    fun clearMediaUrl() = _uiState.update { it.copy(showFullScreenViewer = false) }
    fun setVideoLooping(isLooping: Boolean) =
        viewModelScope.launch { dataStoreManager.saveVideoLooping(isLooping) }

    fun setVideoPlaybackSpeed(speed: Float) =
        viewModelScope.launch { dataStoreManager.saveVideoPlaybackSpeed(speed) }

    fun dismissBannedDial