/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Attachment
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.ui.components.AnimatedDotsText
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.SystemMessageBubble
import com.aiwazian.messenger.ui.screens.profile.Profile
import com.aiwazian.messenger.utils.DialogController
import java.util.Locale

@Composable
fun ChatScreen(
    chatId: Long, chatName: String? = null, chatViewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        chatViewModel.init(chatId, chatName)
    }
    
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val firstVisibleItemIndex =
        remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    LaunchedEffect(firstVisibleItemIndex.value) {
        if (firstVisibleItemIndex.value < 10 && uiState.hasMoreMessages && !uiState.isLoadingMore && !uiState.isLoading) {
            chatViewModel.loadMoreMessages()
        }
    }
    
    var fileToCancelId by remember { mutableStateOf<Int?>(null) }
    
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
                    snackbarHostState.showSnackbar(
                        message = effect.message, duration = SnackbarDuration.Short
                    )
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
            SnackbarHost(snackbarHostState) {
                CustomSnackbar(
                    text = it.visuals.message, onDismiss = it::dismiss
                )
            }
        },
        topBar = {
            TopBar(
                title = uiState.chatName.asString(),
                subTitle = uiState.subTitle.asString(),
                dropdownActions = uiState.topBarActions,
                isConnected = uiState.isConnected,
                onBackClicked = chatViewModel::onBackClicked,
                chatId = uiState.chatId
            )
        },
        bottomBar = {
            BottomSection(
                uiState = uiState,
                onTextChanged = chatViewModel::changeText,
                onSendClicked = chatViewModel::onSendMessageClicked,
                onJoinClicked = chatViewModel::onJoinClicked,
                onFilesSelected = { uris ->
                    chatViewModel.uploadFiles(uris)
                })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    overscrollEffect = rememberOverscrollEffect()
                ) {
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
                                item = item, onSeen = {
                                    chatViewModel.markAsReadMessage(item.message)
                                }, onFileAction = { file, action ->
                                    if (action == FileAction.CANCEL) {
                                        fileToCancelId = item.message.id
                                    } else {
                                        chatViewModel.onFileAction(item.message, file, action)
                                    }
                                }, onLinkClicked = chatViewModel::onLinkClicked
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
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
        }
        
        Dialogs(
            uiState = uiState, chatViewModel = chatViewModel
        )
        
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
}

@Composable
private fun BottomSection(
    uiState: ChatUiState,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onJoinClicked: () -> Unit,
    onFilesSelected: (List<Uri>) -> Unit
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        when (uiState.profile) {
            is Profile.Channel -> {
                if (uiState.profile.ownerId == uiState.currentUserId) {
                    InputMessage(
                        value = uiState.messageText,
                        onValueChange = onTextChanged,
                        onSendMessage = onSendClicked,
                        onFilesSelected = onFilesSelected
                    )
                } else {
                    if (!uiState.isJoined) {
                        JoinButton(onClick = onJoinClicked)
                    }
                }
            }
            
            is Profile.Group -> {
                if (uiState.profile.ownerId == uiState.currentUserId) {
                    InputMessage(
                        value = uiState.messageText,
                        onValueChange = onTextChanged,
                        onSendMessage = onSendClicked,
                        onFilesSelected = onFilesSelected
                    )
                } else {
                    if (uiState.isJoined) {
                        InputMessage(
                            value = uiState.messageText,
                            onValueChange = onTextChanged,
                            onSendMessage = onSendClicked,
                            onFilesSelected = onFilesSelected
                        )
                    } else {
                        JoinButton(onClick = onJoinClicked)
                    }
                }
            }
            
            is Profile.User -> {
                InputMessage(
                    value = uiState.messageText,
                    onValueChange = onTextChanged,
                    onSendMessage = onSendClicked,
                    onFilesSelected = onFilesSelected
                )
            }
            
            else -> {}
        }
    }
}

@Composable
private fun JoinButton(onClick: () -> Unit) {
    TextButton(
        shape = RoundedCornerShape(0), modifier = Modifier.fillMaxWidth(), onClick = onClick
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
        DeleteChatDialog(
            onDismissRequest = chatViewModel::hideDeleteChatDialog,
            onConfirm = chatViewModel::onDeleteChatConfirmed
        )
    }
    
    if (uiState.showClearHistoryDialog) {
        ClearHistoryDialog(
            onDismissRequest = chatViewModel::hideClearHistoryDialog,
            onConfirm = chatViewModel::onDeleteMessagesConfirmed
        )
    }
    
    if (uiState.showDeleteMessageDialog) {
        DeleteMessageDialog(
            onDismissRequest = chatViewModel::hideDeleteMessageDialog,
            onConfirm = chatViewModel::onDeleteMessageConfirmed
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
    subTitle: String,
    dropdownActions: List<TopBarAction>,
    isConnected: Boolean,
    onBackClicked: () -> Unit,
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
    
    PageTopBar(
        title = {
            Card(
                shape = RoundedCornerShape(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { navBackStack.add(AppRoute.Profile(chatId)) }),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle, contentDescription = null,
                        modifier = Modifier.sharedElement(key = "avatar-$chatId")
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            maxLines = 1,
                            fontSize = 18.sp,
                            lineHeight = 16.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .sharedElement(key = "chat-name-$chatId")
                        )
                        
                        AnimatedContent(
                            modifier = Modifier.fillMaxWidth(),
                            targetState = isConnected,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                            },
                            label = "connection_animation"
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sharedElement(key = "chat-sub-title-$chatId")
                                )
                            }
                        }
                    }
                }
            }
        }, navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBackClicked
        ), actions = dropdownActions
    )
}

@Composable
private fun DeleteChatDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    CustomDialog(
        title = stringResource(R.string.delete_chat),
        onDismissRequest = onDismissRequest,
        content = {
            Text(text = "Удалить чат без возможности восстановления?", lineHeight = 16.sp)
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        })
}

@Composable
private fun ClearHistoryDialog(
    onDismissRequest: () -> Unit, onConfirm: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.clear_history),
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                text = "Удалить все сообщения в чате, без возможности восстановления?",
                lineHeight = 16.sp
            )
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.delete)) }
        })
}

@Composable
private fun DeleteMessageDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    CustomDialog(
        title = stringResource(R.string.delete_message),
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                text = stringResource(R.string.delete_message_description), lineHeight = 16.sp
            )
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
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFilesSelected: (List<Uri>) -> Unit
) {
    var attachmentModal by remember { mutableStateOf(DialogController()) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                attachmentModal.hide()
                onFilesSelected(uris)
            }
        })
    
    TextField(
        shape = RectangleShape,
        value = value,
        onValueChange = onValueChange,
        maxLines = 5,
        textStyle = TextStyle(lineHeight = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.message)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            Row {
                IconButton(onClick = attachmentModal::show) {
                    Icon(
                        imageVector = Icons.Rounded.Attachment,
                        contentDescription = null,
                        modifier = Modifier.rotate(135f)
                    )
                }
                IconButton(onClick = onSendMessage) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        })
    
    if (attachmentModal.isVisible) {
        AttachmentBottomSheet(
            onDismissRequest = attachmentModal::hide,
            onFileSystemClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onFileSelected = { uris ->
                attachmentModal.hide()
                onFilesSelected(uris)
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentBottomSheet(
    onDismissRequest: () -> Unit, onFileSystemClick: () -> Unit, onFileSelected: (List<Uri>) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val sheetState = rememberModalBottomSheetState()
    
    LaunchedEffect(selectedIndex) {
        pagerState.animateScrollToPage(selectedIndex)
    }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }
    
    ModalBottomSheet(
        sheetState = sheetState, onDismissRequest = onDismissRequest, dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 60.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (page) {
                    0 -> {
                        Column(Modifier.fillMaxSize()) {
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
                    
                    1 -> {
                        Column {
                            SectionContainer {
                                FramelessTextBox(
                                    placeholder = stringResource(R.string.search),
                                    value = "",
                                    onValueChange = {})
                            }
                            
                            SectionContainer {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                ) {
                                    items(30) {
                                        SectionItem(headlineText = "ds")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            PrimaryTabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier
                    .navigationBarsPadding()
                    .offset { IntOffset(x = 0, y = -sheetState.requireOffset().toInt()) }
                    .padding(10.dp)
                    .clip(CircleShape),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                indicator = {},
                divider = {},
            ) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape),
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = selectedIndex,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }) { index ->
                            if (index == 0) {
                                Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, null)
                            } else {
                                Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null)
                            }
                        }
                        Text(
                            stringResource(R.string.files),
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
                Tab(
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    modifier = Modifier
                        .padding(4.dp)
                        .padding(start = 0.dp)
                        .clip(CircleShape),
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            selectedIndex,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }) { index ->
                            if (index == 1) {
                                Icon(Icons.Rounded.PlayCircleFilled, null)
                            } else {
                                Icon(Icons.Rounded.PlayCircleOutline, null)
                            }
                        }
                        Text(
                            stringResource(R.string.music),
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
private fun InviteLinkBottomSheet(
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
