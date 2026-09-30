/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.comments

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Comment
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.CommentAuthorRole
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.getMediaDimensions
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.socket.WebSocketEvent
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputActions
import com.aiwazian.messenger.usecase.JoinChannelUseCase
import com.aiwazian.messenger.utils.AttachmentOutbox
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.UploadManager
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoQuality
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
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PostCommentsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val channelRepository: ChannelRepository,
    private val channelAdminsRepository: ChannelAdminsRepository,
    private val chatRepository: ChatRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val webSocketClient: WebSocketClient,
    private val attachmentOutbox: AttachmentOutbox,
    private val uploadManager: UploadManager,
    private val downloaderManager: DownloaderManager,
    private val fileHandler: FileHandler,
    private val dataStoreManager: DataStoreManager
) : ViewModel(), ChatInputActions {

    private val _uiState = MutableStateFlow(PostCommentsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PostCommentsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var comments = listOf<Comment>()

    fun init(chatId: Long, postId: Long) {
        if (_uiState.value.chatId == chatId && _uiState.value.postId == postId) return

        viewModelScope.launch {
            val me = userRepository.getMe().firstOrNull() ?: return@launch

            _uiState.update {
                it.copy(chatId = chatId, postId = postId, myId = me.id)
            }

            observeChannel(chatId, me.id)
            loadMyPermissions(chatId)
            observeRealtime(chatId, postId)
            loadComments(chatId, postId)
        }

        viewModelScope.launch {
            dataStoreManager.getKeyboardHeight().collect { height ->
                _uiState.update { it.copy(keyboardHeight = height) }
            }
        }

        viewModelScope.launch {
            downloaderManager.activeDownloads.collect {
                rebuildCommentItems()
            }
        }
    }

    fun sendCommentFiles(
        uris: List<Uri>,
        videoQualities: Map<Uri, VideoQuality>,
        mediaTransforms: Map<Uri, MediaTransform>,
        mediaDrawings: Map<Uri, android.graphics.Bitmap>
    ) {
        viewModelScope.launch {
            sendFilesInternal(uris, _uiState.value.commentText, videoQualities, mediaTransforms, mediaDrawings)
        }
    }

    private suspend fun sendFilesInternal(
        uris: List<Uri>,
        caption: String,
        videoQualities: Map<Uri, VideoQuality>,
        mediaTransforms: Map<Uri, MediaTransform>,
        mediaDrawings: Map<Uri, android.graphics.Bitmap>
    ) {
        if (uris.isEmpty() || _uiState.value.isSending) return

        val state = _uiState.value

        _uiState.update { it.copy(isSending = true) }

        try {
            val stamp = System.currentTimeMillis()

            val sourceUris = uris.mapIndexed { index, uri ->
                attachmentOutbox.keep(
                    uri = uri,
                    key = "comment_${state.postId}_${stamp}_$index",
                    videoQuality = videoQualities[uri] ?: MediaCompressionConfig.VIDEO_DEFAULT_QUALITY,
                    transform = mediaTransforms[uri] ?: MediaTransform.None,
                    overlay = mediaDrawings[uri]
                )
            }

            val uploaded = mutableListOf<AttachmentInputDto>()

            for ((index, sourceUri) in sourceUris.withIndex()) {
                val fileName = fileNameOf(sourceUri)
                val mimeType = sourceUri.getFileType(context)
                val attachmentType = attachmentTypeOf(mimeType)
                val frame = sourceUri.getMediaDimensions(context, mimeType)

                val init = channelRepository.initCommentFileUpload(
                    state.chatId,
                    state.postId,
                    FileInitRequestDto(
                        name = fileName,
                        size = sizeOf(sourceUri),
                        mimeType = mimeType,
                        category = attachmentType,
                        width = frame?.width,
                        height = frame?.height
                    )
                )

                if (init == null) {
                    failFilesSend(sourceUris)
                    return
                }

                val uploadResult = uploadManager.upload(
                    fileUri = sourceUri,
                    upload = init,
                    fileId = init.fileId,
                    maxAttempts = 3,
                    keepLocalCopy = false
                )

                if (uploadResult.isFailure) {
                    failFilesSend(sourceUris)
                    return
                }

                uploaded.add(AttachmentInputDto(fileId = init.fileId, type = attachmentType))
            }

            channelRepository.confirmCommentFiles(
                state.chatId,
                state.postId,
                uploaded,
                caption.trim().ifBlank { null }
            ).onSuccess { comment ->
                sourceUris.forEachIndexed { index, sourceUri ->
                    uploadManager.adoptLocalCopy(
                        fileUri = sourceUri,
                        fileId = uploaded[index].fileId,
                        status = DownloadStatus.UPLOADED
                    )
                    attachmentOutbox.release(sourceUri)
                }

                appendComment(comment)
                chatRepository.incrementCommentCount(state.postId)
                _uiState.update { it.copy(commentText = "", isSending = false) }
            }.onFailure {
                sourceUris.forEach { attachmentOutbox.release(it) }
                notifySendFailed()
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSending = false) }
            notifySendFailed()
        }
    }

    private suspend fun failFilesSend(sourceUris: List<Uri>) {
        sourceUris.forEach { attachmentOutbox.release(it) }
        _uiState.update { it.copy(isSending = false) }
        notifySendFailed()
    }

    private suspend fun notifySendFailed() {
        _uiEffect.emit(
            PostCommentsUiEffect.ShowSnackbar(
                UiText.StringResource(R.string.failed_to_send_comment)
            )
        )
    }

    private fun fileNameOf(uri: Uri): String {
        val name = uri.getFileName(context) ?: DEFAULT_FILE_NAME

        if (name.contains('.')) {
            return name
        }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(uri.getFileType(context))

        return if (extension != null) "$name.$extension" else name
    }

    private fun sizeOf(uri: Uri): Long {
        if (uri.scheme == SCHEME_FILE) {
            return uri.path?.let { File(it).length() } ?: 0
        }

        return uri.getFileSize(context) ?: 0
    }

    private fun attachmentTypeOf(mimeType: String): AttachmentType = when {
        mimeType.startsWith("image/") -> AttachmentType.IMAGE
        mimeType.startsWith("video/") -> AttachmentType.VIDEO
        else -> AttachmentType.FILE
    }

    fun join() {
        viewModelScope.launch {
            val chatId = _uiState.value.chatId

            joinChannelUseCase(chatId).onSuccess {
                channelRepository.fetchById(chatId)
                _uiState.update { it.copy(isSubscribed = true) }
            }
        }
    }

    fun onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
        when (action) {
            FileAction.DOWNLOAD -> viewModelScope.launch {
                downloadAttachment(message.id, file)
            }

            FileAction.PAUSE -> viewModelScope.launch { downloaderManager.pause(file.fileId) }
            FileAction.RESUME -> viewModelScope.launch { downloaderManager.resume(file.fileId) }
            FileAction.CANCEL -> viewModelScope.launch { downloaderManager.cancel(file.fileId) }
            FileAction.OPEN -> handleOpenFile(file)
            FileAction.PLAY -> {}
        }
    }

    private suspend fun downloadAttachment(commentId: Long, file: MessageAttachment) {
        if (fileRepository.getById(file.fileId)?.path != null) {
            rebuildCommentItems()
            return
        }

        channelRepository.getCommentFileDownloadUrl(
            _uiState.value.chatId,
            _uiState.value.postId,
            commentId,
            file.fileId
        ).onSuccess { url ->
            downloaderManager.download(url, file.name, file.fileId)
        }
    }

    private fun handleOpenFile(file: MessageAttachment) {
        viewModelScope.launch {
            val path = file.localUri?.path ?: fileRepository.getById(file.fileId)?.path

            if (path == null) {
                downloadAttachment(file.messageId, file)
                return@launch
            }

            if (file.type == AttachmentType.IMAGE ||
                file.type == AttachmentType.VIDEO ||
                file.type == AttachmentType.GIF
            ) {
                _uiEffect.emit(PostCommentsUiEffect.ShowMediaViewer(file.fileId))
            } else {
                fileHandler.openFile(path)
            }
        }
    }

    override fun onKeyboardHeightChanged(height: Float) {
        if (height <= 0f) return

        viewModelScope.launch {
            val savedHeight = dataStoreManager.getKeyboardHeight().firstOrNull()

            if (savedHeight != height) {
                dataStoreManager.saveKeyboardHeight(height)
            }
        }
    }

    override fun changeText(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    override fun onSendMessageClicked() {
        val state = _uiState.value
        val text = state.commentText.trim()

        if (text.isEmpty() || state.isSending) return

        _uiState.update { it.copy(isSending = true) }

        viewModelScope.launch {
            channelRepository.sendComment(state.chatId, state.postId, text).onSuccess { comment ->
                appendComment(comment)
                chatRepository.incrementCommentCount(state.postId)
                _uiState.update { it.copy(commentText = "", isSending = false) }
            }.onFailure {
                _uiState.update { it.copy(isSending = false) }
                notifySendFailed()
            }
        }
    }

    override fun onJoinClicked() {}

    override fun sendFiles(uris: List<Uri>) {
        viewModelScope.launch {
            sendFilesInternal(uris, "", emptyMap(), emptyMap(), emptyMap())
        }
    }

    override fun sendMediaUris(uris: List<Uri>, caption: String, replyTo: MessageReplyPreview?) {
        viewModelScope.launch {
            sendFilesInternal(uris, caption, emptyMap(), emptyMap(), emptyMap())
        }
    }

    override fun sendSticker(stickerId: Long) {
        val state = _uiState.value
        if (state.isSending) return

        viewModelScope.launch {
            channelRepository.sendComment(state.chatId, state.postId, null, stickerId)
                .onSuccess { comment ->
                    appendComment(comment)
                    chatRepository.incrementCommentCount(state.postId)
                }.onFailure {
                    notifySendFailed()
                }
        }
    }

    override fun startRecording() {}

    override fun onMicrophonePermissionDenied() {}

    override fun lockRecording() {}

    override fun cancelRecording() {}

    override fun stopRecordingAndSend() {}

    override fun cancelReply() {}

    override fun onReplyPanelClicked() {}

    override fun cancelEditing() {}

    override fun showBlockDialog() {}

    private suspend fun loadComments(chatId: Long, postId: Long) {
        channelRepository.getComments(chatId, postId).onSuccess { loaded ->
            comments = loaded.map { resolveAttachments(it) }
            rebuildCommentItems()
            _uiState.update { it.copy(isLoading = false) }
        }.onFailure {
            _uiState.update { it.copy(isLoading = false) }
            _uiEffect.emit(
                PostCommentsUiEffect.ShowSnackbar(
                    UiText.StringResource(R.string.failed_to_load_comments)
                )
            )
        }
    }

    private fun observeChannel(chatId: Long, myId: Long) {
        viewModelScope.launch {
            channelRepository.getById(chatId).collectLatest { channel ->
                _uiState.update {
                    it.copy(
                        isSubscribed = channel.isSubscribed,
                        isOwner = channel.ownerId == myId,
                        commentsEnabled = channel.commentsEnabled,
                        commentsRestrictedToSubscribers = channel.commentsRestrictedToSubscribers
                    )
                }
            }
        }
    }

    private fun loadMyPermissions(chatId: Long) {
        viewModelScope.launch {
            channelAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
                _uiState.update {
                    it.copy(isAdmin = permissions.isOwner || permissions.isAdmin)
                }
            }
        }
    }

    private fun observeRealtime(chatId: Long, postId: Long) {
        webSocketClient.subscribeToEvent(WebSocketEvent.NewComment) { payload ->
            if (payload.chatId != chatId || payload.postId != postId) return@subscribeToEvent

            viewModelScope.launch {
                if (comments.none { it.id == payload.comment.id }) {
                    appendComment(payload.comment)
                }
            }
        }

        webSocketClient.subscribeToEvent(WebSocketEvent.CommentsCleared) { payload ->
            if (payload.chatId != chatId) return@subscribeToEvent

            viewModelScope.launch {
                comments = emptyList()
                rebuildCommentItems()
            }
        }
    }

    private suspend fun appendComment(comment: Comment) {
        if (comments.any { it.id == comment.id }) return

        comments = comments + resolveAttachments(comment)
        rebuildCommentItems()
    }

    private suspend fun resolveAttachments(comment: Comment): Comment {
        if (comment.attachments.isEmpty()) return comment

        val attachments = comment.attachments.map { attachment ->
            val localUri = attachment.localUri ?: fileRepository.getById(attachment.fileId)
                ?.path
                ?.let { path -> Uri.fromFile(File(path)) }

            if (localUri != null && localUri != attachment.localUri) {
                attachment.copy(localUri = localUri)
            } else {
                attachment
            }
        }

        return comment.copy(attachments = attachments)
    }

    private fun rebuildCommentItems() {
        val state = _uiState.value
        val items = mutableListOf<ChatItem>()
        var lastDate: LocalDate? = null

        comments.forEach { comment ->
            val date = comment.sendTime
                .toInstance()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            if (lastDate == null || !date.isEqual(lastDate)) {
                val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val capitalizedMonthName = monthName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                items.add(ChatItem.DateSeparator("${date.dayOfMonth} $capitalizedMonthName"))
                lastDate = date
            }

            items.add(comment.toChatItem(state.chatId, state.myId))
        }

        _uiState.update {
            it.copy(commentItems = items, commentCount = comments.size)
        }
    }

    private fun Comment.toChatItem(chatId: Long, myId: Long): ChatItem.MessageItem {
        val isMine = senderId == myId

        val message = Message(
            id = id,
            senderId = senderId,
            chatId = chatId,
            text = text,
            sendTime = sendTime,
            isRead = true,
            status = MessageStatus.SENT,
            messageType = messageType,
            systemMessageEventType = null,
            attachments = attachments
        )

        val tag = when (senderRole) {
            CommentAuthorRole.OWNER -> context.getString(R.string.owner_tag)
            CommentAuthorRole.ADMIN -> context.getString(R.string.admin_tag)
            CommentAuthorRole.MEMBER -> null
        }

        return ChatItem.MessageItem(
            message = message,
            time = sendTime.toInstance().toPrettyTime(),
            isMine = isMine,
            isRead = null,
            senderName = if (!isMine) senderName else null,
            isFirstInGroup = true,
            isSingleEmoji = false,
            dropdownActions = emptyList(),
            chatType = ChatType.GROUP,
            canReply = false,
            senderTag = if (!isMine) tag else null
        )
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "file"
        const val SCHEME_FILE = "file"
    }
}
