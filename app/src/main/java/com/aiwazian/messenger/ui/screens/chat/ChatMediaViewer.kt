/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem

@Composable
internal fun ChatMediaViewer(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel,
    snackbarHostState: SnackbarHostState,
    tappedMedia: MessageAttachment?
) {
    if (!uiState.showFullScreenViewer) return

    val viewerSnapshot = remember(uiState.showFullScreenViewer) {
        val senderByMessageId = uiState.chatItems
            .filterIsInstance<ChatItem.MessageItem>()
            .associate { it.message.id to it.message.senderId }

        val downloadedMedia = uiState.chatItems
            .asReversed()
            .filterIsInstance<ChatItem.MessageItem>()
            .flatMap { it.message.attachments }
            .filter { attachment ->
                attachment.localUri != null && (
                        attachment.type == AttachmentType.IMAGE ||
                                attachment.type == AttachmentType.VIDEO ||
                                attachment.type == AttachmentType.GIF
                        )
            }

        val tapped = tappedMedia
        val viewerAttachments = when {
            tapped == null -> downloadedMedia
            downloadedMedia.any {
                it.messageId == tapped.messageId && it.fileId == tapped.fileId
            } -> downloadedMedia

            tapped.localUri != null -> listOf(tapped)
            else -> downloadedMedia
        }

        val entries = viewerAttachments.mapNotNull { attachment ->
            val uri = attachment.localUri ?: return@mapNotNull null

            attachment to ViewerMediaItem(
                uri = uri,
                isVideo = attachment.type == AttachmentType.VIDEO,
                messageId = attachment.messageId,
                fileId = attachment.fileId,
                viewOnce = attachment.viewOnce,
                viewOnceSeconds = attachment.viewOnceSeconds,
                isSender = senderByMessageId[attachment.messageId] == uiState.myId,
                originKey = chatMediaKey(attachment.messageId, uri)
            )
        }
        val initialPage = entries
            .indexOfFirst { (attachment, _) ->
                tapped != null &&
                        attachment.messageId == tapped.messageId &&
                        attachment.fileId == tapped.fileId
            }
            .coerceAtLeast(0)

        entries to initialPage
    }
    val viewerMedia = viewerSnapshot.first.map { it.second }
    val viewerInitialPage = viewerSnapshot.second

    DisposableEffect(viewerMedia) {
        chatViewModel.protectViewerMedia(viewerMedia.map { it.fileId })

        onDispose { chatViewModel.releaseViewerMedia(viewerMedia.map { it.fileId }) }
    }

    FullScreenViewer(
        media = viewerMedia,
        initialPage = viewerInitialPage,
        isVideoLooping = uiState.isVideoLooping,
        videoPlaybackSpeed = uiState.videoPlaybackSpeed,
        canDownloadMedia = uiState.canDownloadMedia && uiState.copyPolicy.canSaveMedia,
        viewOnceDeadlines = uiState.viewOnceDeadlines,
        deletedViewOnceFileIds = uiState.deletedViewOnceFileIds,
        onViewOnceViewed = { item ->
            chatViewModel.markViewOnceViewed(uiState.chatId, item.messageId, item.fileId)
        },
        onVideoLoopingChange = chatViewModel::setVideoLooping,
        onVideoPlaybackSpeedChange = chatViewModel::setVideoPlaybackSpeed,
        onSaveToGallery = chatViewModel::saveToGallery,
        onShowInChat = { messageId -> chatViewModel.jumpToMessage(messageId) },
        onDismiss = chatViewModel::clearMediaUrl
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AppSnackbar(snackbarHostState)
    }
}
