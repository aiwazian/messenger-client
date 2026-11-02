/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.common

import android.net.Uri
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.isAudioFile
import com.aiwazian.messenger.extensions.isPdfFile
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiText
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextPart
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MessageInteractionsController(
    private val scope: CoroutineScope,
    private val fileRepository: FileRepository,
    private val chatRepository: ChatRepository,
    private val downloaderManager: DownloaderManager,
    private val fileHandler: FileHandler,
    private val clipboardService: ClipboardService,
    private val emojiRepository: EmojiRepository,
    private val searchRepository: SearchRepository,
    private val onSnackbar: (UiText) -> Unit,
    private val onOpenUrl: (String) -> Unit,
    private val onOpenEmail: (String) -> Unit,
    private val onOpenPhone: (String) -> Unit,
    private val onNavigateToChat: (Long, Long?) -> Unit,
    private val onOpenMediaViewer: (MessageAttachment) -> Unit = {},
    private val onOpenPdf: ((Message, MessageAttachment) -> Unit)? = null,
    private val onPlayVoice: (MessageAttachment) -> Unit = {},
    private val onPlayMusic: (MessageAttachment) -> Unit = {},
    private val canCopyText: () -> Boolean = { true },
    private val canSaveMedia: () -> Boolean = { true },
    private val isAttachmentAllowed: (MessageAttachment) -> Boolean = { true },
    private val onSaveAllFailed: () -> Unit = {}
) {

    fun onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
        when (action) {
            FileAction.DOWNLOAD -> downloadFile(message, file)
            FileAction.PAUSE -> scope.launch { downloaderManager.pause(file.fileId) }
            FileAction.RESUME -> scope.launch { downloaderManager.resume(file.fileId) }
            FileAction.CANCEL -> scope.launch { downloaderManager.cancel(file.fileId) }
            FileAction.OPEN -> handleOpenFile(message, file)
            FileAction.PLAY -> {
                if (file.type == AttachmentType.VOICE) {
                    onPlayVoice(file)
                } else if (file.extension.isAudioFile()) {
                    onPlayMusic(file)
                }
            }
        }
    }

    fun copyText(message: Message) {
        if (!canCopyText()) return
        val text = message.text ?: return

        scope.launch {
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

        scope.launch {
            clipboardService.copy(phone)
        }
    }

    fun copyValue(value: String) {
        clipboardService.copy(value)
    }

    fun openLink(url: String) {
        val normalized = if (url.startsWith("http")) url else "https://$url"

        scope.launch {
            onOpenUrl(normalized)
        }
    }

    fun openEmail(email: String) {
        scope.launch {
            onOpenEmail(email)
        }
    }

    fun openPhone(phone: String) {
        scope.launch {
            onOpenPhone(phone)
        }
    }

    fun openUsername(username: String) {
        scope.launch {
            searchRepository.resolveUsername(username.removePrefix("@")).onSuccess { result ->
                if (result == null) {
                    onSnackbar(UiText.StringResource(R.string.chat_not_found))
                } else {
                    onNavigateToChat(result.chatId, null)
                }
            }
        }
    }

    fun saveToGallery(uri: Uri, onResult: (Boolean) -> Unit = {}) {
        if (!canSaveMedia()) return
        onResult(fileHandler.saveToGallery(uri.path ?: uri.toString()))
    }

    fun saveAttachmentsToDownloads(message: Message) {
        if (!canSaveMedia()) return

        scope.launch {
            val downloaded = message.attachments.filter {
                it.localUri != null && isAttachmentAllowed(it) &&
                        (it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.UPLOADED)
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
            onSnackbar(UiText.StringResource(res))
            if (successCount == 0) onSaveAllFailed()
        }
    }

    private fun downloadFile(message: Message, file: MessageAttachment) {
        scope.launch {
            if (fileRepository.getById(file.fileId)?.path != null) return@launch

            chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
                .onSuccess { url -> downloaderManager.download(url, file.name, file.fileId) }
        }
    }

    private fun handleOpenFile(message: Message, file: MessageAttachment) {
        if (file.type == AttachmentType.IMAGE ||
            file.type == AttachmentType.VIDEO ||
            file.type == AttachmentType.GIF
        ) {
            onOpenMediaViewer(file)
        } else if (file.extension.isPdfFile() && onOpenPdf != null) {
            if (file.status != DownloadStatus.UPLOADING) {
                onOpenPdf(message, file)
            }
        } else {
            scope.launch { fileHandler.openFile(file.localUri.toString()) }
        }
    }
}
