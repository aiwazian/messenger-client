/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import androidx.activity.compose.BackHandler
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
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
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
import com.aiwazian.messenger.utils.ActiveChatTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatId: Long,
    chatName: String? = null,
    avatarUri: String? = null,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        chatViewModel.init(chatId, chatName, avatarUri?.toUri())
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
    
    LaunchedEffect(firstVisibleItemIndex.value) {
        if (firstVisibleItemIndex.value < 10 && uiState.hasMoreMessages && !uiState.isLoadingMore && !uiState.isLoading && uiState.isFirstLoadDone) {
            chatViewModel.loadMoreMessages()
        }
    }
    
    var fileToCancelId by remember { mutableStateOf<Long?>(null) }
    var showCancelRecordingDialog by remember { mutableStateOf(false) }
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
                    navBackStack.add(AppRoute.Chat(effect.chatId, null))
                }
                
                is ChatUiEffect.OpenUrl -> {
                    try {
                        val intent =
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                effect.url.toUri()
                            )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback or error handling
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
            onBackClick = onBackClick
        )
    }, bottomBar = {
        ChatInputSection(
            uiState = uiState, chatViewModel = chatViewModel
        )
    }, floatingActionButton = {
        AnimatedVisibility(
            visible = !isAtBottom && !isScrollingUp.value && !uiState.isRecording,
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
                onClick = {
                    scope.launch {
                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                        if (lastIndex >= 0) {
                            listState.animateScrollToItem(lastIndex)
                        }
                    }
                },
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
                    item {
                        Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                    }
                    
                    if (uiState.isLoadingMore) {
                        item {
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
                    
                    items(
                        items = uiState.chatItems, key = { item ->
                            when (item) {
                                is ChatItem.DateSeparator -> "date_${item.text}"
                                is ChatItem.SystemMessage -> "sys_${item.sendTime}"
                                is ChatItem.MessageItem -> "msg_${item.message.id}"
                            }
                        }) { item ->
                        when (item) {
                            is ChatItem.DateSeparator -> DateSeparatorItem(
                                item.text, Modifier.animateItem()
                            )
                            
                            is ChatItem.SystemMessage -> SystemMessageBubble(
                                item.text.asString(), Modifier.animateItem()
                            )
                            
                            is ChatItem.MessageItem -> MessageBubble(
                                modifier = Modifier.animateItem(),
                                item = item,
                                onSeen = {
                                    chatViewModel.markAsReadMessage(
                                        item.message
                                    )
                                },
                                onFileAction = { file, action ->
                                    if (action == FileAction.CANCEL) {
                                        fileToCancelId =
                                            item.message.id
                                    } else {
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
                                onSaveToDownloads = {
                                    chatViewModel.saveAttachmentsToDownloads(
                                        item.message
                                    )
                                })
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
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
    }
    
    if (uiState.showFullScreenViewer) {
        FullScreenViewer(
            mediaUris = uiState.mediaItems.map { it.localUri },
            initialPage = uiState.initialMediaIndex,
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
