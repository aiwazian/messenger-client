/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.push.NotificationHelper
import com.aiwazian.messenger.ui.components.AnimatedDotsText
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomDropdownMenu
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.SystemMessageBubble
import com.aiwazian.messenger.utils.ActiveChatTracker
import com.aiwazian.messenger.utils.DialogController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

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
        NotificationHelper.clearChatNotifications(context, chatId)
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
    
    LaunchedEffect(firstVisibleItemIndex.value) {
        if (firstVisibleItemIndex.value < 10 && uiState.hasMoreMessages && !uiState.isLoadingMore && !uiState.isLoading) {
            chatViewModel.loadMoreMessages()
        }
    }
    
    var fileToCancelId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
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
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.sharedBounds(key = "chat-${chatId}"),
        snackbarHost = {
            if (!uiState.showFullScreenViewer) {
                CustomSnackbar(snackbarHostState)
            }
        },
        topBar = {
            TopBar(
                title = uiState.chatName.asString(),
                avatarUri = uiState.avatarUri,
                subTitle = uiState.subTitle.asString(),
                topBarActions = uiState.topBarActions,
                isConnected = uiState.isConnected,
                chatId = uiState.chatId
            )
        },
        bottomBar = {
            BottomSection(
                uiState = uiState,
                chatViewModel = chatViewModel
            )
        },
    ) { innerPadding ->
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
                            is ChatItem.DateSeparator -> DateSeparatorItem(item.text)
                            is ChatItem.SystemMessage -> SystemMessageBubble(item.text.asString())
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
                                onLinkClicked = chatViewModel::onLinkClicked,
                                onUsernameClicked = chatViewModel::onUsernameClicked
                            )
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
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .height(innerPadding.calculateBottomPadding())
                    .fillMaxWidth()
                    .imePadding()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            )
                        )
                    )
            )
        }
        
        Dialogs(uiState = uiState, chatViewModel = chatViewModel)
        
        if (fileToCancelId != null) {
            CustomDialog(
                title = "Отменить отправку",
                onDismissRequest = { fileToCancelId = null },
                content = { Text("Вы уверены, что хотите отменить отправку файла?") },
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
        
        if (uiState.showBannedDialog) {
            CustomDialog(
                title = "Нет доступа",
                onDismissRequest = chatViewModel::dismissBannedDialog,
                buttons = {
                    TextButton(onClick = chatViewModel::dismissBannedDialog) {
                        Text(stringResource(R.string.ok))
                    }
                },
                content = {
                    Text("Вас заблокировал администратор этого чата")
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

@Composable
private fun BottomSection(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp)
    ) {
        when (ChatType.fromId(uiState.chatId)) {
            ChatType.CHANNEL -> {
                if (uiState.isOwner) {
                    InputMessage(
                        uiState = uiState,
                        chatViewModel = chatViewModel
                    )
                } else if (!uiState.isJoined) {
                    JoinButton(onClick = chatViewModel::onJoinClicked)
                }
            }
            
            ChatType.GROUP -> {
                if (uiState.isOwner) {
                    InputMessage(
                        uiState = uiState,
                        chatViewModel = chatViewModel
                    )
                } else if (uiState.isJoined) {
                    InputMessage(
                        uiState = uiState,
                        chatViewModel = chatViewModel
                    )
                } else {
                    JoinButton(onClick = chatViewModel::onJoinClicked)
                }
            }
            
            ChatType.PRIVATE -> {
                InputMessage(
                    uiState = uiState,
                    chatViewModel = chatViewModel
                )
            }
            
            else -> {}
        }
    }
}

@Composable
private fun JoinButton(onClick: () -> Unit) {
    TextButton(
        shape = RectangleShape, modifier = Modifier.fillMaxWidth(), onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.join).uppercase(),
            modifier = Modifier.padding(vertical = 8.dp),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Dialogs(
    uiState: ChatUiState, chatViewModel: ChatViewModel
) {
    if (uiState.showDeleteChatDialog) {
        val isPrivateChat =
            ChatType.fromId(uiState.chatId) == ChatType.PRIVATE && uiState.chatId != uiState.myId
        DeleteChatDialog(
            onDismissRequest = chatViewModel::hideDeleteChatDialog,
            onConfirm = chatViewModel::onDeleteChatConfirmed,
            vibrate = chatViewModel::vibrate,
            deleteForRecipient = uiState.deleteForRecipient,
            onDeleteForRecipientChanged = chatViewModel::setDeleteForRecipient,
            isPrivateChat = isPrivateChat
        )
    }
    
    if (uiState.showClearHistoryDialog) {
        val isPrivateChat =
            ChatType.fromId(uiState.chatId) == ChatType.PRIVATE && uiState.chatId != uiState.myId
        ClearHistoryDialog(
            onDismissRequest = chatViewModel::hideClearHistoryDialog,
            onConfirm = chatViewModel::onDeleteMessagesConfirmed,
            vibrate = chatViewModel::vibrate,
            clearForRecipient = uiState.deleteForRecipient,
            onClearForRecipientChanged = chatViewModel::setDeleteForRecipient,
            isPrivateChat = isPrivateChat
        )
    }
    
    if (uiState.showDeleteMessageDialog) {
        val isPrivateChat =
            ChatType.fromId(uiState.chatId) == ChatType.PRIVATE && uiState.chatId != uiState.myId
        DeleteMessageDialog(
            onDismissRequest = chatViewModel::hideDeleteMessageDialog,
            onConfirm = chatViewModel::onDeleteMessageConfirmed,
            deleteForRecipient = uiState.deleteForRecipient,
            onDeleteForRecipientChanged = chatViewModel::setDeleteForRecipient,
            isPrivateChat = isPrivateChat
        )
    }
    
    if (uiState.showLeaveDialog) {
        val chatType = ChatType.fromId(uiState.chatId)
        LeaveDialog(
            onDismiss = chatViewModel::hideLeaveDialog,
            onConfirm = chatViewModel::onLeaveClicked,
            chatName = uiState.chatName.asString(),
            chatType = chatType
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    avatarUri: Uri?,
    subTitle: String,
    topBarActions: List<TopBarAction>,
    isConnected: Boolean,
    chatId: Long
) {
    val navBackStack = LocalNavBackStack.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.96f else 1f,
        label = "card_scale_animation"
    )
    
    TopAppBar(
        title = {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(
                            interactionSource = interactionSource, indication = null, onClick = {
                                navBackStack.add(
                                    AppRoute.Profile(
                                        profileId = chatId,
                                        profileName = title,
                                        avatarUri = avatarUri?.toString()
                                    )
                                )
                            }), horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatAvatar(id = chatId, chatName = title, avatarUri = avatarUri)
                        
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = title,
                                maxLines = 1,
                                fontSize = 18.sp,
                                lineHeight = 16.sp,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.sharedElement(key = "chat-name-$chatId")
                            )
                            
                            AnimatedContent(
                                targetState = isConnected, transitionSpec = {
                                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                }, label = "connection_animation"
                            ) { connected ->
                                if (!connected) {
                                    AnimatedDotsText(
                                        text = stringResource(R.string.connecting),
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (subTitle.isNotBlank()) {
                                    Text(
                                        text = subTitle,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.sharedElement(key = "chat-sub-title-$chatId")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }, navigationIcon = {
            IconButton(
                onClick = navBackStack::removeLastOrNull,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }
        }, actions = {
            topBarActions.forEach { action ->
                var expand by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        expand = true
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(action.icon, null)
                }
                CustomDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                    action.dropdownActions.forEach { action ->
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(action.icon, null)
                            },
                            text = {
                                Text(action.text.asString())
                            },
                            onClick = {
                                action.onClick()
                            })
                    }
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DeleteChatDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    vibrate: () -> Unit,
    deleteForRecipient: Boolean,
    onDeleteForRecipientChanged: (Boolean) -> Unit,
    isPrivateChat: Boolean
) {
    CustomDialog(
        title = stringResource(R.string.delete_chat),
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                Text(text = "Удалить чат без возможности восстановления?", lineHeight = 16.sp)
                if (isPrivateChat) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onDeleteForRecipientChanged(!deleteForRecipient) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = deleteForRecipient,
                            onCheckedChange = onDeleteForRecipientChanged,
                            interactionSource = remember { MutableInteractionSource() })
                        Text(
                            text = stringResource(R.string.delete_for_recipient),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            CountdownTextButton(
                text = stringResource(R.string.delete),
                seconds = 5,
                onClickAfterFinish = onConfirm,
                onClickWhileRunning = vibrate,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            )
        })
}

@Composable
private fun ClearHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    vibrate: () -> Unit,
    clearForRecipient: Boolean,
    onClearForRecipientChanged: (Boolean) -> Unit,
    isPrivateChat: Boolean
) {
    CustomDialog(
        title = stringResource(R.string.clear_history),
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                Text(
                    text = "Удалить все сообщения в чате, без возможности восстановления?",
                    lineHeight = 16.sp
                )
                if (isPrivateChat) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onClearForRecipientChanged(!clearForRecipient) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = clearForRecipient,
                            onCheckedChange = null,
                            interactionSource = remember { MutableInteractionSource() })
                        Text(
                            text = stringResource(R.string.delete_for_recipient),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            CountdownTextButton(
                text = stringResource(R.string.delete),
                seconds = 5,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClickAfterFinish = onConfirm,
                onClickWhileRunning = vibrate
            )
        })
}

@Composable
private fun DeleteMessageDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    deleteForRecipient: Boolean,
    onDeleteForRecipientChanged: (Boolean) -> Unit,
    isPrivateChat: Boolean
) {
    CustomDialog(
        title = stringResource(R.string.delete_message),
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                Text(
                    text = stringResource(R.string.delete_message_description), lineHeight = 16.sp
                )
                if (isPrivateChat) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onDeleteForRecipientChanged(!deleteForRecipient) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = deleteForRecipient,
                            onCheckedChange = null,
                            interactionSource = remember { MutableInteractionSource() })
                        Text(
                            text = stringResource(R.string.delete_for_recipient),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        })
}

@Composable
private fun LeaveDialog(
    onDismiss: () -> Unit, onConfirm: () -> Unit, chatName: String, chatType: ChatType
) {
    val title = when (chatType) {
        ChatType.CHANNEL -> stringResource(R.string.leave_channel)
        ChatType.GROUP -> stringResource(R.string.leave_group)
        else -> stringResource(R.string.leave)
    }
    
    val message = buildAnnotatedString {
        append(stringResource(R.string.leave_channel_confirm_message))
        withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $chatName") }
        append("?")
    }
    
    CustomDialog(title = title, onDismissRequest = onDismiss, content = {
        Text(text = message)
    }, buttons = {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(title)
        }
    })
}

@Composable
private fun InputMessage(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel
) {
    var attachmentModal by remember { mutableStateOf(DialogController()) }
    var micTranslationX by remember { mutableFloatStateOf(0f) }
    var micTranslationY by remember { mutableFloatStateOf(0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "recording_dot_transition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_dot_alpha"
    )
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                attachmentModal.hide()
                chatViewModel.sendFiles(uris)
            }
        })
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            chatViewModel.startRecording()
        }
    }
    
    val animatedAmplitude by animateFloatAsState(
        targetValue = uiState.recordingAmplitude,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "amplitude_animation"
    )
    
    val swipeScale = 1f - (abs(micTranslationX) / 250f).coerceIn(0f, 1f) * 0.5f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraLarge
            ),
        verticalAlignment = Alignment.Bottom
    ) {
        if (uiState.isRecording) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val durationText = String.format(
                    LocalLocale.current.platformLocale,
                    "%02d:%02d",
                    uiState.recordingDurationMs / 1000 / 60,
                    uiState.recordingDurationMs / 1000 % 60
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = dotAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(durationText, style = MaterialTheme.typography.bodyLarge)
                }
                
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    AnimatedContent(
                        targetState = uiState.isRecordingLocked,
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                        },
                        label = "recording_hint_animation",
                        contentAlignment = Alignment.Center
                    ) { isLocked ->
                        if (isLocked) {
                            TextButton(onClick = chatViewModel::cancelRecording) {
                                Text(stringResource(R.string.cancel).uppercase())
                            }
                        } else {
                            val infiniteTransition = rememberInfiniteTransition(label = "shake")
                            
                            val offsetX by infiniteTransition.animateFloat(
                                initialValue = -4f,
                                targetValue = 4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 1000,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "offsetX"
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.graphicsLayer {
                                    translationX = micTranslationX * 0.5f
                                    alpha = (1f - (abs(micTranslationX) / 250f)).coerceIn(0.2f, 1f)
                                }.offset {
                                    IntOffset(x = offsetX.dp.roundToPx(), y = 0)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Влево – отмена",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            BasicTextField(
                value = uiState.messageText,
                onValueChange = chatViewModel::changeText,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 5,
                minLines = 1,
                decorationBox = { innerTextField ->
                    Box {
                        if (uiState.messageText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
        
        if (!uiState.isRecording) {
            IconButton(onClick = attachmentModal::show) {
                Icon(
                    imageVector = Icons.Rounded.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(225f)
                )
            }
        }
        
        if (uiState.messageText.trim().isEmpty() || uiState.isRecording) {
            Box(
                modifier = Modifier
                    .zIndex(if (uiState.isRecording) 10f else 0f)
                    .pointerInput(uiState.isRecordingLocked) {
                        if (uiState.isRecordingLocked) {
                            detectTapGestures(
                                onTap = {
                                    chatViewModel.stopRecordingAndSend()
                                }
                            )
                        } else {
                            awaitPointerEventScope {
                                while (true) {
                                    val downEvent = awaitFirstDown()
                                    downEvent.consume()
                                    
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val releasedBeforeLongPress = withTimeoutOrNull(100L) {
                                            do {
                                                val event = awaitPointerEvent()
                                                event.changes.forEach { it.consume() }
                                            } while (event.changes.any { it.pressed })
                                            true
                                        } ?: false
                                        
                                        if (releasedBeforeLongPress) {
                                            micTranslationX = 0f
                                            micTranslationY = 0f
                                            continue
                                        }
                                        
                                        chatViewModel.startRecording()
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        do {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { it.consume() }
                                        } while (event.changes.any { it.pressed })
                                        continue
                                    }
                                    
                                    var isCanceled = false
                                    var isLocked = false
                                    micTranslationX = 0f
                                    micTranslationY = 0f
                                    val startX = downEvent.position.x
                                    val startY = downEvent.position.y
                                    
                                    var lockedAxis: String? = null
                                    
                                    do {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        val position = event.changes.first().position
                                        val currentX = position.x
                                        val currentY = position.y
                                        val deltaX = currentX - startX
                                        val deltaY = currentY - startY
                                        
                                        if (lockedAxis == null) {
                                            if (deltaX < -20f && abs(deltaX) > abs(deltaY)
                                            ) {
                                                lockedAxis = "X"
                                            } else if (deltaY < -20f && abs(deltaY) > abs(deltaX)) {
                                                lockedAxis = "Y"
                                            }
                                        } else if (abs(deltaX) < 20f && abs(deltaY) < 20f) {
                                            lockedAxis = null
                                        }
                                        
                                        if (deltaY < -250f && !isLocked && !isCanceled) {
                                            chatViewModel.lockRecording()
                                            isLocked = true
                                            micTranslationX = 0f
                                            micTranslationY = 0f
                                        } else if (deltaX < -250f && !isCanceled && !isLocked) {
                                            chatViewModel.cancelRecording()
                                            isCanceled = true
                                            micTranslationX = 0f
                                            micTranslationY = 0f
                                        } else if (!isCanceled && !isLocked) {
                                            micTranslationX =
                                                if (lockedAxis == "X" || lockedAxis == null) deltaX.coerceAtMost(
                                                    0f
                                                ) else 0f
                                            micTranslationY =
                                                if (lockedAxis == "Y" || lockedAxis == null) deltaY.coerceAtMost(
                                                    0f
                                                ) else 0f
                                        }
                                    } while (event.changes.any { it.pressed })
                                    
                                    if (!isCanceled && !isLocked) {
                                        chatViewModel.stopRecordingAndSend()
                                    }
                                    micTranslationX = 0f
                                    micTranslationY = 0f
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isRecording,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier.offset {
                        val y = (-70).dp + if (!uiState.isRecordingLocked) {
                            (micTranslationY * 0.3f).dp
                        } else {
                            0.dp
                        }
                        IntOffset(x = 0, y = y.roundToPx())
                    }
                ) {
                    val isNearLock = uiState.isRecordingLocked || micTranslationY < -150f
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isNearLock) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = micTranslationX
                            scaleX = swipeScale
                            scaleY = swipeScale
                        }
                ) {
                    if (uiState.isRecording) {
                        val maxBackgroundScale = 2.2f
                        val currentScale =
                            1f + ((animatedAmplitude * 2.5f).coerceAtMost(1f) * (maxBackgroundScale - 1f))
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = currentScale
                                    scaleY = currentScale
                                }
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isRecording) MaterialTheme.colorScheme.primary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isRecordingLocked) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = if (uiState.isRecording) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            IconButton(onClick = chatViewModel::onSendMessageClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    if (attachmentModal.isVisible) {
        AttachmentBottomSheet(
            onDismissRequest = attachmentModal::hide,
            onFileSystemClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onFileSelected = { uris ->
                attachmentModal.hide()
                chatViewModel.sendFiles(uris)
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentBottomSheet(
    onDismissRequest: () -> Unit, onFileSystemClick: () -> Unit, onFileSelected: (List<Uri>) -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    
    ModalBottomSheet(
        sheetState = sheetState, onDismissRequest = onDismissRequest, dragHandle = null
    ) {
        Spacer(Modifier.height(10.dp))
        SectionContainer {
            Card(
                onClick = onFileSystemClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            modifier = Modifier.padding(10.dp),
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Column {
                        Text(
                            text = stringResource(R.string.internal_storage),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.file_system_search),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
            
            val d = rememberLauncherForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(
                    10
                )
            ) { uris ->
                if (uris.isNotEmpty()) {
                    onFileSelected(uris)
                }
            }
            Card(
                onClick = {
                    d.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            modifier = Modifier.padding(10.dp),
                            imageVector = Icons.Rounded.Photo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Column {
                        Text(
                            text = stringResource(R.string.gallery),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.to_send_images_without_compression),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteLinkBottomSheet(
    chatId: Long,
    name: String,
    description: String?,
    count: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    val chatType = ChatType.fromId(chatId)
    
    val countText = pluralStringResource(R.plurals.subscribers_count, count, count)
    
    val buttonText = if (chatType == ChatType.CHANNEL) {
        stringResource(R.string.subscribe).uppercase()
    } else {
        stringResource(R.string.join).uppercase()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss, dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = countText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onJoin,
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularWavyProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(
                        text = buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
