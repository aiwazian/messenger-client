/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.pinned

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.PinnedMessage
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.isAudioFile
import com.aiwazian.messenger.extensions.isMusicFile
import com.aiwazian.messenger.playback.MusicPlayerManager
import com.aiwazian.messenger.playback.MusicTrack
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.ChatItemMapper
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiText
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextPart
import com.aiwazian.messenger.utils.AudioMetadataCache
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinnedMessagesViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val fileRepository: FileRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val channelAdminsRepository: ChannelAdminsRepository,
    private val groupAdminsRepository: GroupAdminsRepository,
    private val userRepository: UserRepository,
    private val searchRepository: SearchRepository,
    private val emojiRepository: EmojiRepository,
    private val clipboardService: ClipboardService,
    private val downloaderManager: DownloaderManager,
    private val fileHandler: FileHandler,
    private val dataStoreManager: DataStoreManager,
    private val voicePlayerManager: VoicePlayerManager,
    private val musicPlayerManager: MusicPlayerManager,
    private val realtimeEventSyncService: RealtimeEventSyncService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinnedMessagesUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PinnedMessagesUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var chatId = 0L
    private var isInitialized = false
    private var lastPins: List<PinnedMessage> = emptyList()

    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    private val pendingMusicStartPositions = mutableMapOf<String, Int>()
    private val pendingMetadataRequests = mutableSetOf<String>()

    fun init(chatId: Long) {
        if (isInitialized) return
        isInitialized = true
        this.chatId = chatId

        viewModelScope.launch {
            val me = userRepository.getMe().firstOrNull() ?: return@launch

            _uiState.update { it.copy(myId = me.id, chatType = ChatType.fromId(chatId)) }

            when (ChatType.fromId(chatId)) {
                ChatType.CHANNEL -> loadChannelInfo(chatId, me.id)
                ChatType.GROUP -> loadGroupInfo(chatId, me.id)
                ChatType.PRIVATE -> loadUserInfo(chatId, me.id)
                else -> {}
            }
        }

        viewModelScope.launch {
            chatRepository.refreshPinnedMessages(chatId)
        }

        viewModelScope.launch {
            chatRepository.observePinnedMessages(chatId).collect { pins ->
                rebuildItems(pins)
            }
        }

        observePlayback()
        observeAudioMetadata()
        observeGroupReadEvents()
        observeSettings()
    }

    private fun loadChannelInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            channelRepository.fetchById(chatId)
        }

        viewModelScope.launch {
            channelAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
                _uiState.update { it.copy(myPermissions = permissions) }
            }
        }

        viewModelScope.launch {
            channelRepository.getById(chatId).collectLatest { channel ->
                _uiState.update {
                    it.copy(
                        isJoined = channel.isSubscribed,
                        isOwner = channel.ownerId == myId,
                        noCopy = channel.noCopy
                    )
                }
                rebuildItems(lastPins)
            }
        }
    }

    private fun loadGroupInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            groupRepository.fetchById(chatId)
        }

        viewModelScope.launch {
            groupAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
                _uiState.update { it.copy(myPermissions = permissions) }
            }
        }

        viewModelScope.launch {
            groupAdminsRepository.getMemberTags(chatId).onSuccess { tags ->
                _uiState.update { it.copy(memberTagsCache = tags) }
                rebuildItems(lastPins)
            }
        }

        viewModelScope.launch {
            groupRepository.getById(chatId).collectLatest { group ->
                _uiState.update {
                    it.copy(
                        isJoined = group.isMember,
                        isOwner = group.ownerId == myId,
                        noCopy = group.noCopy
                    )
                }
                rebuildItems(lastPins)
            }
        }
    }

    private fun loadUserInfo(chatId: Long, myId: Long) {
        viewModelScope.launch {
            userRepository.fetchById(chatId)
        }

        if (chatId == myId) {
            _uiState.update { it.copy(isOwner = true) }
            rebuildItems(lastPins)
            return
        }

        viewModelScope.launch {
            userRepository.getById(chatId).collectLatest { user ->
                _uiState.update { it.copy(peerNoCopy = !user.canForwardAndCopy) }
                rebuildItems(lastPins)
            }
        }
    }

    private fun rebuildItems(pins: List<PinnedMessage>) {
        lastPins = pins
        val state = _uiState.value
        val mapper = ChatItemMapper(
            context = context,
            myId = state.myId,
            chatId = chatId,
            isOwner = state.isOwner,
            isJoined = state.isJoined,
            userNamesCache = state.userNamesCache,
            memberTagsCache = state.memberTagsCache,
            groupReadInfo = state.groupReadInfo,
            copyPolicy = state.copyPolicy,
            pinnedByMeMessageIds = pins
                .filter { pin -> !pin.forEveryone }
                .map { pin -> pin.messageId }
                .toSet(),
            sharedPinnedMessageIds = pins
                .filter { pin -> pin.forEveryone }
                .map { pin -> pin.messageId }
                .toSet(),
            canPinForEveryone = state.myPermissions.canPinMessages,
            onCopyText = ::copyToClipboard,
            onEditMessage = { message -> openInChat(message, PinnedMessageOpenAction.EDIT) },
            onDeleteMessage = ::showDeleteMessageDialog,
            onRetrySendMessage = {},
            onCancelSendMessage = {},
            onReplyMessage = { message -> openInChat(message, PinnedMessageOpenAction.REPLY) },
            onForwardMessage = ::startForward,
            onPinMessage = ::onPinMessage,
            onUnpinMessage = ::onUnpinMessage,
            onLoadUserName = ::loadUserName
        )

        val forEveryoneMessages = pins
            .filter { pin -> pin.forEveryone }
            .mapNotNull { pin -> pin.message }
            .distinctBy { message -> message.id }
            .sortedBy { message -> message.sendTime }
        val forMeMessages = pins
            .filter { pin -> !pin.forEveryone }
            .mapNotNull { pin -> pin.message }
            .distinctBy { message -> message.id }
            .sortedBy { message -> message.sendTime }

        _uiState.update { current ->
            current.copy(
                forEveryoneItems = mapper.map(forEveryoneMessages).asReversed(),
                forMeItems = mapper.map(forMeMessages).asReversed(),
                pinnedCount = pins.map { pin -> pin.messageId }.distinct().size,
                isLoading = false
            )
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
                (state.forEveryoneItems + state.forMeItems).forEach { item ->
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

    private fun observeGroupReadEvents() {
        viewModelScope.launch {
            realtimeEventSyncService.groupReadEvents.collect { payload ->
                if (payload.chatId == chatId && payload.userId != _uiState.value.myId) {
                    val readerInfo = MessageReadInfo(
                        userId = payload.userId,
                        firstName = "",
                        lastName = null,
                        readAt = payload.time
                    )
                    val current = _uiState.value.groupReadInfo
                    val existing = current[payload.messageId].orEmpty()
                    if (existing.none { it.userId == payload.userId }) {
                        _uiState.update {
                            it.copy(groupReadInfo = current + (payload.messageId to (existing + readerInfo)))
                        }
                        rebuildItems(lastPins)
                    }
                    loadUserName(payload.userId)
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

    private fun openInChat(message: Message, action: PinnedMessageOpenAction) {
        viewModelScope.launch {
            _uiEffect.emit(PinnedMessagesUiEffect.OpenInChat(message = message, action = action))
        }
    }

    fun onPinMessage(message: Message) {
        if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

        if (!_uiState.value.canManagePin) {
            pinMessage(message, forEveryone = false)
            return
        }

        val pinnedForEveryone = lastPins
            .any { pin -> pin.messageId == message.id && pin.forEveryone }

        _uiState.update {
            it.copy(pinSheetMessage = message, pinForEveryone = pinnedForEveryone)
        }
    }

    fun onUnpinMessage(message: Message) {
        if (message.id <= 0) return

        unpinMessage(message, includeShared = _uiState.value.canManagePin)
    }

    fun selectPinScope(forEveryone: Boolean) {
        _uiState.update { it.copy(pinForEveryone = forEveryone) }
    }

    fun dismissPinSheet() {
        _uiState.update { it.copy(pinSheetMessage = null, pinForEveryone = false) }
    }

    fun confirmPin() {
        val state = _uiState.value
        val message = state.pinSheetMessage ?: return

        _uiState.update { it.copy(pinSheetMessage = null, pinForEveryone = false) }

        pinMessage(message, forEveryone = state.pinForEveryone)
    }

    fun confirmUnpin() {
        val state = _uiState.value
        val message = state.pinSheetMessage ?: return

        _uiState.update { it.copy(pinSheetMessage = null, pinForEveryone = false) }

        unpinMessage(message, includeShared = state.canManagePin)
    }

    private fun pinMessage(message: Message, forEveryone: Boolean) {
        viewModelScope.launch {
            chatRepository.pinMessage(chatId, message.id, forEveryone)
                .onSuccess {
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_pinned))
                    )
                }
                .onFailure {
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.pin_message_failed))
                    )
                }
        }
    }

    private fun unpinMessage(message: Message, includeShared: Boolean) {
        viewModelScope.launch {
            chatRepository.unpinMessage(chatId, message.id, includeShared)
                .onSuccess {
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_unpinned))
                    )
                }
                .onFailure {
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.unpin_message_failed))
                    )
                }
        }
    }

    fun showDeleteMessageDialog(message: Message) {
        _uiState.update { it.copy(deleteMessage = message, deleteForRecipient = false) }
    }

    fun hideDeleteMessageDialog() {
        _uiState.update { it.copy(deleteMessage = null, deleteForRecipient = false) }
    }

    fun setDeleteForRecipient(delete: Boolean) {
        _uiState.update { it.copy(deleteForRecipient = delete) }
    }

    fun confirmDeleteMessage() {
        val state = _uiState.value
        val message = state.deleteMessage ?: return

        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, message.id, state.deleteForRecipient)
            _uiState.update { it.copy(deleteMessage = null, deleteForRecipient = false) }
        }
    }

    fun startForward(message: Message) {
        if (!_uiState.value.copyPolicy.canForward()) return
        if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

        viewModelScope.launch {
            val candidates = loadShareCandidates()

            _uiState.update {
                it.copy(
                    forwardingMessage = message,
                    forwardCandidates = candidates,
                    selectedForwardChatIds = emptySet(),
                    isForwarding = false,
                    forwardHideAuthor = false,
                    forwardHideCaption = false,
                    isForwardSheetVisible = true
                )
            }
        }
    }

    private suspend fun loadShareCandidates() =
        chatRepository.getAllChats().firstOrNull().orEmpty().filter { chat ->
            when (ChatType.fromId(chat.id)) {
                ChatType.CHANNEL ->
                    channelRepository.getByIdOrNull(chat.id)
                        .firstOrNull()?.ownerId == _uiState.value.myId

                ChatType.UNKNOWN -> false
                else -> true
            }
        }

    fun toggleForwardTarget(targetChatId: Long) {
        _uiState.update { state ->
            val selected = state.selectedForwardChatIds
            state.copy(
                selectedForwardChatIds = if (targetChatId in selected) selected - targetChatId
                else selected + targetChatId
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
                isForwarding = false,
                forwardHideAuthor = false,
                forwardHideCaption = false
            )
        }
    }

    fun toggleForwardHideAuthor() {
        _uiState.update { it.copy(forwardHideAuthor = !it.forwardHideAuthor) }
    }

    fun toggleForwardHideCaption() {
        _uiState.update { state ->
            val hideCaption = !state.forwardHideCaption
            state.copy(
                forwardHideCaption = hideCaption,
                forwardHideAuthor = if (hideCaption) true else state.forwardHideAuthor
            )
        }
    }

    fun confirmForward() {
        val state = _uiState.value
        val message = state.forwardingMessage ?: return
        if (!state.copyPolicy.canForward()) return
        val targets = state.selectedForwardChatIds.toList()
        if (targets.isEmpty() || state.isForwarding) return

        _uiState.update { it.copy(isForwarding = true) }

        viewModelScope.launch {
            chatRepository.forwardMessage(
                sourceChatId = chatId,
                messageId = message.id,
                targetChatIds = targets,
                hideAuthor = state.forwardHideAuthor,
                hideCaption = state.forwardHideCaption
            )
                .onSuccess {
                    dismissForwardSheet()
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_forwarded))
                    )
                }
                .onFailure {
                    _uiState.update { it.copy(isForwarding = false) }
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.forward_failed))
                    )
                }
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
        if (file.type == AttachmentType.IMAGE || file.type == AttachmentType.VIDEO || file.type == AttachmentType.GIF) {
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
        return pinnedMessages()
            .flatMap { it.attachments }
            .filter { it.type == AttachmentType.VOICE }
            .map { attachment ->
                val isReady = attachment.localUri != null &&
                        (attachment.status == DownloadStatus.COMPLETED || attachment.status == DownloadStatus.UPLOADED)
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
        return pinnedMessages()
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

    private fun pinnedMessages(): List<Message> =
        lastPins.mapNotNull { pin -> pin.message }.distinctBy { it.id }.sortedBy { it.sendTime }

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
        if (!_uiState.value.copyPolicy.canSaveMedia) return

        viewModelScope.launch {
            val downloaded = message.attachments.filter {
                it.localUri != null && (it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.UPLOADED)
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

            val res =
                if (successCount > 0) R.string.saved_to_downloads else R.string.failed_to_save_to_downloads
            _uiEffect.emit(PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(res)))
        }
    }

    fun copyToClipboard(message: Message) {
        if (!_uiState.value.copyPolicy.canCopyText()) return

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

    fun onLinkClicked(url: String) {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        viewModelScope.launch { _uiEffect.emit(PinnedMessagesUiEffect.OpenUrl(normalized)) }
    }

    fun onEmailClicked(email: String) {
        viewModelScope.launch { _uiEffect.emit(PinnedMessagesUiEffect.OpenEmail(email)) }
    }

    fun onUsernameClicked(username: String) {
        viewModelScope.launch {
            searchRepository.resolveUsername(username.removePrefix("@")).onSuccess { result ->
                if (result == null) {
                    _uiEffect.emit(
                        PinnedMessagesUiEffect.ShowSnackbar(UiText.StringResource(R.string.chat_not_found))
                    )
                } else {
                    _uiEffect.emit(PinnedMessagesUiEffect.NavigateToChat(result.chatId))
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
                PinnedMessagesUiEffect.NavigateToChat(
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
            _uiEffect.emit(PinnedMessagesUiEffect.NavigateToChat(chatId = forwardedFrom.chatId))
        }
    }

    fun loadUserName(userId: Long) {
        if (_uiState.value.userNamesCache.containsKey(userId)) return

        viewModelScope.launch {
            try {
                userRepository.getById(userId).collect { user ->
                    val name = "${user.firstName} ${user.lastName.orEmpty()}".trim()
                    _uiState.update {
                        it.copy(userNamesCache = it.userNamesCache + (userId to name))
                    }
                    rebuildItems(lastPins)
                }
            } catch (e: Exception) {
                Log.e("PinnedMessagesViewModel", "Error loading user name", e)
            }
        }
    }
}
