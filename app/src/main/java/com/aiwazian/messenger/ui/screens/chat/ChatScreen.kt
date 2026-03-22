/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Attachment
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.FileAction
import com.aiwazian.messenger.ui.screens.main.MainViewModel
import com.aiwazian.messenger.ui.components.navigation.AppRoute

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(
    chatId: Long,
    chatViewModel: ChatViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    val context = LocalContext.current
    
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    
    var fileToCancelId by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(chatId) {
        if (chatId != -1L) {
            chatViewModel.init(chatId)
        }
    }
    
    LaunchedEffect(Unit) {
        chatViewModel.uiEffect.collect { effect ->
            when (effect) {
                is ChatUiEffect.NavigateBack -> navHost.removeLastOrNull()
                is ChatUiEffect.NavigateToMain -> {
                    navHost.clear()
                    navHost.add(AppRoute.Main)
                }
                
                is ChatUiEffect.ScrollToBottom -> {
                    if (effect.index >= 0) {
                        listState.animateScrollToItem(effect.index)
                    }
                }
                
                is ChatUiEffect.NotifyMainMessageSent -> mainViewModel.onSendMessage(effect.message)
                is ChatUiEffect.NotifyMainChatDeleted -> mainViewModel.deleteChat(effect.chatId)
                is ChatUiEffect.NotifyMainNewChat -> mainViewModel.showNewChat(
                    effect.chat,
                    effect.lastMessage
                )
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.close()
        }
    }
    
    Scaffold(
        topBar = {
            TopBar(
                title = if (uiState.isSavedMessages) stringResource(R.string.saved_messages) else uiState.topBarTitle,
                subTitle = getLocalizedSubTitle(uiState),
                dropdownActions = uiState.topBarActions,
                isConnected = uiState.isConnected,
                onBackClicked = chatViewModel::onBackClicked,
                chatId = uiState.chat.id
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                    overscrollEffect = rememberOverscrollEffect()
                ) {
                    items(
                        items = uiState.chatItems,
                        key = { item ->
                            when (item) {
                                is ChatItem.DateSeparator -> "date_${item.text}"
                                is ChatItem.MessageItem -> "msg_${item.message.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ChatItem.DateSeparator -> DateSeparator(item.text)
                            is ChatItem.MessageItem -> MessageBubble(
                                item = item,
                                onSeen = { chatViewModel.markAsReadMessage(item.message) },
                                onFileAction = { file, action ->
                                    if (action == FileAction.CANCEL) {
                                        fileToCancelId = item.message.id
                                    } else {
                                        chatViewModel.onFileAction(item.message, file, action)
                                    }
                                }
                            )
                        }
                    }
                }
                
                BottomSection(
                    uiState = uiState,
                    onTextChanged = chatViewModel::changeText,
                    onSendClicked = chatViewModel::onSendMessageClicked,
                    onJoinClicked = chatViewModel::onJoinClicked,
                    onToggleMuteClicked = chatViewModel::onToggleMuteClicked,
                    onFilesSelected = { uris -> chatViewModel.uploadFiles(uris, context) }
                )
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }
        
        Dialogs(
            uiState = uiState,
            chatViewModel = chatViewModel
        )
        
        if (fileToCancelId != null) {
            CustomDialog(
                title = "Отмена отправки",
                onDismissRequest = { fileToCancelId = null },
                content = { Text("Вы уверены, что хотите отменить отправку файла?") },
                buttons = {
                    TextButton(onClick = { fileToCancelId = null }) { Text("Нет") }
                    TextButton(
                        onClick = {
                            fileToCancelId?.let { 
                                chatViewModel.cancelUpload(it) 
                            }
                            fileToCancelId = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Да, отменить") }
                }
            )
        }
    }
}

@Composable
private fun getLocalizedSubTitle(uiState: ChatUiState): String {
    return when {
        uiState.subscriberCount != null -> {
            "${uiState.subscriberCount} ${stringResource(R.string.subscriberCount).lowercase()}"
        }
        
        uiState.memberCount != null -> {
            "${uiState.memberCount} ${stringResource(R.string.members)}"
        }
        
        else -> uiState.subTitle
    }
}

@Composable
private fun DateSeparator(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.clip(CircleShape)) {
            Text(
                text = text,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BottomSection(
    uiState: ChatUiState,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onJoinClicked: () -> Unit,
    onToggleMuteClicked: () -> Unit,
    onFilesSelected: (List<Uri>) -> Unit
) {
    val chatId = uiState.chat.id
    val chatType = ChatType.fromId(chatId)
    
    when (chatType) {
        ChatType.CHANNEL -> {
            if (uiState.isOwner) {
                // Владелец канала - поле ввода
                InputMessage(
                    value = uiState.messageText,
                    onValueChange = onTextChanged,
                    onSendMessage = onSendClicked,
                    onFilesSelected = onFilesSelected
                )
            } else {
                if (uiState.isJoined) {
                    // Подписан - кнопка Mute/Unmute
                    MuteButton(
                        isMuted = uiState.isMuted,
                        onClick = onToggleMuteClicked
                    )
                } else {
                    // Не подписан - кнопка JOIN
                    JoinButton(onClick = onJoinClicked)
                }
            }
        }
        
        ChatType.GROUP, ChatType.PRIVATE -> {
            // Личный чат или группа - поле ввода
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

@Composable
private fun MuteButton(
    isMuted: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        shape = RoundedCornerShape(0),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        AnimatedContent(
            targetState = isMuted,
            transitionSpec = {
                slideInVertically(tween(200)) { height -> height } + fadeIn(tween(200)) + scaleIn(tween(200)) togetherWith
                        slideOutVertically(tween(200)) { height -> -height } + fadeOut(tween(200)) + scaleOut(tween(200))
            },
            label = "mute_animation"
        ) { isMute ->
            Text(
                text = if (isMute) stringResource(R.string.mute).uppercase() else stringResource(R.string.unmute).uppercase(),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JoinButton(onClick: () -> Unit) {
    TextButton(
        shape = RoundedCornerShape(0),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
    uiState: ChatUiState,
    chatViewModel: ChatViewModel
) {
    if (uiState.showDeleteChatDialog) {
        DeleteChatDialog(
            onDismissRequest = chatViewModel::hideDeleteChatDialog,
            onConfirm = chatViewModel::onDeleteChatConfirmed,
            chatName = uiState.chat.chatName,
            isSelf = uiState.isSavedMessages
        )
    }
    
    if (uiState.showClearHistoryDialog) {
        ClearHistoryDialog(
            onDismissRequest = chatViewModel::hideClearHistoryDialog,
            onConfirm = chatViewModel::onDeleteMessagesConfirmed,
            chatName = uiState.chat.chatName,
            isSelf = uiState.isSavedMessages
        )
    }
    
    if (uiState.showDeleteMessageDialog) {
        DeleteMessageDialog(
            onDismissRequest = chatViewModel::hideDeleteMessageDialog,
            onConfirm = chatViewModel::onDeleteMessageConfirmed,
            chatName = uiState.chat.chatName,
            isSelf = uiState.isSavedMessages
        )
    }
    
    if (uiState.showLeaveDialog) {
        val chatType = ChatType.fromId(uiState.chat.id)
        LeaveDialog(
            onDismiss = chatViewModel::hideLeaveDialog,
            onConfirm = chatViewModel::onLeaveClicked,
            chatName = uiState.chat.chatName,
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
    val navHost = LocalNavHost.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
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
                        scaleX = scale,
                        scaleY = scale
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { navHost.add(AppRoute.Profile(chatId)) }
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            maxLines = 1,
                            fontSize = 18.sp,
                            lineHeight = 16.sp,
                            overflow = TextOverflow.Ellipsis,
                        )
                        
                        AnimatedContent(
                            modifier = Modifier.fillMaxWidth(),
                            targetState = isConnected,
                            transitionSpec = { slideInVertically(tween(200)) togetherWith slideOutVertically(tween(200)) },
                            label = "connection_animation"
                        ) { connected ->
                            Text(
                                text = if (!connected) "${stringResource(R.string.connecting)}..." else subTitle,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = onBackClicked
        ),
        actions = dropdownActions
    )
}

@Composable
private fun DeleteChatDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    chatName: String,
    isSelf: Boolean
) {
    var deleteForReceiver by remember { mutableStateOf(false) }
    
    CustomDialog(
        title = stringResource(R.string.delete_chat),
        onDismissRequest = onDismissRequest,
        content = {
            val suffix = if (!isSelf) " c " + chatName.trimEnd() else ""
            Text(
                text = "Удалить чат$suffix без возможности восстановления?",
                lineHeight = 16.sp
            )
            
            if (!isSelf) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { deleteForReceiver = !deleteForReceiver }
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Checkbox(
                            checked = deleteForReceiver,
                            onCheckedChange = null,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.also_delete_for)} $chatName",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                deleteForReceiver = true
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = { onConfirm(deleteForReceiver) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.delete_chat)) }
        }
    )
}

@Composable
private fun ClearHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    chatName: String,
    isSelf: Boolean
) {
    var deleteForReceiver by remember { mutableStateOf(false) }
    
    CustomDialog(
        title = stringResource(R.string.clear_history),
        onDismissRequest = onDismissRequest,
        content = {
            val suffix = if (!isSelf) " в чате c " + chatName.trimEnd() else ""
            Text(
                text = "Удалить все сообщения$suffix без возможности восстановления?",
                lineHeight = 16.sp
            )
            
            if (!isSelf) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { deleteForReceiver = !deleteForReceiver }
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Checkbox(
                            checked = deleteForReceiver,
                            onCheckedChange = null,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.also_delete_for)} $chatName",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                deleteForReceiver = true
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = { onConfirm(deleteForReceiver) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.delete)) }
        }
    )
}

@Composable
private fun DeleteMessageDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    chatName: String,
    isSelf: Boolean
) {
    var deleteForReceiver by remember { mutableStateOf(false) }
    
    CustomDialog(
        title = stringResource(R.string.delete_message),
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                text = stringResource(R.string.delete_message_description),
                lineHeight = 16.sp
            )
            
            if (!isSelf) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { deleteForReceiver = !deleteForReceiver }
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Checkbox(
                            checked = deleteForReceiver,
                            onCheckedChange = null,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.also_delete_for)} $chatName",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                deleteForReceiver = true
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = { onConfirm(deleteForReceiver) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.delete)) }
        }
    )
}

@Composable
private fun LeaveDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    chatName: String,
    chatType: ChatType
) {
    val title = when (chatType) {
        ChatType.CHANNEL -> stringResource(R.string.leave_channel)
        ChatType.GROUP -> stringResource(R.string.leave_group)
        else -> stringResource(R.string.leave)
    }
    
    val message = when (chatType) {
        ChatType.CHANNEL -> buildAnnotatedString {
            append(stringResource(R.string.leave_channel_confirm))
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $chatName") }
            append("?")
        }
        
        ChatType.GROUP -> buildAnnotatedString {
            append(stringResource(R.string.leave_group_confirm))
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $chatName") }
            append("?")
        }
        
        else -> buildAnnotatedString {
            append(stringResource(R.string.leave_confirm))
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $chatName") }
            append("?")
        }
    }
    
    CustomDialog(
        title = title,
        onDismissRequest = onDismiss,
        content = {
            Text(text = message)
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(title) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputMessage(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFilesSelected: (List<Uri>) -> Unit
) {
    var attachmentModal by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                attachmentModal = false
                onFilesSelected(uris)
            }
        }
    )
    
    TextField(
        shape = RectangleShape,
        value = value,
        onValueChange = onValueChange,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.message)) },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            Row {
                IconButton(onClick = { attachmentModal = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Attachment,
                        contentDescription = null,
                        modifier = Modifier.rotate(135f)
                    )
                }
                IconButton(onClick = onSendMessage) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null
                    )
                }
            }
        }
    )
    
    if (attachmentModal) {
        AttachmentBottomSheet(
            onDismissRequest = { attachmentModal = false },
            onFileSystemClick = { filePickerLauncher.launch(arrayOf("*/*")) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentBottomSheet(
    onDismissRequest: () -> Unit,
    onFileSystemClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column {
            Card(
                onClick = onFileSystemClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0),
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
                            contentDescription = null
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Внутреннее хранилище",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Поиск в файловой системе",
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

@Composable
private fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
    )
}
