/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.Wallpaper
import com.aiwazian.messenger.domain.WallpaperTarget
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.ShareForwardOptions
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.screens.chat.components.ChatDialogs
import com.aiwazian.messenger.ui.screens.chat.components.ChatWallpaperSheet
import com.aiwazian.messenger.ui.screens.chat.components.ContactViewBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.EmojiPackBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.InviteLinkBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.MediaPickerBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.MicrophonePermissionBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.MusicPlayerSheet
import com.aiwazian.messenger.ui.screens.chat.components.PinMessageBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.StickerPackBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.utils.EmojiLink
import com.aiwazian.messenger.utils.StickerLink

@Composable
internal fun ChatScreenOverlays(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel,
    stickersViewModel: ChatStickersViewModel,
    emojiViewModel: ChatEmojiViewModel,
    stickersState: ChatStickersUiState,
    emojiState: ChatEmojiUiState,
    chatWallpaper: Wallpaper?,
    wallpaperTarget: WallpaperTarget,
    snackbarHostState: SnackbarHostState,
    navBackStack: NavBackStack<NavKey>,
    viewingContact: Message?,
    tappedMedia: MessageAttachment?,
    fileToCancelId: Long?,
    showCancelRecordingDialog: Boolean,
    showWallpaperPicker: Boolean,
    showMusicPlayerSheet: Boolean,
    pinnedMessageIds: Set<Long>,
    onFileToCancelIdChange: (Long?) -> Unit,
    onDismissCancelRecordingDialog: () -> Unit,
    onShowWallpaperPicker: () -> Unit,
    onDismissMusicPlayerSheet: () -> Unit,
    onDismissContactView: () -> Unit
) {
    ChatDialogs(uiState = uiState, chatViewModel = chatViewModel)
    
    if (fileToCancelId != null) {
        AppDialog(
            title = stringResource(R.string.cancel_sending),
            onDismissRequest = { onFileToCancelIdChange(null) },
            content = { Text(stringResource(R.string.cancel_upload_confirm)) },
            buttons = {
                TextButton(onClick = {
                    onFileToCancelIdChange(null)
                }) {
                    Text(stringResource(R.string.no))
                }
                TextButton(
                    onClick = {
                        chatViewModel.cancelUpload(fileToCancelId)
                        onFileToCancelIdChange(null)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.yes))
                }
            })
    }
    
    if (showCancelRecordingDialog) {
        AppDialog(
            title = stringResource(R.string.cancel_voice_recording),
            onDismissRequest = onDismissCancelRecordingDialog,
            content = { Text(stringResource(R.string.cancel_voice_recording_confirm)) },
            buttons = {
                TextButton(onClick = onDismissCancelRecordingDialog) {
                    Text(stringResource(R.string.continue_recording))
                }
                TextButton(
                    onClick = {
                        chatViewModel.cancelRecording()
                        onDismissCancelRecordingDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.reset))
                }
            })
    }
    
    if (uiState.showInviteBottomSheet && uiState.inviteLinkInfo != null) {
        val info = uiState.inviteLinkInfo
        InviteLinkBottomSheet(
            chatId = info.chatId,
            name = info.name.orEmpty(),
            description = info.description,
            count = info.membersCount ?: 0,
            requireApproval = info.requireApproval,
            isLoading = uiState.isProcessingInvite,
            onDismiss = chatViewModel::dismissInviteBottomSheet,
            onJoin = chatViewModel::onSubscribeViaInviteLink
        )
    }
    
    if (uiState.showMicrophonePermissionSheet) {
        MicrophonePermissionBottomSheet(
            onDismiss = chatViewModel::dismissMicrophonePermissionSheet
        )
    }
    
    if (uiState.showBannedDialog) {
        AppDialog(
            title = stringResource(R.string.no_access),
            onDismissRequest = chatViewModel::dismissBannedDialog,
            buttons = {
                TextButton(onClick = chatViewModel::dismissBannedDialog) {
                    Text(stringResource(R.string.ok))
                }
            },
            content = {
                Text(stringResource(R.string.banned_message))
            })
    }
    
    if (uiState.showBlockDialog) {
        AppDialog(
            title = stringResource(R.string.unblock),
            onDismissRequest = chatViewModel::dismissBlockDialog,
            buttons = {
                TextButton(onClick = chatViewModel::dismissBlockDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = chatViewModel::unblockUser) {
                    Text(stringResource(R.string.yes))
                }
            },
            content = {
                Text("Вы уверены что хотите разблокировать этого пользователя?")
            })
    }
    
    uiState.pinSheetMessage?.let { sheetMessage ->
        PinMessageBottomSheet(
            chatType = ChatType.fromId(uiState.chatId),
            forEveryone = uiState.pinForEveryone,
            isPinned = sheetMessage.id in pinnedMessageIds,
            onSelectScope = chatViewModel::selectPinScope,
            onConfirm = chatViewModel::confirmPin,
            onUnpin = chatViewModel::confirmUnpin,
            onDismiss = chatViewModel::dismissPinSheet
        )
    }
    
    if (uiState.showWallpaperSheet) {
        ChatWallpaperSheet(
            wallpaper = chatWallpaper,
            onDismiss = chatViewModel::hideWallpaperSheet,
            onSelectFromGallery = {
                chatViewModel.hideWallpaperSheet()
                onShowWallpaperPicker()
            },
            onReset = {
                chatViewModel.hideWallpaperSheet()
                chatViewModel.resetChatWallpaper()
            }
        )
    }
    
    viewingContact?.let { contactMessage ->
        contactMessage.contact?.let { contact ->
            ContactViewBottomSheet(
                contact = contact,
                onCopyValue = chatViewModel::copyContactValue,
                onDismiss = onDismissContactView
            )
        }
    }
    
    if (showWallpaperPicker) {
        MediaPickerBottomSheet(
            onDismissRequest = { onShowWallpaperPicker() },
            wallpaperTarget = wallpaperTarget
        )
    }
    
    if (uiState.isForwardSheetVisible) {
        val forwardingMessage = uiState.forwardingMessage
        ShareBottomSheet(
            items = uiState.forwardCandidates.map { chat ->
                ShareItem(
                    id = chat.id,
                    name = chat.chatName,
                    isSelected = chat.id in uiState.selectedForwardChatIds,
                    avatarUri = chat.avatarUri,
                    isSavedMessages = chat.id == uiState.myId
                )
            },
            onItemClick = chatViewModel::toggleForwardTarget,
            onSendClick = chatViewModel::confirmForward,
            onDismiss = chatViewModel::dismissForwardSheet,
            forwardOptions = if (forwardingMessage != null && uiState.sharingLink == null) {
                ShareForwardOptions(
                    hasAttachments = forwardingMessage.attachments.isNotEmpty(),
                    hideAuthor = uiState.forwardHideAuthor,
                    hideCaption = uiState.forwardHideCaption,
                    onHideAuthorClick = chatViewModel::toggleForwardHideAuthor,
                    onHideCaptionClick = chatViewModel::toggleForwardHideCaption
                )
            } else {
                null
            }
        )
    }
    
    stickersState.openedPack?.let { pack ->
        StickerPackBottomSheet(
            pack = pack,
            onDismiss = stickersViewModel::closePack,
            onSendSticker = { sticker ->
                stickersViewModel.closePack()
                stickersViewModel.sendSticker(uiState.chatId, sticker.id)
            },
            onInstall = stickersViewModel::installOpenedPack,
            onUninstall = stickersViewModel::uninstallOpenedPack,
            onShare = {
                stickersViewModel.closePack()
                chatViewModel.startShareLink(StickerLink.build(pack.username))
            },
            onCopyLink = {
                stickersViewModel.closePack()
                chatViewModel.copyLink(StickerLink.build(pack.username))
            })
    }
    
    emojiState.openedPack?.let { pack ->
        EmojiPackBottomSheet(
            pack = pack,
            onDismiss = emojiViewModel::closePack,
            onInstall = emojiViewModel::installOpenedPack,
            onUninstall = emojiViewModel::uninstallOpenedPack,
            onShare = {
                emojiViewModel.closePack()
                chatViewModel.startShareLink(EmojiLink.build(pack.username))
            },
            onCopyLink = {
                emojiViewModel.closePack()
                chatViewModel.copyLink(EmojiLink.build(pack.username))
            })
    }
    
    if (uiState.showFullScreenViewer) {
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
        
        val viewerEntries = viewerAttachments.mapNotNull { attachment ->
            val uri = attachment.localUri ?: return@mapNotNull null
            attachment to ViewerMediaItem(
                uri = uri,
                isVideo = attachment.type == AttachmentType.VIDEO,
                messageId = attachment.messageId,
                originKey = chatMediaKey(attachment.messageId, uri)
            )
        }
        val viewerMedia = viewerEntries.map { it.second }
        val viewerInitialPage = viewerEntries
            .indexOfFirst { (attachment, _) ->
                tapped != null &&
                        attachment.messageId == tapped.messageId &&
                        attachment.fileId == tapped.fileId
            }
            .coerceAtLeast(0)
        
        FullScreenViewer(
            media = viewerMedia,
            initialPage = viewerInitialPage,
            isVideoLooping = uiState.isVideoLooping,
            videoPlaybackSpeed = uiState.videoPlaybackSpeed,
            canDownloadMedia = uiState.canDownloadMedia && uiState.copyPolicy.canSaveMedia,
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
    
    if (showMusicPlayerSheet && uiState.currentMusicFileId != null) {
        val musicMetadata = uiState.audioMetadata[uiState.currentMusicFileId]
        MusicPlayerSheet(
            title = uiState.currentMusicTitle,
            artist = uiState.currentMusicArtist,
            cover = musicMetadata?.cover,
            isPlaying = uiState.isMusicPlaying,
            positionMs = uiState.musicPositionMs,
            durationMs = if (uiState.musicDurationMs > 0) {
                uiState.musicDurationMs
            } else {
                musicMetadata?.durationMs ?: 0
            },
            repeatMode = uiState.musicRepeatMode,
            onSeek = chatViewModel::seekMusicTo,
            onTogglePlayPause = chatViewModel::toggleMusicPlayPause,
            onToggleRepeat = chatViewModel::toggleMusicRepeat,
            onPrevious = chatViewModel::playPreviousMusicTrack,
            onNext = chatViewModel::playNextMusicTrack,
            onShare = chatViewModel::shareCurrentMusicTrack,
            onOpenEqualizer = if (uiState.equalizerInfo != null) {
                {
                    onDismissMusicPlayerSheet()
                    navBackStack.add(AppRoute.Equalizer)
                }
            } else {
                null
            },
            onDismiss = onDismissMusicPlayerSheet
        )
    }
}
