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
import com.aiwazian.messenger.domain.CustomEmoji
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
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputActions
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiText
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextPart
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
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoQuality
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
    private var recordingTimerJob: Job? = null

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

    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    private val pendingMusicStartPositions = mutableMapOf<String, Int>()
    private val pendingMetadataRequests = mutableSetOf<String>()

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
        observeAudioMetadata()
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
        when (action) {
            FileAction.DOWNLOAD -> downloadFile(message, file)
            FileAction.PAUSE -> viewModelScope.launch { downloaderManager.pause(file.fileId) }
            FileAction.RESUME -> viewModelScope.launch { downloaderManager.resume(file.fileId) }
            FileAction.CANCEL -> viewModelScope.launch { downloaderManager.cancel(file.fileId) }
            FileAction.OPEN -> handleOpenFile(file)
            FileAction.PLAY -> {
                if (file.type == AttachmentType.VOICE) {
                    handlePlayVoice(file)
                } else if (file.extension.isAudioFile()) {
                    handlePlayMusic(file)
                }
            }
        }
    }

    private fun downloadFile(message: Message, file: MessageAttachment) {
        viewModelScope.launch {
            if (fileRepository.getById(file.fileId)?.path != null) return@launch

            chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
                .onSuccess { url -> downloaderManager.download(url, file.name, file.fileId) }
        }
    }

    private fun handleOpenFile(file: MessageAttachment) {
        if (file.type == AttachmentType.IMAGE ||
            file.type == AttachmentType.VIDEO ||
            file.type == AttachmentType.GIF
        ) {
            _uiState.update { it.copy(showFullScreenViewer = true) }
        } else {
            viewModelScope.launch { fileHandler.openFile(file.localUri.toString()) }
        }
    }

    private fun handlePlayVoice(file: MessageAttachment) {
        if (file.type != AttachmentType.VOICE || file.localUri == null) return

        musicPlayerManager.stop()

        if (_uiState.value.currentPlayingVoiceFileId == file.fileId) {
            voicePlayerManager.togglePlayPause()
        } else {
            val startPosition = pendingVoiceStartPositions.remove(file.fileId) ?: 0
            playVoice(file.fileId, startPosition)
        }
    }

    private fun playVoice(fileId: String, startPositionMs: Int = 0) {
        val queue = buildVoiceQueue()
        if (queue.none { it.fileId == fileId }) return
        voicePlayerManager.play(queue, fileId, startPositionMs)
    }

    private fun buildVoiceQueue(): List<VoiceQueueItem> {
        val title = context.getString(R.string.voice_message)
        return lastMessages
            .flatMap { it.attachments }
            .filter { it.type == AttachmentType.VOICE }
            .map { attachment ->
                val isReady = attachment.localUri != null &&
                        (attachment.status == DownloadStatus.COMPLETED ||
                                attachment.status == DownloadStatus.UPLOADED)
                VoiceQueueItem(
                    uri = if (isReady) attachment.localUri else null,
                    fileId = attachment.fileId,
                    title = title,
                    subtitle = context.getString(R.string.voice_message),
                    artworkUri = null
                )
            }
    }

    private fun handlePlayMusic(file: MessageAttachment) {
        if (file.localUri == null) return

        if (_uiState.value.currentMusicFileId == file.fileId) {
            musicPlayerManager.togglePlayPause()
            return
        }

        voicePlayerManager.stop()

        val startPosition = pendingMusicStartPositions.remove(file.fileId) ?: 0
        musicPlayerManager.play(buildMusicQueue(), file.fileId, startPosition)
    }

    private fun buildMusicQueue(): List<MusicTrack> {
        return lastMessages
            .flatMap { it.attachments }
            .filter { it.extension.isMusicFile() && it.localUri != null }
            .map { attachment ->
                val metadata = _uiState.value.audioMetadata[attachment.fileId]
                MusicTrack(
                    fileId = attachment.fileId,
                    uri = attachment.localUri!!,
                    title = metadata?.title ?: attachment.name,
                    artist = metadata?.artist,
                    artworkUri = null,
                    artworkData = metadata?.cover
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

    fun onMusicSeek(file: MessageAttachment, positionMs: Int) {
        if (_uiState.value.currentMusicFileId != file.fileId) {
            pendingMusicStartPositions[file.fileId] = positionMs
        } else {
            musicPlayerManager.seekTo(positionMs)
        }
    }

    fun toggleVoicePlayPause() {
        voicePlayerManager.togglePlayPause()
    }

    fun toggleMusicPlayPause() {
        musicPlayerManager.togglePlayPause()
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
        viewModelScope.launch {
            val downloaded = message.attachments.filter {
                it.localUri != null && (it.status == DownloadStatus.COMPLETED ||
                        it.status == DownloadStatus.UPLOADED)
            }
            if (downloaded.isEmpty()) return@launch

            var successCount = 0
            downloaded.forEach {
                if (fileHandler.saveToDownloads(
                        it.localUri?.path ?: it.localUri.toString(),
                        it.name
                    )
                ) successCount++
            }

            val res = if (successCount > 0) {
                R.string.saved_to_downloads
            } else {
                R.string.failed_to_save_to_downloads
            }
            _uiEffect.emit(ScheduledMessagesUiEffect.ShowSnackbar(UiText.StringResource(res)))
        }
    }

    private fun copyToClipboard(message: Message) {
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

    private fun copyContactPhone(message: Message) {
        val phone = message.contact?.firstPhone ?: return

        viewModelScope.launch {
            clipboardService.copy(phone)
        }
    }

    fun onCopyContactValue(value: String) {
        clipboardService.copy(value)
    }

    fun onLinkClicked(url: String) {
        val normalized = if (url.startsWith("http")) url else "https://$url"

        viewModelScope.launch {
            _uiEffect.emit(ScheduledMessagesUiEffect.OpenUrl(normalized))
        }
    }

    fun onEmailClicked(email: String) {
        viewModelScope.launch {
            _uiEffect.emit(ScheduledMessagesUiEffect.OpenEmail(email))
        }
    }

    fun onPhoneClicked(phone: String) {
        viewModelScope.launch {
            _uiEffect.emit(ScheduledMessagesUiEffect.OpenPhone(phone))
        }
    }

    fun onUsernameClicked(username: String) {
        viewModelScope.launch {
            searchRepository.resolveUsername(username.removePrefix("@")).onSuccess { result ->
                if (result == null) {
                    _uiEffect.emit(
                        ScheduledMessagesUiEffect.ShowSnackbar(
                            UiText.StringResource(R.string.chat_not_found)
                        )
                    )
                } else {
                    _uiEffect.emit(
                        ScheduledMessagesUiEffect.NavigateToChat(result.chatId)
                    )
                }
            }
        }
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
        if (_uiState.value.userNamesCache.containsKey(userId)) return

        viewModelScope.launch {
            try {
                userRepository.getById(userId).collect { user ->
                    val name = "${user.firstName} ${user.lastName.orEmpty()}".trim()
                    _uiState.update {
                        it.copy(userNamesCache = it.userNamesCache + (userId to name))
                    }
                    rebuildItems(lastMessages)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun observePlayback() {
        voicePlayerManager.connect()
        musicPlayerManager.connect()

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
            musicPlayerManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        currentMusicFileId = state.fileId,
                        isMusicPlaying = state.isPlaying,
                        musicPositionMs = state.positionMs,
                        musicDurationMs = state.durationMs
                    )
                }
            }
        }
    }

    private fun observeAudioMetadata() {
        viewModelScope.launch {
            _uiState.collect { state ->
                state.items.forEach { item ->
                    if (item !is ChatItem.MessageItem) return@forEach
                    item.message.attachments.forEach { attachment ->
                        val fileId = attachment.fileId
                        if (!attachment.extension.isAudioFile()) return@forEach
                        val localUri = attachment.localUri ?: return@forEach
                        if (state.audioMetadata.containsKey(fileId)) return@forEach
                        if (!pendingMetadataRequests.add(fileId)) return@forEach

                        viewModelScope.launch {
                            val metadata = AudioMetadataCache.get(
                                fileId = fileId,
                                filePath = localUri.path ?: localUri.toString(),
                                fallbackTitle = attachment.name
                            )
                            pendingMetadataRequests.remove(fileId)
                            if (metadata != null) {
                                _uiState.update {
                                    it.copy(audioMetadata = it.audioMetadata + (fileId to metadata))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeSettings() {
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

    override fun onKeyboardHeightChanged(height: Float) {
        if (height <= 0f) return

        viewModelScope.launch {
            val savedHeight = dataStoreManager.getKeyboardHeight().first()

            if (savedHeight != height) {
                dataStoreManager.saveKeyboardHeight(height)
            }
        }
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
        if (_chatState.value.isRecording) return
        val file = audioRecorderManager.startRecording()
        if (file != null) {
            musicAutoPauseController.onRecordingStarted()
            _chatState.update {
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
                    _chatState.update {
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
                _uiEffect.emit(
                    ScheduledMessagesUiEffect.ShowSnackbar(
                        UiText.DynamicString("Не удалось начать запись аудио")
                    )
                )
            }
        }
    }

    override fun lockRecording() {
        if (_chatState.value.isRecording) {
            _chatState.update { it.copy(isRecordingLocked = true) }
            vibrationManager.vibrate(VibrationPattern.TactileResponse)
        }
    }

    override fun stopRecordingAndSend() {
        if (!_chatState.value.isRecording) return
        val file = audioRecorderManager.stopRecording()
        musicAutoPauseController.onRecordingStopped()
        recordingTimerJob?.cancel()
        _chatState.update {
            it.copy(
                isRecording = false,
                isRecordingLocked = false,
                recordingDurationMs = 0L
            )
        }
        if (file != null) sendFiles(listOf(Uri.fromFile(file)))
    }

    override fun cancelRecording() {
        if (!_chatState.value.isRecording) return
        audioRecorderManager.cancelRecording()
        musicAutoPauseController.onRecordingStopped()
        recordingTimerJob?.cancel()
        _chatState.update {
            it.copy(
                isRecording = false,
                isRecordingLocked = false,
                recordingDurationMs = 0L
            )
        }
        vibrationManager.vibrate(VibrationPattern.TactileResponse)
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
