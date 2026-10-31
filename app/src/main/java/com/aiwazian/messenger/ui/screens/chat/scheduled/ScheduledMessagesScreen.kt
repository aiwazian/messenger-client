/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.scheduled

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.MediaPickerViewModel
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputSection
import com.aiwazian.messenger.ui.screens.chat.components.ContactViewBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.MicrophonePermissionBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.ScheduleSendBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.SystemMessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ScheduledMessagesScreen(
    chatId: Long,
    viewModel: ScheduledMessagesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val mediaPickerViewModel: MediaPickerViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var tappedMedia by remember { mutableStateOf<MessageAttachment?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    var viewingContact by remember { mutableStateOf<MessageContact?>(null) }
    
    LaunchedEffect(chatId) {
        viewModel.init(chatId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ScheduledMessagesUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is ScheduledMessagesUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                
                is ScheduledMessagesUiEffect.OpenUrl -> {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }
                
                is ScheduledMessagesUiEffect.OpenEmail -> {
                    val mailIntent = Intent(
                        Intent.ACTION_SENDTO,
                        "mailto:${Uri.encode(effect.email)}".toUri()
                    )
                    val chooser = Intent.createChooser(
                        mailIntent,
                        UiText.StringResource(R.string.open_email_with).asString(context)
                    )
                    
                    runCatching { context.startActivity(chooser) }
                }
                
                is ScheduledMessagesUiEffect.OpenPhone -> {
                    val dialIntent = Intent(
                        Intent.ACTION_DIAL,
                        "tel:${effect.phone}".toUri()
                    )
                    val chooser = Intent.createChooser(
                        dialIntent,
                        UiText.StringResource(R.string.open_phone_with).asString(context)
                    )
                    
                    runCatching { context.startActivity(chooser) }
                }
                
                is ScheduledMessagesUiEffect.NavigateToChat -> {
                    navBackStack.add(
                        AppRoute.Chat(
                            chatId = effect.chatId,
                            scrollToMessageId = effect.scrollToMessageId
                        )
                    )
                }
            }
        }
    }
    
    Scaffold(snackbarHost = {
        AppSnackbar(snackbarHostState)
    }, topBar = {
        PageTopBar(
            title = {
                Text(text = stringResource(R.string.scheduled_messages_title))
            }
        )
    }, bottomBar = {
        ChatInputSection(
            uiState = chatUiState,
            actions = viewModel,
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                ),
            alwaysShowInput = true,
            sendOptionsEnabled = false,
            onMediaSheetSend = { _, _ ->
                viewModel.onMediaSheetSendRequested(mediaPickerViewModel.uiState.value)
            },
            sendIcon = Icons.Rounded.Schedule
        )
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.items.isEmpty()) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator()
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_scheduled_messages),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ScheduledMessagesList(
                    items = uiState.items,
                    uiState = uiState,
                    viewModel = viewModel,
                    onTappedMediaChanged = { tappedMedia = it },
                    onContactClick = { viewingContact = it },
                    contentPadding = innerPadding,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            
            TopBarScrim(height = innerPadding.calculateTopPadding())
            
            BottomBarScrim(height = innerPadding.calculateBottomPadding())
        }
    }
    
    if (chatUiState.showMicrophonePermissionSheet) {
        MicrophonePermissionBottomSheet(onDismiss = viewModel::dismissMicrophonePermissionSheet)
    }
    
    if (uiState.rescheduleMessage != null) {
        ScheduleSendBottomSheet(
            onDismissRequest = viewModel::dismissRescheduleSheet,
            initialTime = uiState.rescheduleMessage?.scheduledAt,
            onConfirm = viewModel::confirmReschedule
        )
    }
    
    if (uiState.showScheduleSendSheet) {
        ScheduleSendBottomSheet(
            onDismissRequest = viewModel::dismissScheduleSendSheet,
            onConfirm = viewModel::onSendScheduledClicked
        )
    }
    
    if (uiState.deleteMessage != null) {
        AppDialog(
            title = stringResource(R.string.delete_scheduled_message_title),
            onDismissRequest = viewModel::dismissDeleteDialog,
            content = {
                Text(
                    text = stringResource(R.string.delete_scheduled_message_confirm),
                    lineHeight = 16.sp
                )
            },
            buttons = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = viewModel::confirmDeleteMessage,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            }
        )
    }
    
    viewingContact?.let { contact ->
        ContactViewBottomSheet(
            contact = contact,
            onCopyValue = { value ->
                viewModel.onCopyContactValue(value)
            },
            onDismiss = { viewingContact = null }
        )
    }
    
    if (uiState.showFullScreenViewer) {
        val downloadedMedia = remember(uiState.items) {
            uiState.items
                .filterIsInstance<ChatItem.MessageItem>()
                .flatMap { it.message.attachments }
                .filter { attachment ->
                    attachment.localUri != null && (
                            attachment.type == AttachmentType.IMAGE ||
                                    attachment.type == AttachmentType.VIDEO ||
                                    attachment.type == AttachmentType.GIF
                            )
                }
                .distinctBy { it.messageId to it.fileId }
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
                originKey = chatMediaKey(
                    attachment.messageId,
                    uri,
                    SCHEDULED_MEDIA_KEY_SCOPE
                )
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
            canDownloadMedia = true,
            onVideoLoopingChange = viewModel::onVideoLoopingChange,
            onVideoPlaybackSpeedChange = viewModel::onVideoPlaybackSpeedChange,
            onSaveToGallery = viewModel::onSaveToGallery,
            onDismiss = viewModel::onViewerDismiss
        )
    }
}

@Composable
private fun ScheduledMessagesList(
    items: List<ChatItem>,
    uiState: ScheduledMessagesUiState,
    viewModel: ScheduledMessagesViewModel,
    onTappedMediaChanged: (MessageAttachment?) -> Unit,
    onContactClick: (MessageContact?) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = modifier,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        overscrollEffect = rememberOverscrollEffect(),
        contentPadding = contentPadding
    ) {
        itemsIndexed(items = items, key = { index, item ->
            when (item) {
                is ChatItem.DateSeparator -> "date_${item.text}_$index"
                is ChatItem.UnreadSeparator -> "unread_separator_$index"
                is ChatItem.SystemMessage -> "sys_${item.sendTime}_$index"
                is ChatItem.MessageItem -> "msg_${item.message.id}"
            }
        }) { _, item ->
            when (item) {
                is ChatItem.DateSeparator -> DateSeparatorItem(
                    item.text, Modifier.animateItem()
                )
                
                is ChatItem.UnreadSeparator -> Unit
                
                is ChatItem.SystemMessage -> SystemMessageBubble(
                    item.text.asString(), Modifier.animateItem()
                )
                
                is ChatItem.MessageItem -> MessageBubble(
                    modifier = Modifier.animateItem(),
                    item = item,
                    mediaKeyScope = SCHEDULED_MEDIA_KEY_SCOPE,
                    onFileAction = { file, action ->
                        if (action == FileAction.OPEN) {
                            onTappedMediaChanged(file)
                        }
                        viewModel.onFileAction(item.message, file, action)
                    },
                    currentPlayingVoiceFileId = uiState.currentPlayingVoiceFileId,
                    isVoicePlaying = uiState.isVoicePlaying,
                    voicePositionMs = uiState.voicePositionMs,
                    voiceDurationMs = uiState.voiceDurationMs,
                    onVoiceSeek = viewModel::onVoiceSeek,
                    audioMetadata = uiState.audioMetadata,
                    currentMusicFileId = uiState.currentMusicFileId,
                    isMusicPlaying = uiState.isMusicPlaying,
                    musicPositionMs = uiState.musicPositionMs,
                    musicDurationMs = uiState.musicDurationMs,
                    onMusicSeek = viewModel::onMusicSeek,
                    onLinkClicked = viewModel::onLinkClicked,
                    onUsernameClicked = viewModel::onUsernameClicked,
                    onEmailClicked = viewModel::onEmailClicked,
                    onPhoneClicked = viewModel::onPhoneClicked,
                    onSaveToDownloads = {
                        viewModel.saveAttachmentsToDownloads(item.message)
                    },
                    onReplyPreviewClick = {
                        viewModel.onReplyPreviewClicked(item.message)
                    },
                    onForwardedFromClick = {
                        viewModel.onForwardedFromClicked(item.message)
                    },
                    onContactClick = if (item.message.messageType == MessageType.CONTACT) {
                        { onContactClick(item.message.contact) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

private const val SCHEDULED_MEDIA_KEY_SCOPE = "scheduled"
