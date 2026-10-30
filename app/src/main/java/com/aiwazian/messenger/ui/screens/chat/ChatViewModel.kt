/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.playback.MusicAutoPauseController
import com.aiwazian.messenger.playback.MusicPlayerManager
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.repository.ChatPinnedMessagesRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.ReplyDraftCache
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.repository.WallpaperRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import com.aiwazian.messenger.ui.app.GlobalBottomSheetManager
import com.aiwazian.messenger.socket.OutgoingSocketEvent
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputActions
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiText
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextPart
import com.aiwazian.messenger.utils.AudioRecorderManager
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.EqualizerManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.MessageSendQueue
import com.aiwazian.messenger.utils.UploadManager
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.push.NotificationHelper
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.ui.screens.chat.paging.MessageWindowPager
import com.aiwazian.messenger.usecase.JoinChannelUseCase
import com.aiwazian.messenger.usecase.JoinGroupUseCase
import com.aiwazian.messenger.usecase.JoinViaInviteLinkUseCase
import com.aiwazian.messenger.usecase.LeaveChatUseCase
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import com.aiwazian.messenger.usecase.SendContactUseCase
import com.aiwazian.messenger.usecase.SendStickerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    @param:ApplicationContext internal val context: Context,
    internal val chatRepository: ChatRepository,
    internal val pinnedMessagesRepository: ChatPinnedMessagesRepository,
    internal val fileRepository: FileRepository,
    internal val channelRepository: ChannelRepository,
    internal val groupRepository: GroupRepository,
    internal val channelAdminsRepository: ChannelAdminsRepository,
    internal val groupAdminsRepository: GroupAdminsRepository,
    internal val userRepository: UserRepository,
    internal val inviteLinkRepository: InviteLinkRepository,
    internal val searchRepository: SearchRepository,
    private val emojiRepository: EmojiRepository,
    internal val replyDraftCache: ReplyDraftCache,
    internal val clipboardService: ClipboardService,
    private val webSocketClient: WebSocketClient,
    internal val downloaderManager: DownloaderManager,
    internal val uploadManager: UploadManager,
    internal val vibrationManager: VibrationManager,
    internal val fileHandler: FileHandler,
    internal val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase,
    internal val joinChannelUseCase: JoinChannelUseCase,
    internal val joinGroupUseCase: JoinGroupUseCase,
    internal val joinViaInviteLinkUseCase: JoinViaInviteLinkUseCase,
    internal val leaveChatUseCase: LeaveChatUseCase,
    private val dataStoreManager: DataStoreManager,
    private val voicePlayerManager: VoicePlayerManager,
    private val musicPlayerManager: MusicPlayerManager,
    private val musicAutoPauseController: MusicAutoPauseController,
    private val equalizerManager: EqualizerManager,
    internal val onlineUsersTracker: OnlineUsersTracker,
    internal val realtimeEventSyncService: RealtimeEventSyncService,
    private val notificationHelper: NotificationHelper,
    private val messageSendQueue: MessageSendQueue,
    internal val wallpaperRepository: WallpaperRepository,
    private val sendStickerUseCase: SendStickerUseCase,
    private val sendContactUseCase: SendContactUseCase,
    internal val globalBottomSheetManager: GlobalBottomSheetManager
) : ViewModel(), ChatInputActions {

    fun showMusicPlayerSheet() {
        globalBottomSheetManager.showMusicPlayerSheet()
    }

    private val audioRecorderManager = AudioRecorderManager(context)
    private var recordingTimerJob: Job? = null
    private var draftSaveJob: Job? = null

    internal val sendingJobs = mutableMapOf<Long, Job>()

    internal val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    internal val _uiEffect = MutableSharedFlow<ChatUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    internal val playbackConnector = ChatPlaybackConnector(
        context = context,
        scope = viewModelScope,
        stateFlow = _uiState,
        updateState = { _uiState.update(it) },
        voicePlayerManager = voicePlayerManager,
        musicPlayerManager = musicPlayerManager,
        equalizerManager = equalizerManager
    )

    internal var isFirstLoadDone = false

    internal var messagePager: MessageWindowPager? = null
    internal var windowObserverJob: Job? = null
    internal var highlightJob: Job? = null
    internal var searchJob: Job? = null
    internal var searchCursorId: Long? = null
    internal var lastMessages: List<Message> = emptyList()

    internal val returnStack = ArrayDeque<Long>()

    internal var isViewportAtBottom = true

    internal var unreadAnchorMessageId: Long? = null

    internal var reportedReadUpToId = 0L

    internal var lastKnownNewestId = 0L

    private var isInit = false
    internal var autoDownloadMedia = false
    internal var autoDownloadPhotos = true
    internal var autoDownloadVideos = true
    internal var autoDownloadFiles = true

    internal val copyPolicy: ChatCopyPolicy
        get() = _uiState.value.copyPolicy

    internal val isSavedMessages: Boolean
        get() {
            val state = _uiState.value
            return ChatType.fromId(state.chatId) == ChatType.PRIVATE && state.chatId == state.myId
        }

    init {
        loadSettings()
        playbackConnector.observe()
        setupSocketConnectionObserver()
        observeViewOnceDeletions()
    }

    private fun observeViewOnceDeletions() {
        viewModelScope.launch {
            chatRepository.viewOnceAttachmentDeletions.collect { fileId ->
                _uiState.update { state ->
                    state.copy(
                        viewOnceDeadlines = state.viewOnceDeadlines.filterKeys { key ->
                            key.substringAfterLast(':') != fileId
                        },
                        deletedViewOnceFileIds = state.deletedViewOnceFileIds + fileId
                    )
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

        notificationHelper.clearChatNotifications(chatId)
        webSocketClient.emitEvent(
            OutgoingSocketEvent.CHAT_OPEN,
            mapOf("chatId" to chatId.toString())
        )

        refreshChatWallpaper(chatId)

        loadDraft(chatId)
        loadChatInfo()
        observeRealtimeEvents()
        observeMessages()
        observePinnedMessages()
        viewModelScope.launch {
            chatRepository.reconcileViewOnceAttachments(chatId)
        }
    }

    fun observeChatWallpaper(chatId: Long) = wallpaperRepository.observeChatWallpaper(chatId)

    private fun refreshChatWallpaper(chatId: Long) {
        viewModelScope.launch {
            wallpaperRepository.refreshChatWallpaper(chatId)
        }
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
        viewModelScope.launch {
            dataStoreManager.getKeyboardHeight().collect { height ->
                _uiState.update { it.copy(keyboardHeight = height) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getRaiseToListen().collect { enabled ->
                _uiState.update { it.copy(raiseToListen = enabled) }
            }
        }
    }

    override fun onKeyboardHeightChanged(height: Float) {
        if (height <= 0f) return

        viewModelScope.launch {
            val savedHeight = dataStoreManager.getKeyboardHeight().first()

            if (savedHeight != height) {
                dataStoreManager.saveKeyboardHeight(height)
            }
        }
    }

    private fun setupSocketConnectionObserver() {
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state == ConnectionState.CONNECTED) }

                if (state == ConnectionState.CONNECTED) {
                    val chatId = _uiState.value.chatId

                    if (chatId > 0) {
                        launch {
                            chatRepository.reconcileViewOnceAttachments(chatId)
                        }
                    }
                }
            }
        }
    }

    override fun changeText(text: String) {
        _uiState.update { it.copy(messageText = text) }

        draftSaveJob?.cancel()
        draftSaveJob = if (text.isNotBlank()) {
            viewModelScope.launch {
                delay(500.milliseconds)
                chatRepository.saveDraft(_uiState.value.chatId, text)
            }
        } else {
            viewModelScope.launch {
                chatRepository.deleteDraft(_uiState.value.chatId)
            }
        }
    }

    override fun onSendMessageClicked() {
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

    override fun onSendWithoutSoundClicked() {
        val editingId = _uiState.value.editingMessageId
        if (editingId != null) return

        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return

        val replyTo = _uiState.value.replyToMessage
        changeText("")
        clearReply()
        onSendMessageInternal(text, replyTo, silent = true)
    }

    override fun sendFiles(uris: List<Uri>) {
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

    override fun sendMediaUris(uris: List<Uri>, caption: String, replyTo: MessageReplyPreview?) {
        messageSendQueue.enqueueFiles(
            chatId = _uiState.value.chatId,
            uris = uris,
            text = caption.trim().ifBlank { null },
            replyTo = replyTo,
            videoQualities = emptyMap(),
            mediaTransforms = emptyMap(),
            mediaDrawings = emptyMap()
        )
    }

    override fun sendSticker(stickerId: Long) {
        val replyTo = _uiState.value.replyToMessage

        viewModelScope.launch {
            val result = sendStickerUseCase(
                chatId = _uiState.value.chatId,
                stickerId = stickerId,
                replyToId = replyTo?.messageId
            )

            if (result.isSuccess && replyTo != null) {
                clearReply()
            }
        }
    }

    fun sendContact(contact: MessageContact) {
        viewModelScope.launch {
            sendContactUseCase(
                chatId = _uiState.value.chatId,
                contact = contact
            )
        }
    }

    override fun cancelReply() {
        clearReply()
    }

    override fun onReplyPanelClicked() {
        val preview = _uiState.value.replyToMessage ?: return
        jumpToMessage(preview.messageId)
    }

    override fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingMessageId = null,
                editingOriginalText = null,
                messageText = ""
            )
        }
    }

    override fun startRecording() {
        if (_uiState.value.isRecording) return
        val file = audioRecorderManager.startRecording()
        if (file != null) {
            musicAutoPauseController.onRecordingStarted()
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

    override fun lockRecording() {
        if (_uiState.value.isRecording) {
            _uiState.update { it.copy(isRecordingLocked = true) }
            vibrationManager.vibrate(VibrationPattern.TactileResponse)
        }
    }

    override fun stopRecordingAndSend() {
        if (!_uiState.value.isRecording) return
        val file = audioRecorderManager.stopRecording()
        musicAutoPauseController.onRecordingStopped()
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

    override fun cancelRecording() {
        if (!_uiState.value.isRecording) return
        audioRecorderManager.cancelRecording()
        musicAutoPauseController.onRecordingStopped()
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

    override fun onMicrophonePermissionDenied() =
        _uiState.update { it.copy(showMicrophonePermissionSheet = true) }

    override fun showBlockDialog() {
        _uiState.update { it.copy(showBlockDialog = true) }
    }

    override fun onJoinClicked() {
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

    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) {
        playbackConnector.onVoiceSeek(file, positionMs)
    }

    fun onMusicSeek(file: MessageAttachment, positionMs: Int) {
        playbackConnector.onMusicSeek(file, positionMs)
    }

    fun seekMusicTo(positionMs: Int) {
        musicPlayerManager.seekTo(positionMs)
    }

    fun toggleMusicPlayPause() {
        musicPlayerManager.togglePlayPause()
    }

    fun toggleMusicRepeat() {
        musicPlayerManager.toggleRepeatMode()
    }

    fun playNextMusicTrack() {
        musicPlayerManager.next()
    }

    fun playPreviousMusicTrack() {
        musicPlayerManager.previous()
    }

    fun shareCurrentMusicTrack() {
        val fileId = _uiState.value.currentMusicFileId ?: return
        val message = _uiState.value.chatItems
            .filterIsInstance<ChatItem.MessageItem>()
            .firstOrNull { item -> item.message.attachments.any { it.fileId == fileId } }
            ?.message ?: return

        startForward(message)
    }

    fun stopMusic() {
        musicPlayerManager.stop()
    }

    fun stopVoice() {
        voicePlayerManager.stop()
    }

    fun toggleVoicePlayPause() {
        voicePlayerManager.togglePlayPause()
    }

    fun setEqualizerBandLevel(band: Int, levelMb: Int) {
        equalizerManager.setBandLevel(band, levelMb)
    }

    fun persistEqualizerBandLevels() {
        equalizerManager.persistBandLevels()
    }

    fun copyToClipboard(message: Message) {
        if (!copyPolicy.canCopyText()) return

        val text = message.text ?: return

        viewModelScope.launch {
            val emojiIds = CustomEmojiText.parse(text)
                .filterIsInstance<CustomEmojiTextPart.Emoji>()
                .map { it.emojiId }
                .distinct()

            val emojis = if (emojiIds.isEmpty()) {
                emptyMap<Long, CustomEmoji>()
            } else {
                emojiRepository.resolveEmojis(emojiIds)
                    .getOrNull()
                    .orEmpty()
                    .associateBy { it.id }
            }

            clipboardService.copy(
                CustomEmojiText.toPlainText(text) { emojiId ->
                    emojis[emojiId]?.emojis?.firstOrNull()
                }
            )
        }
    }

    fun copyContactPhone(message: Message) {
        val phone = message.contact?.firstPhone ?: return

        viewModelScope.launch {
            clipboardService.copy(phone)
        }
    }

    fun copyContactValue(value: String) {
        clipboardService.copy(value)
    }

    fun vibrate() = vibrationManager.vibrate(VibrationPattern.Error)

    fun vibrateTactile() = vibrationManager.vibrate(VibrationPattern.TactileResponse)

    fun selectMessage(message: Message) =
        _uiState.update { it.copy(selectedMessages = it.selectedMessages + message) }

    fun setVideoLooping(isLooping: Boolean) =
        viewModelScope.launch { dataStoreManager.saveVideoLooping(isLooping) }

    fun setVideoPlaybackSpeed(speed: Float) =
        viewModelScope.launch { dataStoreManager.saveVideoPlaybackSpeed(speed) }

    internal suspend fun markViewOnceViewed(chatId: Long, messageId: Long, fileId: String): Boolean {
        val key = viewOnceMediaKey(messageId, fileId)
        
        if (uiState.value.viewOnceDeadlines.containsKey(key)) return true
        
        return chatRepository.markViewOnceViewed(chatId, messageId, fileId)
            .fold(
                onSuccess = { response ->
                    if (response == null) {
                        chatRepository.deleteLocalAttachment(
                            messageId = messageId,
                            fileId = fileId,
                            messageDeleted = false
                        )
                        return@fold true
                    }

                    val deleteAt = response.deleteAt ?: return@fold true

                    _uiState.update {
                        it.copy(viewOnceDeadlines = it.viewOnceDeadlines + (key to deleteAt))
                    }
                    true
                },
                onFailure = { false }
            )
    }

    internal fun protectViewerMedia(fileIds: Collection<String>) {
        chatRepository.protectViewerFiles(fileIds)
    }

    internal fun releaseViewerMedia(fileIds: Collection<String>) {
        viewModelScope.launch { chatRepository.releaseViewerFiles(fileIds) }
    }

    fun clearMediaUrl() {
        _uiState.update { it.copy(showFullScreenViewer = false, deletedViewOnceFileIds = emptySet()) }
    }

    fun dismissBannedDialog() = _uiState.update { it.copy(showBannedDialog = false) }

    fun dismissMicrophonePermissionSheet() =
        _uiState.update { it.copy(showMicrophonePermissionSheet = false) }

    fun onEmailClicked(email: String) {
        viewModelScope.launch { _uiEffect.emit(ChatUiEffect.OpenEmail(email)) }
    }

    fun onPhoneClicked(phone: String) {
        viewModelScope.launch { _uiEffect.emit(ChatUiEffect.OpenPhone(phone)) }
    }

    fun dismissInviteBottomSheet() = _uiState.update {
        it.copy(
            showInviteBottomSheet = false,
            inviteLinkInfo = null,
            inviteLinkCode = null,
            isProcessingInvite = false
        )
    }

    internal companion object {
        const val PAGE_SIZE = 50
        const val AROUND_RADIUS = 25
        const val MAX_WINDOW_MESSAGES = 400
        const val UNREAD_VIEWPORT_FRACTION = 0.08f

        const val SEARCH_PAGE_SIZE = 50
    }
}
