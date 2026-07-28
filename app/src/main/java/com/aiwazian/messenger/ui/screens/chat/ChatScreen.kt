/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.chat.components.ChatDialogs
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputSection
import com.aiwazian.messenger.ui.screens.chat.components.ChatTopBar
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.InviteLinkBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.MicrophonePermissionBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.SystemMessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.UnreadSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.utils.ActiveChatTracker
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun ChatScreen(
    chatId: Long,
    chatName: String? = null,
    avatarUri: String? = null,
    scrollToMessageId: Long? = null,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        chatViewModel.init(chatId, chatName, avatarUri?.toUri())
        
        if (scrollToMessageId != null) {
            chatViewModel.jumpToMessageWhenReady(scrollToMessageId)
        }
    }
    
    DisposableEffect(chatId) {
        ActiveChatTracker.pushChat(chatId)
        onDispose {
            ActiveChatTracker.popChat(chatId)
        }
    }
    
    val navBackStack = LocalNavBackStack.current
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val firstVisibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 1
        }
    }
    
    val isScrollingUp = remember { mutableStateOf(false) }
    val scrollAccumulator = remember { mutableFloatStateOf(0f) }
    val scrollThresholdPx = with(LocalDensity.current) { 20.dp.toPx() }
    
    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            val delta = if (index == lastIndex) {
                (offset - lastOffset).toFloat()
            } else if (index > lastIndex) {
                scrollThresholdPx + 1f
            } else {
                -(scrollThresholdPx + 1f)
            }
            
            val newAccumulator = scrollAccumulator.floatValue + delta
            scrollAccumulator.floatValue = newAccumulator
            
            when {
                newAccumulator > scrollThresholdPx -> {
                    isScrollingUp.value = false
                    scrollAccumulator.floatValue = 0f
                }
                
                newAccumulator < -scrollThresholdPx -> {
                    isScrollingUp.value = true
                    scrollAccumulator.floatValue = 0f
                }
            }
            
            lastIndex = index
            lastOffset = offset
        }
    }
    
    val lastVisibleItemIndex = remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    
    LaunchedEffect(
        firstVisibleItemIndex.value,
        uiState.hasMoreMessages,
        uiState.isRelocating,
        uiState.isLoadingOlder
    ) {
        if (uiState.isRelocating || uiState.isLoading || !uiState.isFirstLoadDone) return@LaunchedEffect
        if (uiState.scrollTarget != null) return@LaunchedEffect
        if (firstVisibleItemIndex.value < PREFETCH_THRESHOLD && uiState.hasMoreMessages && !uiState.isLoadingOlder) {
            chatViewModel.loadOlderMessages()
        }
    }
    
    LaunchedEffect(
        lastVisibleItemIndex.value,
        uiState.hasMoreNewerMessages,
        uiState.isRelocating,
        uiState.isLoadingNewer
    ) {
        if (uiState.isRelocating || uiState.isLoading || !uiState.isFirstLoadDone) return@LaunchedEffect
        if (uiState.scrollTarget != null) return@LaunchedEffect
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0 &&
            lastVisibleItemIndex.value >= total - PREFETCH_THRESHOLD &&
            uiState.hasMoreNewerMessages &&
            !uiState.isLoadingNewer
        ) {
            chatViewModel.loadNewerMessages()
        }
    }
    
    LaunchedEffect(uiState.scrollTarget?.requestId, uiState.chatItems) {
        val target = uiState.scrollTarget ?: return@LaunchedEffect
        val targetId = target.messageId
        
        if (targetId == null) {
            val lastIndex = uiState.chatItems.size + 1
            if (target.animate) listState.animateScrollToItem(lastIndex)
            else listState.animateScrollToItem(lastIndex)
            chatViewModel.onScrollTargetHandled(target.requestId)
            return@LaunchedEffect
        }
        
        val itemIndex = uiState.chatItems.indexOfFirst {
            it is ChatItem.MessageItem && it.message.id == targetId
        }
        if (itemIndex < 0) return@LaunchedEffect
        
        val anchorIndex =
            if (itemIndex > 0 && uiState.chatItems[itemIndex - 1] is ChatItem.UnreadSeparator) {
                itemIndex - 1
            } else itemIndex
        
        val listIndex = anchorIndex + 1
        if (target.animate) {
            listState.animateScrollToItem(listIndex)
        } else {
            val offset =
                -(listState.layoutInfo.viewportSize.height * target.viewportFraction).toInt()
            listState.animateScrollToItem(listIndex, offset)
        }
        chatViewModel.onScrollTargetHandled(target.requestId)
    }
    
    LaunchedEffect(isAtBottom) {
        chatViewModel.onViewportAtBottomChanged(isAtBottom)
    }
    
    LaunchedEffect(listState, uiState.myId) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            
            layoutInfo.visibleItemsInfo.filter { info ->
                if (info.size <= 0) return@filter false
                val visiblePart =
                    (minOf(info.offset + info.size, viewportEnd) - maxOf(
                        info.offset,
                        viewportStart
                    ))
                visiblePart * 2 >= info.size
            }.mapNotNull { info ->
                val chatItem = uiState.chatItems.getOrNull(info.index - 1)
                (chatItem as? ChatItem.MessageItem)?.message
            }.filter { message ->
                message.id > 0 && !message.isRead && message.senderId != uiState.myId
            }.maxOfOrNull { it.id }
        }.distinctUntilChanged()
            .debounce(READ_REPORT_DEBOUNCE_MS.milliseconds)
            .collect { messageId ->
                if (messageId != null) chatViewModel.onMessagesSeen(messageId)
        }
    }
    
    var fileToCancelId by remember { mutableStateOf<Long?>(null) }
    var showCancelRecordingDialog by remember { mutableStateOf(false) }
    /**
     * Вложение, по которому открыли просмотрщик.
     *
     * Берём именно тот объект, который отрисовал MessageBubble: у него тип уже
     * пересчитан по mime скачанного файла (ChatItemMapper.processAttachments),
     * а не взят из ответа сервера.
     */
    var tappedMedia by remember { mutableStateOf<MessageAttachment?>(null) }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    val onBackClick: () -> Unit = {
        if (uiState.isRecording) {
            showCancelRecordingDialog = true
        } else {
            navBackStack.removeLastOrNull()
        }
    }
    
    BackHandler(enabled = uiState.isRecording) {
        showCancelRecordingDialog = true
    }
    
    LaunchedEffect(Unit) {
        chatViewModel.uiEffect.collect { effect ->
            when (effect) {
                is ChatUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                is ChatUiEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }
                
                is ChatUiEffect.ScrollToBottom -> {
                    if (effect.index >= 0) {
                        listState.animateScrollToItem(effect.index)
                    }
                }
                
                is ChatUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                
                is ChatUiEffect.NavigateToChat -> {
                    navBackStack.add(
                        AppRoute.Chat(
                            chatId = effect.chatId,
                            scrollToMessageId = effect.scrollToMessageId
                        )
                    )
                }
                
                is ChatUiEffect.OpenUrl -> {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }
                
                is ChatUiEffect.OpenEmail -> {
                    val mailIntent = Intent(
                        Intent.ACTION_SENDTO,
                        "mailto:${Uri.encode(effect.email)}".toUri()
                    )
                    val chooser = Intent.createChooser(
                        mailIntent,
                        UiText.StringResource(R.string.open_email_with).asString(context)
                    )
                    
                    val launched = runCatching { context.startActivity(chooser) }.isSuccess
                    if (!launched) {
                        snackbarJob?.cancel()
                        snackbarJob = scope.launch {
                            snackbarHostState.showSnackbar(
                                message = UiText.StringResource(R.string.no_email_app)
                                    .asString(context),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
    }
    
    Scaffold(snackbarHost = {
        if (!uiState.showFullScreenViewer) {
            CustomSnackbar(snackbarHostState)
        }
    }, topBar = {
        ChatTopBar(
            title = uiState.chatName.asString(),
            avatarUri = uiState.avatarUri,
            subTitle = uiState.subTitle.asString(),
            topBarActions = uiState.topBarActions,
            isConnected = uiState.isConnected,
            chatId = uiState.chatId,
            myId = uiState.myId,
            onBackClick = onBackClick
        )
    }, bottomBar = {
        ChatInputSection(
            uiState = uiState, chatViewModel = chatViewModel
        )
    }, floatingActionButton = {
        AnimatedVisibility(
            visible = (!isAtBottom || !uiState.isAtLiveEdge) && !isScrollingUp.value && !uiState.isRecording,
            enter = scaleIn() + fadeIn() + slideInVertically { it },
            exit = scaleOut() + fadeOut() + slideOutVertically { it },
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                targetValue = if (isPressed) 0.9f else 1f,
                label = "scroll_bottom_button_scale_animation"
            )
            FloatingActionButton(
                onClick = { chatViewModel.jumpToLatest() },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                interactionSource = interactionSource,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null
                )
            }
        }
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    overscrollEffect = rememberOverscrollEffect()
                ) {
                    item(key = "chat_header") {
                        Column {
                            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                            if (uiState.isLoadingOlder) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator()
                                }
                            }
                        }
                    }
                    
                    items(
                        items = uiState.chatItems, key = { item ->
                            when (item) {
                                is ChatItem.DateSeparator -> "date_${item.text}"
                                is ChatItem.UnreadSeparator -> "unread_separator"
                                is ChatItem.SystemMessage -> "sys_${item.sendTime}"
                                is ChatItem.MessageItem -> "msg_${item.message.id}"
                            }
                        }) { item ->
                        when (item) {
                            is ChatItem.DateSeparator -> DateSeparatorItem(
                                item.text, Modifier.animateItem()
                            )
                            
                            is ChatItem.UnreadSeparator -> UnreadSeparatorItem(
                                Modifier.animateItem()
                            )
                            
                            is ChatItem.SystemMessage -> SystemMessageBubble(
                                item.text.asString(), Modifier.animateItem()
                            )
                            
                            is ChatItem.MessageItem -> MessageBubble(
                                modifier = Modifier.animateItem(),
                                item = item,
                                onFileAction = { file, action ->
                                    if (action == FileAction.CANCEL) {
                                        fileToCancelId =
                                            item.message.id
                                    } else {
                                        if (action == FileAction.OPEN) {
                                            tappedMedia = file
                                        }
                                        chatViewModel.onFileAction(
                                            item.message,
                                            file,
                                            action
                                        )
                                    }
                                },
                                currentPlayingVoiceFileId = uiState.currentPlayingVoiceFileId,
                                isVoicePlaying = uiState.isVoicePlaying,
                                voicePositionMs = uiState.voicePositionMs,
                                voiceDurationMs = uiState.voiceDurationMs,
                                onVoiceSeek = chatViewModel::onVoiceSeek,
                                onLinkClicked = chatViewModel::onLinkClicked,
                                onUsernameClicked = chatViewModel::onUsernameClicked,
                                onEmailClicked = chatViewModel::onEmailClicked,
                                onSaveToDownloads = {
                                    chatViewModel.saveAttachmentsToDownloads(
                                        item.message
                                    )
                                },
                                onReplyPreviewClick = {
                                    chatViewModel.onReplyPreviewClicked(item.message)
                                },
                                onForwardedFromClick = {
                                    chatViewModel.onForwardedFromClicked(item.message)
                                },
                                onSwipeThresholdReached = chatViewModel::vibrateTactile,
                                onSwipeToReply = {
                                    chatViewModel.startReply(item.message)
                                })
                        }
                    }
                    
                    item(key = "chat_footer") {
                        Column {
                            if (uiState.isLoadingNewer) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator()
                                }
                            }
                            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
                        }
                    }
                }
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(innerPadding.calculateTopPadding())
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .height(
                        innerPadding.calculateBottomPadding() + WindowInsets().getBottom(
                            LocalDensity.current
                        ).dp
                    )
                    .fillMaxWidth()
                    .imePadding()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            )
                        )
                    )
            )
        }
        
        ChatDialogs(uiState = uiState, chatViewModel = chatViewModel)
        
        if (fileToCancelId != null) {
            CustomDialog(
                title = stringResource(R.string.cancel_sending),
                onDismissRequest = { fileToCancelId = null },
                content = { Text(stringResource(R.string.cancel_upload_confirm)) },
                buttons = {
                    TextButton(onClick = {
                        fileToCancelId = null
                    }) {
                        Text(stringResource(R.string.no))
                    }
                    TextButton(
                        onClick = {
                            fileToCancelId?.let {
                                chatViewModel.cancelUpload(it)
                            }
                            fileToCancelId = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                })
        }
        
        if (showCancelRecordingDialog) {
            CustomDialog(
                title = stringResource(R.string.cancel_voice_recording),
                onDismissRequest = { showCancelRecordingDialog = false },
                content = { Text(stringResource(R.string.cancel_voice_recording_confirm)) },
                buttons = {
                    TextButton(onClick = { showCancelRecordingDialog = false }) {
                        Text(stringResource(R.string.continue_recording))
                    }
                    TextButton(
                        onClick = {
                            chatViewModel.cancelRecording()
                            showCancelRecordingDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                })
        }
        
        if (uiState.showInviteBottomSheet && uiState.inviteLinkInfo != null) {
            val info = uiState.inviteLinkInfo!!
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
            CustomDialog(
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
            CustomDialog(
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
    }
    
    if (uiState.isForwardSheetVisible) {
        ShareBottomSheet(
            items = uiState.forwardCandidates.map { chat ->
                ShareItem(
                    id = chat.id,
                    name = chat.chatName,
                    isSelected = chat.id in uiState.selectedForwardChatIds,
                    avatarUri = chat.avatarUri
                )
            },
            onItemClick = chatViewModel::toggleForwardTarget,
            onSendClick = chatViewModel::confirmForward,
            onDismiss = chatViewModel::dismissForwardSheet
        )
    }
    
    if (uiState.showFullScreenViewer) {
        /*
         * Список медиа собираем из chatItems, а не из uiState.mediaItems.
         *
         * В mediaItems вложения лежат с типом, пришедшим с сервера, а пузырь
         * рисует chatItems, где ChatItemMapper.processAttachments пересчитывает
         * тип по mime уже скачанного файла. Из-за расхождения у заново
         * скачанных фото и видео просмотрщик получал пустой список: страницы
         * были пустыми и пейджер не листался, хотя превью в пузыре рисовалось.
         *
         * В просмотрщик уходят только скачанные вложения: у остальных localUri
         * ещё null и показывать нечего.
         */
        val downloadedMedia = remember(uiState.chatItems) {
            uiState.chatItems
                .filterIsInstance<ChatItem.MessageItem>()
                .flatMap { it.message.attachments }
                .filter { attachment ->
                    attachment.localUri != null && (
                            attachment.type == AttachmentType.IMAGE ||
                                    attachment.type == AttachmentType.VIDEO ||
                                    attachment.type == AttachmentType.GIF
                            )
                }
        }
        
        val tapped = tappedMedia
        /* Нажатое вложение показываем даже в одиночку: пустой пейджер недопустим. */
        val viewerAttachments = when {
            tapped == null -> downloadedMedia
            downloadedMedia.any { it.fileId == tapped.fileId } -> downloadedMedia
            tapped.localUri != null -> listOf(tapped)
            else -> downloadedMedia
        }
        
        val viewerMedia = viewerAttachments.mapNotNull { attachment ->
            val uri = attachment.localUri ?: return@mapNotNull null
            ViewerMediaItem(
                uri = uri,
                isVideo = attachment.type == AttachmentType.VIDEO
            )
        }
        val viewerInitialPage = viewerAttachments
            .indexOfFirst { it.fileId == tapped?.fileId }
            .coerceAtLeast(0)
        
        FullScreenViewer(
            media = viewerMedia,
            initialPage = viewerInitialPage,
            isVideoLooping = uiState.isVideoLooping,
            videoPlaybackSpeed = uiState.videoPlaybackSpeed,
            canDownloadMedia = uiState.canDownloadMedia,
            onVideoLoopingChange = chatViewModel::setVideoLooping,
            onVideoPlaybackSpeedChange = chatViewModel::setVideoPlaybackSpeed,
            onSaveToGallery = chatViewModel::saveToGallery,
            onDismiss = chatViewModel::clearMediaUrl
        )
        BackHandler {
            chatViewModel.clearMediaUrl()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            CustomSnackbar(snackbarHostState)
        }
    }
}

/** За сколько элементов до границы окна начинать догрузку. */
private const val PREFETCH_THRESHOLD = 10

/**
 * Сколько ждать перед отправкой отметки о прочтении.
 *
 * Без паузы быстрый скролл через десятки сообщений помечал бы прочитанным всё,
 * что промелькнуло мимо экрана.
 */
private const val READ_REPORT_DEBOUNCE_MS = 400L
