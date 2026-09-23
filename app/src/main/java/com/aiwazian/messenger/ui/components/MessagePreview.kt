package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.SystemMessageEventType
import com.aiwazian.messenger.extensions.isMusicFile
import com.aiwazian.messenger.utils.AudioMetadataCache

/**
 * Однострочный превью-текст сообщения: то же правило, что и у последнего
 * сообщения в списке чатов — вложение подписывается («Фото», «Голосовое
 * сообщение»), кастомные эмодзи отображаются внутри текста.
 */
data class MessagePreview(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
fun rememberMessagePreview(
    message: Message,
    showMyPrefix: Boolean = false,
    myPrefix: String = ""
): MessagePreview {
    val emojiSourceText = when {
        message.sticker != null -> ""
        message.attachments.isNotEmpty() -> ""
        else -> message.text.orEmpty()
    }

    val emojiInlineContent = rememberCustomEmojiInlineContent(
        text = emojiSourceText,
        emojiSize = MESSAGE_PREVIEW_EMOJI_SIZE
    )

    val text = when {
        message.sticker != null -> {
            val emoji = message.sticker.emojis.firstOrNull()
            val stickerLabel = stringResource(R.string.sticker_message)

            buildAnnotatedString {
                if (!emoji.isNullOrBlank()) {
                    append("$emoji ")
                }

                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(stickerLabel)
                }
            }
        }

        message.attachments.isNotEmpty() -> {
            val attachment = message.attachments.first()
            val musicPreview = rememberMusicPreview(attachment)

            if (musicPreview != null) {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(musicPreview)
                    }
                }
            } else {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(attachmentLabel(attachment.type))
                    }
                }
            }
        }

        !message.text.isNullOrBlank() -> buildAnnotatedString {
            if (showMyPrefix) {
                append("$myPrefix: ")
            }
            appendCustomEmojiText(message.text.trim())
        }

        message.systemMessageEventType != null -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(systemEventLabel(message.systemMessageEventType))
            }
        }

        else -> AnnotatedString("")
    }

    return MessagePreview(text = text, inlineContent = emojiInlineContent)
}

@Composable
private fun rememberMusicPreview(attachment: MessageAttachment): String? {
    if (!attachment.extension.isMusicFile()) return null

    var metadata by remember(attachment.fileId) { mutableStateOf<AudioTrackMetadata?>(null) }

    LaunchedEffect(attachment.fileId, attachment.localUri) {
        val uri = attachment.localUri ?: return@LaunchedEffect
        metadata = AudioMetadataCache.get(
            fileId = attachment.fileId,
            filePath = uri.path ?: uri.toString(),
            fallbackTitle = attachment.name
        )
    }

    val title = metadata?.title ?: attachment.name.substringBeforeLast('.')
    val artist = metadata?.artist?.takeIf { it.isNotBlank() }
    return if (artist == null) title else "$title - $artist"
}

@Composable
private fun attachmentLabel(type: AttachmentType): String = stringResource(
    when (type) {
        AttachmentType.IMAGE -> R.string.photo
        AttachmentType.FILE -> R.string.file
        AttachmentType.VIDEO -> R.string.video
        AttachmentType.VOICE -> R.string.voice_message
        AttachmentType.GIF -> R.string.gif
    }
)

@Composable
private fun systemEventLabel(type: SystemMessageEventType): String = stringResource(
    when (type) {
        SystemMessageEventType.CHANNEL_CREATED -> R.string.channel_created
        SystemMessageEventType.GROUP_CREATED -> R.string.group_created
        SystemMessageEventType.HISTORY_CLEARED -> R.string.history_cleared
    }
)

private val MESSAGE_PREVIEW_EMOJI_SIZE = 14.sp
