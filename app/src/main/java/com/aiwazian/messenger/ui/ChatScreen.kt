package com.aiwazian.messenger.ui

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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.DropdownMenuAction
import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.data.Message
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileType
import com.aiwazian.messenger.services.ClipboardHelper
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.MessageFile
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.utils.Shape
import com.aiwazian.messenger.utils.WebSocketManager
import com.aiwazian.messenger.utils.formatFileSize
import com.aiwazian.messenger.utils.getFileExtension
import com.aiwazian.messenger.utils.isFileExists
import com.aiwazian.messenger.viewModels.ChannelViewModel
import com.aiwazian.messenger.viewModels.ChatViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.time.format.TextStyle as MonthTextStyle

@Composable
fun ChatScreen(chatId: Long) {
    Content(chatId)
}

@Composable
private fun Content(chatId: Long) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val mainViewModel = hiltViewModel<MainViewModel>()
    
    val chatViewModel = hiltViewModel<ChatViewModel>()
    
    val chatInfo by chatViewModel.chatInfo.collectAsState()
    
    val selectedMessages by chatViewModel.selectedMessages.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        chatViewModel.open(chatId)
        chatViewModel.onChatDeleted = {
            navViewModel.goToMain()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.close()
        }
    }
    
    val profile by chatViewModel.profile.collectAsState()
    
    val deleteChatDialog = chatViewModel.deleteChatDialog
    val clearHistoryChatDialog = chatViewModel.clearHistoryDialog
    val deleteMessageDialog = chatViewModel.deleteMessageDialog
    
    val messageText by chatViewModel.messageText.collectAsState()
    
    val messages by chatViewModel.messages.collectAsState()
    
    var subTitle by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    
    var isVisibleLeaveDialog by remember { mutableStateOf(false) }
    
    var actions = listOf<TopBarAction>()
    
    when (ChatType.fromId(chatId)) {
        ChatType.PRIVATE -> {
            subTitle = ""
            
            actions = listOf(
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = listOf(
                        DropdownMenuAction(
                            icon = Icons.Outlined.DeleteOutline,
                            text = stringResource(R.string.delete_chat),
                            onClick = deleteChatDialog::show
                        )
                    )
                )
            )
        }
        
        ChatType.CHANNEL -> {
            if (profile is ChannelInfo) {
                subTitle =
                    "${(profile as ChannelInfo).subscribers} ${stringResource(R.string.subscriberCount).lowercase()}"
            }
            
            if (profile is ChannelInfo && (profile as ChannelInfo).ownerId != chatViewModel.myId) {
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Outlined.MoreVert,
                        dropdownActions = listOf(
                            DropdownMenuAction(
                                icon = Icons.AutoMirrored.Outlined.Logout,
                                text = stringResource(R.string.leave_channel),
                                onClick = {
                                    isVisibleLeaveDialog = true
                                })
                        )
                    )
                )
            }
        }
        
        ChatType.GROUP -> {
            if (profile is GroupInfo) {
                val group = profile as GroupInfo
                subTitle = "${group.members} ${stringResource(R.string.members)}"
                
                if (group.ownerId != chatViewModel.myId) {
                    actions = listOf(
                        TopBarAction(
                            icon = Icons.Outlined.MoreVert,
                            dropdownActions = listOf(
                                DropdownMenuAction(
                                    icon = Icons.AutoMirrored.Outlined.Logout,
                                    text = stringResource(R.string.leave_group),
                                    onClick = {
                                        isVisibleLeaveDialog = true
                                    })
                            )
                        )
                    )
                }
            }
        }
        
        else -> {}
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(index = messages.lastIndex)
        }
    }
    
    Scaffold(
        topBar = {
            TopBar(
                chatInfo,
                subTitle,
                actions
            )
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyChatPlaceholder()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                    overscrollEffect = rememberOverscrollEffect()
                ) {
                    itemsIndexed(
                        items = messages,
                        key = { _, message -> message.id }) { index, message ->
                        val currentMessageSendDate =
                            Instant.ofEpochMilli(message.sendTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        
                        val monthName = currentMessageSendDate.month.getDisplayName(
                            MonthTextStyle.FULL,
                            Locale.getDefault()
                        )
                        
                        val capitalizedMonthName = monthName.replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase(Locale.getDefault())
                            } else {
                                it.toString()
                            }
                        }
                        
                        val showDateSeparator = if (index > 0) {
                            val previousMessageSendDate =
                                Instant.ofEpochMilli(messages[index - 1].sendTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            !currentMessageSendDate.isEqual(previousMessageSendDate)
                        } else {
                            true
                        }
                        
                        if (showDateSeparator) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.clip(CircleShape)
                                ) {
                                    Text(
                                        text = "${currentMessageSendDate.dayOfMonth} $capitalizedMonthName",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        MessageBubble(
                            message = message,
                            onSeen = {
                                if (!message.isRead) {
                                    scope.launch {
                                        chatViewModel.markAsReadMessage(message)
                                    }
                                }
                            })
                    }
                }
            }
            
            if (ChatType.fromId(chatId) == ChatType.CHANNEL && profile is ChannelInfo) {
                if ((profile as ChannelInfo).ownerId == chatViewModel.myId) {
                    InputMessage(
                        value = messageText,
                        onValueChange = chatViewModel::changeText,
                        onSendMessage = {
                            scope.launch {
                                val sentMessage = chatViewModel.sendMessage()
                                
                                if (sentMessage != null) {
                                    mainViewModel.onSendMessage(sentMessage)
                                }
                            }
                        })
                } else {
                    val channelViewModel = hiltViewModel<ChannelViewModel>()
                    
                    var isJoined by remember { mutableStateOf((profile as ChannelInfo).isSubscribed) }
                    
                    if (isJoined) {
                        var isMuted by remember { mutableStateOf(false) }
                        
                        TextButton(
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                isMuted = !isMuted
                            }) {
                            AnimatedContent(
                                targetState = isMuted,
                                transitionSpec = {
                                    slideInVertically(tween(200)) { height -> height } + fadeIn(
                                        tween(200)
                                    ) + scaleIn(
                                        tween(200)
                                    ) togetherWith slideOutVertically(tween(200)) { height -> -height } + fadeOut(
                                        tween(200)
                                    ) + scaleOut(tween(200))
                                }) { isMute ->
                                Text(
                                    text = if (isMute) {
                                        stringResource(R.string.mute).uppercase()
                                    } else {
                                        stringResource(R.string.unmute).uppercase()
                                    },
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .fillMaxWidth(),
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        TextButton(
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    val isJoin = channelViewModel.tryJoin(chatInfo.id)
                                    
                                    isJoined = isJoin
                                    
                                    if (isJoin) {
                                        mainViewModel.showNewChat(
                                            chatInfo,
                                            messages.lastOrNull()
                                        )
                                    }
                                }
                            }) {
                            Text(
                                text = stringResource(R.string.join).uppercase(),
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                InputMessage(
                    value = messageText,
                    onValueChange = chatViewModel::changeText,
                    onSendMessage = {
                        scope.launch {
                            val sentMessage = chatViewModel.sendMessage()
                            
                            if (sentMessage != null) {
                                mainViewModel.onSendMessage(sentMessage)
                            }
                        }
                    })
            }
        }
        
        if (deleteChatDialog.isVisible) {
            DeleteChatDialog(
                onDismissRequest = deleteChatDialog::hide,
                onConfirm = { deleteForReceiver ->
                    scope.launch {
                        val isDeleted = chatViewModel.tryDeleteChat(deleteForReceiver)
                        
                        if (isDeleted) {
                            deleteChatDialog.hide()
                            mainViewModel.deleteChat(chatInfo.id)
                            navViewModel.goToMain()
                        }
                    }
                },
                chatInfo = chatInfo
            )
        }
        
        if (clearHistoryChatDialog.isVisible) {
            DeleteChatDialog(
                onDismissRequest = clearHistoryChatDialog::hide,
                onConfirm = { deleteForReceiver ->
                    scope.launch {
                        val isDeleted = chatViewModel.tryDeleteChatMessages(deleteForReceiver)
                        
                        if (isDeleted) {
                            clearHistoryChatDialog.hide()
                        }
                    }
                },
                chatInfo = chatInfo
            )
        }
        
        if (deleteMessageDialog.isVisible) {
            DeleteMessageDialog(
                onDismissRequest = deleteMessageDialog::hide,
                onConfirm = { deleteForAll ->
                    scope.launch {
                        selectedMessages.forEach { message ->
                            val isDeleted = chatViewModel.tryDeleteMessage(
                                message.id,
                                deleteForAll
                            )
                            
                            if (isDeleted) {
                                chatViewModel.unselectMessage(message)
                                deleteMessageDialog.hide()
                            }
                        }
                    }
                },
                chatInfo = chatInfo
            )
        }
        
        val channelViewModel = hiltViewModel<ChannelViewModel>()
        
        if (isVisibleLeaveDialog) {
            LeaveChannelDialog(
                onConfirm = {
                    scope.launch {
                        val isLeaved = channelViewModel.tryLeave(chatInfo.id)
                        
                        isVisibleLeaveDialog = false
                        
                        if (isLeaved) {
                            mainViewModel.deleteChat(chatInfo.id)
                            navViewModel.goToMain()
                        }
                    }
                },
                onDismiss = {
                    isVisibleLeaveDialog = false
                },
                channelName = chatInfo.chatName
            )
        }
    }
}

@Composable
private fun EmptyChatPlaceholder() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Напишите сообщение или отправьте стикер",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    chat: ChatInfo,
    subTitle: String,
    dropdownActions: List<TopBarAction>
) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val me by UserManager.user.collectAsState()
    
    val isConnected by WebSocketManager.isConnectedState.collectAsState()
    
    val interactionSource = remember { MutableInteractionSource() }
    
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "card_scale_animation"
    )
    
    PageTopBar(
        title = {
            Card(
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            navViewModel.addScreenInStack {
                                ProfileScreen(chat.id)
                            }
                        }),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val chatName = if (chat.id == me.id) {
                            stringResource(R.string.saved_messages)
                        } else {
                            chat.chatName
                        }
                        
                        Text(
                            text = chatName,
                            maxLines = 1,
                            fontSize = 18.sp,
                            lineHeight = 16.sp,
                            overflow = TextOverflow.Ellipsis,
                        )
                        
                        AnimatedContent(
                            targetState = isConnected,
                            transitionSpec = { slideInVertically(tween(200)) togetherWith slideOutVertically(tween(200)) }) { isConnected ->
                            if (!isConnected) {
                                Text(
                                    text = "${stringResource(R.string.connecting)}...",
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = subTitle,
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        ),
        actions = dropdownActions
    )
}

@Composable
private fun DeleteChatDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    chatInfo: ChatInfo
) {
    var deleteForReceiver by remember { mutableStateOf(false) }
    
    CustomDialog(
        title = stringResource(R.string.delete_chat),
        onDismissRequest = onDismissRequest,
        content = {
            val me by UserManager.user.collectAsState()
            
            val chatName = if (chatInfo.id != me.id) " c " + chatInfo.chatName.trimEnd() else ""
            
            Text(
                text = "Удалить чат$chatName без возможности восстановления?",
                lineHeight = 16.sp
            )
            
            if (chatInfo.id != me.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            deleteForReceiver = !deleteForReceiver
                        }) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Checkbox(
                            modifier = Modifier.padding(end = 10.dp),
                            checked = deleteForReceiver,
                            onCheckedChange = null
                        )
                        Text(
                            text = "Также удалить для ${chatInfo.chatName}",
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
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            
            TextButton(
                onClick = {
                    onConfirm(deleteForReceiver)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_chat))
            }
        })
}

@Composable
private fun DeleteMessageDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    chatInfo: ChatInfo
) {
    var deleteForReceiver by remember { mutableStateOf(false) }
    
    CustomDialog(
        title = stringResource(R.string.delete_message),
        onDismissRequest = onDismissRequest,
        content = {
            val me by UserManager.user.collectAsState()
            
            Text(
                text = stringResource(R.string.delete_message_description),
                lineHeight = 16.sp
            )
            
            if (chatInfo.id != me.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            deleteForReceiver = !deleteForReceiver
                        }) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Checkbox(
                            modifier = Modifier.padding(end = 10.dp),
                            checked = deleteForReceiver,
                            onCheckedChange = null
                        )
                        Text(
                            text = "${stringResource(R.string.also_delete_for)} ${chatInfo.chatName}",
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
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            
            TextButton(
                onClick = {
                    onConfirm(deleteForReceiver)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        })
}

@Composable
private fun LeaveChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    channelName: String
) {
    CustomDialog(
        title = stringResource(R.string.leave_channel),
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.leave_channel))
            }
        }) {
        Text(text = buildAnnotatedString {
            append(stringResource(R.string.leave_channel_confirm))
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) {
                append(
                    " $channelName"
                )
            }
            
            append("?")
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputMessage(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    attachFile: () -> Unit = { },
) {
    var attachmentModal by remember { mutableStateOf(false) }
    
    TextField(
        shape = RectangleShape,
        value = value,
        onValueChange = onValueChange,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(stringResource(R.string.message))
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            Row {
                IconButton(onClick = { attachmentModal = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Attachment,
                        contentDescription = null,
                        modifier = Modifier.rotate(135f)
                    )
                }
                IconButton(onClick = onSendMessage) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                    )
                }
            }
        })
    
    val viewModel = hiltViewModel<ChatViewModel>()
    
    val context = LocalContext.current
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.sendDocument(
                    context,
                    uris
                )
                
                attachmentModal = false
            }
        })
    
    if (attachmentModal) {
        ModalBottomSheet(onDismissRequest = { attachmentModal = false }) {
            Column {
                Card(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
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
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                modifier = Modifier.padding(10.dp),
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Внутреннее хранилище",
                                color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
private fun MessageBubble(
    message: Message,
    onSeen: (Int) -> Unit
) {
    val me by UserManager.user.collectAsState()
    
    val formatter = SimpleDateFormat(
        "HH:mm",
        Locale.getDefault()
    )
    val sendMessageTime = formatter.format(message.sendTime)
    
    var expanded by remember { mutableStateOf(false) }
    
    val alignment = if (message.senderId == me.id) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            onSeen(message.id)
        }
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInParent()
                
                val isElementVisible =
                    position.y >= 0 && position.y < coordinates.parentLayoutCoordinates!!.size.height
                if (isElementVisible) {
                    isVisible = true
                }
            }) {
        
        val dropdownActions = remember { mutableStateListOf<DropdownMenuAction>() }
        
        MessageText(
            message = message,
            time = sendMessageTime,
            isRead = if (me.id != message.senderId) null else message.isRead,
            alignment = alignment,
            onClick = {
                dropdownActions.clear()
                dropdownActions.addAll(it)
                expanded = true
            })
        
        DropdownMenu(
            shape = Shape.DropdownMenu,
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(
                LocalConfiguration.current.screenWidthDp.dp / 2,
                0.dp
            ),
            properties = PopupProperties(focusable = true),
        ) {
            dropdownActions.forEach { action ->
                DropdownMenuItem(
                    leadingIcon = {
                        Row(horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    text = {
                        Text(action.text)
                    },
                    onClick = {
                        expanded = false
                        action.onClick.invoke()
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MessageText(
    message: Message,
    time: String,
    isRead: Boolean?,
    alignment: Alignment,
    onClick: (List<DropdownMenuAction>) -> Unit
) {
    val chatViewModel = hiltViewModel<ChatViewModel>()
    val me by UserManager.user.collectAsState()
    val text = message.text
    val context = LocalContext.current
    
    val actions = remember { mutableStateListOf<DropdownMenuAction>() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 1.dp
            )
            .combinedClickable(
                onClick = { onClick.invoke(actions) },
                onLongClick = { },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }),
        contentAlignment = alignment
    ) {
        if (!text.isNullOrBlank() && isSingleEmoji(text)) {
            actions.clear()
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Outlined.ContentCopy,
                    text = stringResource(R.string.copy),
                    onClick = { ClipboardHelper(context).copy(text) })
            )
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Outlined.Delete,
                    text = stringResource(R.string.delete),
                    onClick = {
                        chatViewModel.deleteMessageDialog.show()
                        chatViewModel.selectMessage(message)
                    })
            )
            
            Box(
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = text,
                    fontSize = 64.sp,
                    lineHeight = 72.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .background(
                            color = Color(0x66646464),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = time,
                        style = TextStyle(
                            textAlign = TextAlign.Center
                        ),
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp,
                                horizontal = 6.dp
                            )
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        } else if (message.attachments.isNotEmpty()) {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            
            Box(
                modifier = Modifier
                    .background(
                        color = if (alignment == Alignment.CenterEnd) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else Color(
                            0x66646464
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    val downloads by chatViewModel.downloads.collectAsState()
                    
                    message.attachments.forEach { attachment ->
                        actions.clear()
                        actions.add(
                            DropdownMenuAction(
                                icon = Icons.Outlined.Delete,
                                text = stringResource(R.string.delete_message),
                                onClick = {
                                    chatViewModel.deleteMessageDialog.show()
                                    chatViewModel.selectMessage(message)
                                })
                        )
                        
                        val item = downloads.find { it.url == attachment.url }
                        
                        val fileSize = formatFileSize(attachment.size)
                        val fileExtension = getFileExtension(attachment.name)
                        val filePath = attachment.id.toString() + '.' + fileExtension
                        val isExists = isFileExists(
                            context,
                            filePath
                        )
                        
                        if (isExists) {
                            actions.add(
                                0,
                                DropdownMenuAction(
                                    icon = Icons.Filled.Download,
                                    text = stringResource(R.string.save_to_downloads),
                                    onClick = {
                                        scope.launch {
                                            chatViewModel.saveFile(
                                                context,
                                                attachment
                                            )
                                        }
                                    })
                            )
                        }
                        
                        MessageFile(
                            fileName = attachment.name,
                            fileInfo = "$fileSize ${fileExtension.uppercase()}",
                            onClick = {
                                if (isExists) {
                                    chatViewModel.openFile(
                                        context,
                                        filePath
                                    )
                                } else {
                                    scope.launch {
                                        if (item == null) {
                                            chatViewModel.addDownload(
                                                context,
                                                attachment
                                            )
                                        } else if (item.status == DownloadStatus.DOWNLOADING) {
                                            chatViewModel.cancelDownload(item)
                                        } else if (item.status == DownloadStatus.COMPLETED) {
                                            chatViewModel.openFile(
                                                context,
                                                filePath
                                            )
                                        }
                                    }
                                }
                            },
                            fileType = FileType.fromExtension(fileExtension),
                            downloadStatus = if (isExists) {
                                DownloadStatus.COMPLETED
                            } else {
                                item?.status ?: DownloadStatus.PENDING
                            },
                            progress = item?.progress ?: 0
                        )
                    }
                }
            }
        } else if (!text.isNullOrBlank()) {
            actions.clear()
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Outlined.ContentCopy,
                    text = stringResource(R.string.copy),
                    onClick = { ClipboardHelper(context).copy(text) })
            )
            actions.add(
                DropdownMenuAction(
                    icon = Icons.Outlined.Delete,
                    text = stringResource(R.string.delete_message),
                    onClick = {
                        chatViewModel.deleteMessageDialog.show()
                        chatViewModel.selectMessage(message)
                    })
            )
            
            Box(
                modifier = Modifier
                    .background(
                        color = if (alignment == Alignment.CenterEnd) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else Color(
                            0x66646464
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .widthIn(max = 280.dp)
                    .padding(
                        top = 6.dp
                    )
            ) {
                Column {
                    if (ChatType.fromId(message.chatId) == ChatType.GROUP && message.senderId != me.id) {
                        val chatViewModel = hiltViewModel<ChatViewModel>()
                        
                        LaunchedEffect(Unit) {
                            chatViewModel.loadUserName(message.senderId)
                        }
                        
                        val userNamesCache by chatViewModel.userNamesCache.collectAsState()
                        
                        val senderName = userNamesCache[message.senderId]
                        
                        senderName?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.W500,
                                lineHeight = 10.sp,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp
                                )
                            )
                        }
                    }
                    
                    val annotatedString = buildAnnotatedString {
                        val parts = text.split(" ")
                        
                        for (part in parts) {
                            if (part.startsWith("@")) {
                                pushStringAnnotation(
                                    tag = "user",
                                    annotation = part
                                )
                                
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(part)
                                }
                                
                                pop()
                            } else {
                                append(part)
                            }
                            
                            append(" ")
                        }
                    }
                    
                    Text(
                        text = annotatedString,
                        modifier = Modifier
                            .padding(
                                start = 8.dp,
                                end = 40.dp,
                                bottom = 6.dp
                            )
                            .clip(shape = RoundedCornerShape(4.dp)),
                        lineHeight = 18.sp
                    )
                }
                
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = time,
                        style = TextStyle(
                            textAlign = TextAlign.Center
                        ),
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(
                            end = if (isRead == null) 8.dp else 0.dp
                        )
                    )
                    
                    if (isRead != null) {
                        Box(
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                null,
                                Modifier.size(16.dp)
                            )
                            
                            if (isRead) {
                                Icon(
                                    Icons.Outlined.Check,
                                    null,
                                    Modifier
                                        .padding(end = 6.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isSingleEmoji(text: String): Boolean {
    val emojiRegex =
        Regex("^[\\p{So}\\p{Cntrl}\\p{InEmoticons}\\p{InMiscellaneousSymbolsAndPictographs}\\p{InSupplementalSymbolsAndPictographs}\\uD83C\\uDFF0-\\uD83D\\uDFFF]+$")
    
    return emojiRegex.matches(text.trim())
}
