/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.scheduled

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.ViewOnceOption
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.isAudioFile
import com.aiwazian.messenger.extensions.isMusicFile
import com.aiwazian.messenger.playback.MusicAutoPauseController
import com.aiwazian.messenger.playback.MusicPlayerManager
import com.aiwazian.messenger.playback.MusicTrack
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.screens.chat.ChatCopyPolicy
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.ChatItemMapper
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import com.aiwazian.messenger.ui.screens.chat.MediaPickerUiState
import com.aiwazian.messenger.ui.screens.chat.common.MediaSettingsController
import com.aiwazian.messenger.ui.screens.chat.common.MessageInteractionsController
import com.aiwazian.messenger.ui.screens.chat.common.PlaybackCoordinator
import com.aiwazian.messenger.ui.screens.chat.common.PlaybackHost
import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.ui.screens.chat.common.UserNameCache
import com.aiwazian.messenger.ui.screens.chat.common.VoiceRecordingController
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputActions
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendStickerUseCase
import com.aiwazian.messenger.utils.AudioMetadataCache
import com.aiwazian.messenger.utils.AudioRecorderManager
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.MessageSendQueue
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduledMessagesViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val searchRepository: SearchRepository,
    private val fileRepository: FileRepository,
    private val clipboardService: ClipboardService,
    private val emojiRepository: EmojiRepository,
    private val downloaderManager: DownloaderManager,
    private val fileHandler: FileHandler,
    private val dataStoreManager: DataStoreManager,
    private val voicePlayerManager: VoicePlayerManager,
    private val musicPlayerManager: MusicPlayerManager,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendStickerUseCase: SendStickerUseCase,
    private val messageSendQueue: MessageSendQueue,
    private val musicAutoPauseController: MusicAutoPauseController,
    private val vibrationManager: VibrationManager
) : ViewModel(), ChatInputActions {

    private val audioRecorderManager = AudioRecorderManager(context)

    private val _uiState = MutableStateFlow(ScheduledMessagesUiState())
    val uiState = _uiState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatUiState = _chatState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ScheduledMessagesUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var chatId = 0L
    private var isInitialized = false
    private var lastMessages: List<Message> = emptyList()

    private var pendingMedia: PendingScheduledMedia? = null
    private var pendingStickerId: Long? = null

    private val playbackCoordinator = PlaybackCoordinator(
        context = context,
        scope = viewModelScope,
        voicePlayerManager = voicePlayerManager,
        musicPlayerManager = musicPlayerManager,
        hostProvider = {
            val state = _uiState.value
            object : PlaybackHost {
                override val attachments = lastMessages.flatMap { it.attachments }

                override val audioMetadata = state.audioMetadata

                override val voiceTitle = context.getString(R.string.voice_message)

                override val artworkUri: android.net.Uri? = null

                override val currentPlayingVoiceFileId = state.currentPlayingVoiceFileId

                override val currentMusicFileId = state.currentMusicFileId

                override fun onVoicePlayerState(
                    currentFileId: String?, isPlaying: Boolean, positionMs: Int, durationMs: Int
                ) {
                    _uiState.update {
                        it.copy(
                            currentPlayingVoiceFileId = currentFileId,
                            isVoicePlaying = isPlaying,
                            voicePositionMs = positionMs,
                            voiceDurationMs = durationMs
                        )
                    }
                }

                override fun onMusicPlayerState(
                    fileId: String?, title: String, artist: String?, isPlaying: Boolean,
                    positionMs: Int, durationMs: Int
                ) {
                    _uiState.update {
                        it.copy(
                            currentMusicFileId = fileId,
                            isMusicPlaying = isPlaying,
                            musicPositionMs = positionMs,
                            musicDurationMs = durationMs
                        )
                    }
                }

                override fun onAudioMetadata(fileId: String, metadata: AudioTrackMetadata) {
                    _uiState.update {
                        it.copy(audioMetadata = it.audioMetadata + (fileId to metadata))
                    }
                }
            }
        }
    )

    private val interactions = MessageInteractionsController(
        scope = viewModelScope,
        fileRepository = fileRepository,
        chatRepository = chatRepository,
        downloaderManager = downloaderManager,
        fileHandler = fileHandler,
        clipboardService = clipboardService,
        emojiRepository = emojiRepository,
        searchRepository = searchRepository,
        onSnackbar = { text ->
            viewModelScope.launch { _uiEffect.emit(ScheduledMessagesUiEffect.ShowSnackbar(text)) }
        },
        onOpenUrl = { url ->
            viewModelScope.launch { _uiEffect.emit(ScheduledMessagesUiEffect.OpenUrl(url)) }
        },
        onOpenEmail = { email ->
            viewModelScope.launch { _uiEffect.emit(ScheduledMessagesUiEffect.OpenEmail(email)) }
        },
        onOpenPhone = { phone ->
            viewModelScope.launch { _uiEffect.emit(ScheduledMessagesUiEffect.OpenPhone(phone)) }
        },
        onNavigateToChat = { targetChatId, scrollToMessageId ->
            viewModelScope.launch {
                _uiEffect.emit(
                    ScheduledMessagesUiEffect.NavigateToChat(
                        chatId = targetChatId,
                        scrollToMessageId = scrollToMessageId
                    )
                )
            }
        },
        onOpenMediaViewer = {
            _uiState.update { it.copy(showFullScreenViewer = true) }
        },
        onPlayVoice = { file -> playbackCoordinator.handlePlayVoice(file) },
        onPlayMusic = { file -> playbackCoordinator.handlePlayMusic(file) }
    )

    private val voiceRecording = VoiceRecordingController(
        scope = viewModelScope,
        recorder = audioRecorderManager,
        vibration = vibrationManager,
        musicAutoPauseController = musicAutoPauseController,
        isRecording = { _chatState.value.isRecording },
        updateRecordingState = { isRecording, isLocked, durationMs, amplitude ->
            _chatState.update {
                it.copy(
                    isRecording = isRecording,
                    isRecordingLocked = isLocked,
                    recordingDurationMs = durationMs,
                    recordingAmplitude = amplitude
                )
            }
        },
        onRecordStartFailed = {
            viewModelScope.launch {
                _uiEffect.emit(
                    ScheduledMessagesUiEffect.ShowSnackbar(
                        UiText.StringResource(R.string.voice_recording_start_failed)
                    )
                )
            }
        },
        onRecordedFile = { file -> sendFiles(listOf(file)) }
    )

    private val userNameCache = UserNameCache(
        scope = viewModelScope,
        userRepository = userRepository
    )

    private val mediaSettings = MediaSettingsController(
        scope = viewModelScope,
        dataStoreManager = dataStoreManager
    )

    private data class PendingScheduledMedia(
        val uris: List<Uri>,
        val text: String?,
        val videoQualities: Map<Uri, VideoQuality>,
        val mediaTransforms: Map<Uri, MediaTransform>,
        val mediaDrawings: Map<Uri, Bitmap>,
        val viewOnceOptions: Map<Uri, ViewOnceOption>,
        val spoilerUris: Set<Uri>
    )

    fun init(chatId: Long) {
        if (isInitialized) return
        isInitialized = true
        this.chatId = chatId

        _uiState.update { it.copy(chatId = chatId, chatType = ChatType.fromId(chatId)) }

        viewModelScope.launch {
            val me = userRepository.getMe().firstOrNull() ?: return@launch

            _uiState.update { it.copy(myId = me.id) }
            _chatState.update {
                it.copy(
                    chatId = chatId,
                    myId = me.id,
                    isOwner = true,
                    isJoined = true
                )
            }
            rebuildItems(lastMessages)
        }

        viewModelScope.launch {
            chatRepository.observeScheduledMessages(chatId).collect { messages ->
                rebuildItems(messages)
            }
        }

        viewModelScope.launch {
            chatRepository.syncScheduledMessages(chatId)
        }

        observePlayback()
        observeSettings()
    }

    private fun rebuildItems(messages: List<Message>) {
        lastMessages = messages
        val state = _uiState.value
        val mapper = ChatItemMapper(
            context = context,
            myId = state.myId,
            chatId = chatId,
            isOwner = true,
            isJoined = true,
            userNamesCache = state.userNamesCache,
            memberTagsCache = state.memberTagsCache,
            groupReadInfo = emptyMap(),
            copyPolicy = ChatCopyPolicy.Unrestricted,
            onCopyText = ::copyToClipboard,
            onCopyContactPhone = ::copyContactPhone,
            onCallContact = { phone ->
                viewModelScope.launch {
                    _uiEffect.emit(ScheduledMessagesUiEffect.OpenPhone(phone))
                }
            },
            onEditMessage = {},
            onDeleteMessage = {},
            onRetrySendMessage = {},
            onCancelSendMessage = {},
            onReplyMessage = {},
            onForwardMessage = {},
            onPinMessage = {},
            onUnpinMessage = {},
            onLoadUserName = ::loadUserName
        )

        val items = mapper.map(messages)
            .map { item ->
                if (item is ChatItem.MessageItem) {
                    item.copy(dropdownActions = dropdownActions(item.message))
                } else {
                    item
                }
            }
            .asReversed()

        _uiState.update { it.copy(items = items, isLoading = false) }
    }

    private fun dropdownActions(message: Message): List<DropdownMenuAction> = buildList {
        if (message.id > 0) {
            add(
                DropdownMenuAction(
                    Icons.AutoMirrored.Rounded.Send,
                    UiText.StringResource(R.string.send_now),
                    onClick = { sendNow(message) }
                )
            )
        }

        if (!message.text.isNullOrBlank()) {
            add(
                DropdownMenuAction(
                    Icons.Rounded.ContentCopy,
                    UiText.StringResource(R.string.copy),
                    onClick = { copyToClipboard(message) }
                )
            )
        }

        if (canEditMessage(message)) {
            add(
                DropdownMenuAction(
                    Icons.Rounded.Edit,
                    UiText.StringResource(R.string.edit),
                    onClick = { startEditing(message) }
                )
            )
        }

        if (message.id > 0) {
            add(
                DropdownMenuAction(
                    Icons.Outlined.EditCalendar,
                    UiText.StringResource(R.string.edit_schedule_time),
                    onClick = { showRescheduleSheet(message) }
                )
            )
        }

        add(
            DropdownMenuAction(
                Icons.Rounded.DeleteOutline,
                UiText.StringResource(R.string.delete),
                onClick = { showDeleteDialog(message) },
                isDestructive = true
            )
        )
    }

    private fun canEditMessage(message: Message): Boolean {
        return message.forwardedFrom == null &&
                !message.text.isNullOrBlank() &&
                message.messageType == MessageType.TEXT
    }

    private fun sendNow(message: Message) {
        if (message.id <= 0) return

        viewModelScope.launch {
            chatRepository.sendScheduledMessageNow(chatId, message.id)
        }
    }

    private fun showRescheduleSheet(message: Message) {
        _uiState.update { it.copy(rescheduleMessage = message) }
    }

    fun dismissRescheduleSheet() {
        _uiState.update { it.copy(rescheduleMessage = null) }
    }

    fun confirmReschedule(scheduledAt: Long) {
        val message = _uiState.value.rescheduleMessage ?: return

        _uiState.update { it.copy(rescheduleMessage = null) }

        viewModelScope.launch {
            chatRepository.updateScheduledMessageTime(chatId, message.id, scheduledAt)
        }
    }

    private fun showDeleteDialog(message: Message) {
        _uiState.update { it.copy(deleteMessage = message) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteMessage = null) }
    }

    fun confirmDeleteMessage() {
        val message = _uiState.value.deleteMessage ?: return

        _uiState.update { it.copy(deleteMessage = null) }

        viewModelScope.launch {
            if (message.id <= 0) {
                chatRepository.deleteLocalMessage(message.id)
            } else {
                chatRepository.deleteScheduledMessage(chatId, message.id)
            }
        }
    }

    fun startEditing(message: Message) {
        if (message.forwardedFrom != null) return
        if (message.text.isNullOrBlank()) return

        _chatState.update {
            it.copy(
                editingMessageId = message.id,
                editingOriginalText = message.text,
                messageText = message.text
            )
        }
    }

    private suspend fun confirmEditMessage(editingId: Long) {
        val newText = _chatState.value.messageText.trim()
        val originalText = _chatState.value.editingOriginalText

        if (newText.isEmpty() || newText == originalText) {
            cancelEditing()
            return
        }

        cancelEditing()

        chatRepository.editMessage(chatId, editingId, newText)
            .onSuccess { editedMessage ->
                chatRepository.updateLocalMessage(
                    editingId,
                    editedMessage.text,
                    editedMessage.editedAt
                )
            }
    }

    private fun openScheduleSheet() {
        _uiState.update { it.copy(showScheduleSendSheet = true) }
    }

    fun dismissScheduleSendSheet() {
        pendingMedia = null
        pendingStickerId = null
        _uiState.update { it.copy(showScheduleSendSheet = false) }
    }

    fun onMediaSheetSendRequested(picker: MediaPickerUiState) {
        if (picker.selected.isEmpty()) return

        pendingMedia = PendingScheduledMedia(
            uris = picker.selected,
            text = _chatState.value.messageText.trim().ifBlank { null },
            videoQualities = picker.videoQualities,
            mediaTransforms = picker.mediaTransforms,
            mediaDrawings = picker.mediaDrawings.mapValues { it.value.bitmap },
            viewOnceOptions = picker.viewOnceOptions,
            spoilerUris = picker.spoilerUris
        )

        openScheduleSheet()
    }

    private fun confirmSchedule(scheduledAt: Long) {
        _uiState.update { it.copy(showScheduleSendSheet = false) }

        val stickerId = pendingStickerId
        if (stickerId != null) {
            pendingStickerId = null

            viewModelScope.launch {
                sendStickerUseCase(chatId, stickerId, scheduledAt = scheduledAt)
            }
            return
        }

        val media = pendingMedia
        if (media != null) {
            pendingMedia = null

            messageSendQueue.enqueueFiles(
                chatId = chatId,
                uris = media.uris,
                text = media.text,
                videoQualities = media.videoQualities,
                mediaTransforms = media.mediaTransforms,
                mediaDrawings = media.mediaDrawings,
                viewOnceOptions = media.viewOnceOptions,
                spoilerUris = media.spoilerUris,
                silent = false,
                scheduledAt = scheduledAt
            )
            return
        }

        val text = _chatState.value.messageText.trim()
        if (text.isEmpty()) return

        _chatState.update { it.copy(messageText = "") }

        viewModelScope.launch {
            sendMessageUseCase(chatId, message = text, scheduledAt = scheduledAt)
        }
    }

    fun onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
        interactions.onFileAction(message, file, action)
    }

    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) {
        playbackCoordinator.onVoiceSeek(file, positionMs)
    }

    fun onMusicSeek(file: MessageAttachment, positionMs: Int) {
        playbackCoordinator.onMusicSeek(file, positionMs)
    }

    fun toggleVoicePlayPause() {
        playbackCoordinator.toggleVoicePlayPause()
    }

    fun toggleMusicPlayPause() {
        playbackCoordinator.toggleMusicPlayPause()
    }

    fun onViewerDismiss() {
        _uiState.update { it.copy(showFullScreenViewer = false) }
    }

    fun onVideoLoopingChange(isLooping: Boolean) {
        viewModelScope.launch { dataStoreManager.saveVideoLooping(isLooping) }
    }

    fun onVideoPlaybackSpeedChange(speed: Float) {
        viewModelScope.launch { dataStoreManager.saveVideoPlaybackSpeed(speed) }
    }

    fun onSaveToGallery(uri: Uri) {
        fileHandler.saveToGallery(uri.toString())
    }

    fun saveAttachmentsToDownloads(message: Message) {
        interactions.saveAttachmentsToDownloads(message)
    }

    private fun copyToClipboard(message: Message) {
        interactions.copyText(message)
    }

    private fun copyContactPhone(message: Message) {
        interactions.copyContactPhone(message)
    }

    fun onCopyContactValue(value: String) {
        interactions.copyValue(value)
    }

    fun onLinkClicked(url: String) {
        interactions.openLink(url)
    }

    fun onEmailClicked(email: String) {
        interactions.openEmail(email)
    }

    fun onPhoneClicked(phone: String) {
        interactions.openPhone(phone)
    }

    fun onUsernameClicked(username: String) {
        interactions.openUsername(username)
    }

    fun onReplyPreviewClicked(message: Message) {
        val preview = message.replyTo ?: return
        val previewChatId = preview.chatId
        val resolvesHere = previewChatId == null ||
                previewChatId == chatId ||
                (_uiState.value.chatType == ChatType.PRIVATE && previewChatId == _uiState.value.myId)
        val destination = if (resolvesHere) chatId else previewChatId

        viewModelScope.launch {
            _uiEffect.emit(
                ScheduledMessagesUiEffect.NavigateToChat(
                    chatId = destination,
                    scrollToMessageId = preview.messageId
                )
            )
        }
    }

    fun onForwardedFromClicked(message: Message) {
        val forwardedFrom = message.forwardedFrom ?: return
        if (forwardedFrom.access != ForwardSourceAccess.OPEN) return
        if (forwardedFrom.chatId == chatId) return

        viewModelScope.launch {
            _uiEffect.emit(
                ScheduledMessagesUiEffect.NavigateToChat(chatId = forwardedFrom.chatId)
            )
        }
    }

    private fun loadUserName(userId: Long) {
        userNameCache.load(userId) { id, name ->
            _uiState.update {
                it.copy(userNamesCache = it.userNamesCache + (id to name))
            }
            rebuildItems(lastMessages)
        }
    }

    private fun observePlayback() {
        playbackCoordinator.connect()
        playbackCoordinator.observeAudioMetadata(
            _uiState.map { state ->
                state.items.filterIsInstance<ChatItem.MessageItem>()
                    .flatMap { it.message.attachments }
            }
        )
    }

    private fun observeSettings() {
        mediaSettings.observeVideoLooping { isLooping ->
            _uiState.update { it.copy(isVideoLooping = isLooping) }
        }

        mediaSettings.observeVideoPlaybackSpeed { speed ->
            _uiState.update { it.copy(videoPlaybackSpeed = speed) }
        }
    }

    override fun onKeyboardHeightChanged(height: Float) {
        if (height <= 0f) return

        mediaSettings.persistKeyboardHeight(height)
    }

    override fun changeText(text: String) {
        _chatState.update { it.copy(messageText = text) }
    }

    override fun onSendMessageClicked() {
        val editingId = _chatState.value.editingMessageId

        if (editingId != null) {
            viewModelScope.launch { confirmEditMessage(editingId) }
            return
        }

        if (_chatState.value.messageText.trim().isEmpty()) return

        openScheduleSheet()
    }

    override fun onSendWithoutSoundClicked() {
        onSendMessageClicked()
    }

    override fun onScheduleSendOptionsClicked() {
        if (_chatState.value.editingMessageId != null) return

        openScheduleSheet()
    }

    override fun onSendScheduledClicked(scheduledAt: Long) {
        confirmSchedule(scheduledAt)
    }

    override fun onScheduledMessagesClicked() {}

    override fun onJoinClicked() {}

    override fun sendFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return

        pendingMedia = PendingScheduledMedia(
            uris = uris,
            text = _chatState.value.messageText.trim().ifBlank { null },
            videoQualities = emptyMap(),
            mediaTransforms = emptyMap(),
            mediaDrawings = emptyMap(),
            viewOnceOptions = emptyMap(),
            spoilerUris = emptySet()
        )

        openScheduleSheet()
    }

    override fun sendMediaUris(uris: List<Uri>, caption: String, replyTo: MessageReplyPreview?) {
        if (uris.isEmpty()) return

        pendingMedia = PendingScheduledMedia(
            uris = uris,
            text = caption.trim().ifBlank { null },
            videoQualities = emptyMap(),
            mediaTransforms = emptyMap(),
            mediaDrawings = emptyMap(),
            viewOnceOptions = emptyMap(),
            spoilerUris = emptySet()
        )

        openScheduleSheet()
    }

    override fun sendSticker(stickerId: Long) {
        pendingStickerId = stickerId
        openScheduleSheet()
    }

    override fun startRecording() {
        voiceRecording.start()
    }

    override fun lockRecording() {
        voiceRecording.lock()
    }

    override fun stopRecordingAndSend() {
        voiceRecording.stopAndDeliver()
    }

    override fun cancelRecording() {
        voiceRecording.cancel()
    }

    override fun onMicrophonePermissionDenied() {
        _chatState.update { it.copy(showMicrophonePermissionSheet = true) }
    }

    fun dismissMicrophonePermissionSheet() {
        _chatState.update { it.copy(showMicrophonePermissionSheet = false) }
    }

    override fun cancelReply() {}

    override fun onReplyPanelClicked() {}

    override fun cancelEditing() {
        _chatState.update {
            it.copy(
                editingMessageId = null,
                editingOriginalText = null,
                messageText = ""
            )
        }
    }

    override fun showBlockDialog() {}
}
