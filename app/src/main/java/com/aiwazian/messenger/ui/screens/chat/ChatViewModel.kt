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
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
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

    /** Стек возврата: id сообщений, из которых был совершён переход. */
    private val returnStack = ArrayDeque<Long>()

    /** Видимо ли сейчас самое нижнее сообщение списка. */
    private var isViewportAtBottom = true

    /**
     * Сообщение, перед которым рисуется «Unread messages». Ставится один раз при
     * открытии чата и не меняется, когда счётчик непрочитанных ползёт вниз.
     */
    private var unreadAnchorMessageId: Long? = null

    /** Максимальный id, про который уже сказали серверу «прочитано». */
    private var reportedReadUpToId = 0L

    /** Самый новый id в окне: нужен, чтобы отличить новое сообщение от перерисовки. */
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

    /**
     * Открытие чата само по себе ничего не помечает прочитанным.
     *
     * Отметка уходит только из onMessagesSeen — по сообщениям, которые реально
     * показались на экране больше чем наполовину.
     */
    private suspend fun loadGroupInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            groupRepository.fetchById(chatId)
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
                            type = ChatType.PRIVATE,
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

        /*
         * UI рисует список с reverseLayout, поэтому отдаём его уже перевёрнутым:
         * нулевой элемент — самое новое сообщение и «низ» чата.
         * Группировка и разделители считаются по-старому, по хронологии.
         */
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

    /** Скролл вверх: страница старше текущего окна. */
    fun loadOlderMessages() {
        val pager = messagePager ?: return
        viewModelScope.launch { pager.loadBefore() }
    }

    /** Скролл вниз: страница новее текущего окна (актуально после прыжка в середину). */
    fun loadNewerMessages() {
        val pager = messagePager ?: return
        viewModelScope.launch { pager.loadAfter() }
    }

    /** Совместимость со старым вызовом из UI. */
    fun loadMoreMessages() = loadOlderMessages()

    /**
     * Единая точка входа для перехода к любому сообщению чата:
     * ответы, закреплённые, результаты поиска.
     *
     * Промежуточные сообщения не грузятся: сразу берётся окно вокруг цели.
     *
     * @param messageId id целевого сообщения.
     * @param returnToMessageId id сообщения, из которого прыгаем (для кнопки «вернуться»).
     */
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

    /**
     * Прыжок к сообщению сразу после открытия чата.
     *
     * Окно сообщений в этот момент ещё может грузиться, поэтому ждём
     * инициализацию пейджера.
     */
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

    /** Возврат к сообщению, из которого был совершён переход. */
    fun jumpBack() {
        val messageId = returnStack.removeLastOrNull() ?: return
        _uiState.update { it.copy(canJumpBack = returnStack.isNotEmpty()) }
        jumpToMessage(messageId)
    }

    /**
     * FloatingActionButton: сразу в конец чата.
     * Один запрос за последними сообщениями, без прокрутки и догрузки промежуточных страниц.
     */
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

    /** Вызывается из UI, когда скролл к цели выполнен. */
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
    fun startMessageSearch() = _uiState.update { it.copy(isMessageSearchActive = true) }

    fun stopMessageSearch() {
        searchJob?.cancel()
        searchCursorId = null
        _uiState.update {
            it.copy(
                isMessageSearchActive = false,
                messageSearchQuery = "",
                messageSearchResults = emptyList(),
                isSearchingMessages = false,
                hasMoreSearchResults = false
            )
        }
    }

    fun changeMessageSearchQuery(query: String) {
        _uiState.update { it.copy(messageSearchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            searchCursorId = null
            _uiState.update {
                it.copy(
                    messageSearchResults = emptyList(),
                    hasMoreSearchResults = false,
                    isSearchingMessages = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350.milliseconds)
            searchCursorId = null
            runMessageSearch(query, reset = true)
        }
    }

    fun loadMoreSearchResults() {
        val query = _uiState.value.messageSearchQuery
        if (query.isBlank() || searchCursorId == null || _uiState.value.isSearchingMessages) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runMessageSearch(query, reset = false) }
    }

    private suspend fun runMessageSearch(query: String, reset: Boolean) {
        _uiState.update { it.copy(isSearchingMessages = true) }
        chatRepository.searchMessages(
            chatId = _uiState.value.chatId,
            query = query,
            cursorId = if (reset) null else searchCursorId
        ).onSuccess { page ->
            searchCursorId = page.nextCursorId
            _uiState.update {
                it.copy(
                    messageSearchResults = if (reset) page.items else it.messageSearchResults + page.items,
                    hasMoreSearchResults = page.nextCursorId != null,
                    isSearchingMessages = false
                )
            }
        }.onFailure {
            _uiState.update { it.copy(isSearchingMessages = false) }
        }
    }

    /** Клик по результату поиска. */
    fun onSearchResultClicked(hit: MessageSearchHit) = jumpToMessage(hit.id)
    // endregion

    private companion object {
        const val PAGE_SIZE = 50
        const val AROUND_RADIUS = 25
        const val MAX_WINDOW_MESSAGES = 400

        /** Первое непрочитанное ставим ниже середины экрана. */
        /**
         * Граница прочитанного уезжает почти к верхней кромке: всё непрочитанное
         * должно быть ниже него, чтобы счётчик таял по мере скролла вниз.
         */
        const val UNREAD_VIEWPORT_FRACTION = 0.08f
    }

    // region Ответ на сообщение

    /**
     * «Ответить» в меню сообщения.
     *
     * Превью собирается сразу, чтобы панель над полем ввода и локальное
     * pending-сообщение выглядели одинаково до ответа сервера.
     *
     * Заголовок: в группе и канале — название чата, в личном чате — имя автора
     * сообщения (своё имя, если отвечаем сами себе).
     *
     * Ответ и редактирование взаимоисключают друг друга: если шла правка
     * сообщения, она отменяется, а поле ввода очищается.
     */
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
    }

    /** Клик по панели ответа над полем ввода — прыжок к цитируемому сообщению. */
    fun onReplyPanelClicked() {
        val preview = _uiState.value.replyToMessage ?: return
        jumpToMessage(preview.messageId)
    }

    /** Крестик в панели ответа: сбрасываем ответ и чистим поле ввода. */
    fun cancelReply() {
        clearReply()
        changeText("")
    }

    private fun clearReply() {
        if (_uiState.value.replyToMessage == null) return
        _uiState.update { it.copy(replyToMessage = null) }
    }

    /**
     * Клик по блоку ответа внутри сообщения.
     *
     * Оригинал в этом же чате — прыжок по истории с возможностью вернуться,
     * иначе — открываем чужой чат сразу на нужном сообщении.
     */
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

    /**
     * В личном чате chatId сообщения — это получатель, поэтому у двух сообщений
     * одного диалога chatId разные: мой id и id собеседника.
     */
    private fun isInCurrentChat(preview: MessageReplyPreview): Boolean {
        val state = _uiState.value
        val originChatId = preview.chatId ?: return true
        if (originChatId == state.chatId) return true
        return ChatType.fromId(state.chatId) == ChatType.PRIVATE &&
                (originChatId == state.myId || originChatId == state.chatId)
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

    /** Отправка копий во все выбранные чаты одним запросом. */
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

    /**
     * Клик по заголовку «Переслано от…».
     *
     * Доступность считает сервер: публичные и подписанные чаты — OPEN,
     * закрытые и скрытые настройками приватности — RESTRICTED.
     * Тултип при RESTRICTED показывает сам MessageBubble.
     */
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

    /**
     * «Изменить» в меню сообщения.
     *
     * Пересланные сообщения редактировать нельзя: это копия чужого текста,
     * такую же проверку делает сервер.
     *
     * Редактирование и ответ взаимоисключают друг друга: начатый ответ
     * сбрасывается, а в поле ввода остаётся только текст правимого сообщения.
     */
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

    /**
     * Сообщения до messageId реально побывали в поле зрения больше чем наполовину.
     *
     * Единственное место, откуда уходит отметка о прочтении: ни открытие чата,
     * ни загрузка истории сами по себе ничего прочитанным не помечают.
     *
     * Вызывается из UI с дебаунсом и всегда одним максимальным id: один запрос
     * на пачку вместо запроса на каждое сообщение.
     */
    fun onMessagesSeen(messageId: Long) {
        if (messageId <= reportedReadUpToId) return
        reportedReadUpToId = messageId
        viewModelScope.launch {
            chatRepository.markReadUpTo(_uiState.value.chatId, messageId)
        }
    }

    /** Сообщается из UI: видно ли самое нижнее сообщение списка. */
    fun onViewportAtBottomChanged(atBottom: Boolean) {
        isViewportAtBottom = atBottom
    }

    /**
     * Новое сообщение в окне.
     *
     * Крутим вниз только если пользователь уже стоит внизу либо отправил своё:
     * иначе чтение старой переписки прерывалось бы прыжком в конец.
     */
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

    /**
     * Очередь голосовых идёт по времени, поэтому перевёрнутый для UI список
     * разворачивается обратно: иначе автопереход играл бы сообщения вспять.
     */
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

    /** Копирование текста в буфер обмена. При запрете копирования не выполняется. */
    fun copyToClipboard(text: String?) {
        if (!copyPolicy.canCopyText) return
        text?.let { clipboardService.copy(it) }
    }

    fun vibrate() = vibrationManager.vibrate(VibrationPattern.Error)

    /**
     * Короткий тактильный отклик.
     *
     * Свайп сообщения влево вызывает его один раз, когда палец перешёл порог,
     * после которого отпускание начнёт ответ.
     */
    fun vibrateTactile() = vibrationManager.vibrate(VibrationPattern.TactileResponse)

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

    /**
     * Почтовый адрес не открываем в CustomTabs как ссылку: нужно почтовое приложение.
     */
    fun onEmailClicked(email: String) {
        viewModelScope.launch { _uiEffect.emit(ChatUiEffect.OpenEmail(email)) }
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
                if (info.requireApproval) {
                    _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.DynamicString("Заявка отправлена")))
                } else {
                    _uiEffect.emit(ChatUiEffect.NavigateToChat(info.chatId))
                }
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
