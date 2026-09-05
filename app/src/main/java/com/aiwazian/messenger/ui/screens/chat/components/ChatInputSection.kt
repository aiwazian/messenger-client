package com.aiwazian.messenger.ui.screens.chat.components

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.screens.chat.ChatStickersViewModel
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import com.aiwazian.messenger.ui.screens.chat.ChatViewModel
import com.aiwazian.messenger.ui.screens.chat.MediaPickerViewModel
import com.aiwazian.messenger.utils.DialogController
import kotlin.math.abs

@Composable
fun ChatInputSection(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp)
    ) {
        when (ChatType.fromId(uiState.chatId)) {
            ChatType.CHANNEL -> {
                AnimatedContent(
                    targetState = when {
                        uiState.isOwner -> "input"
                        !uiState.isJoined -> "join"
                        else -> "none"
                    }, modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) { state ->
                    when (state) {
                        "input" -> InputMessage(uiState = uiState, chatViewModel = chatViewModel)
                        "join" -> JoinButton(onClick = chatViewModel::onJoinClicked)
                        "none" -> Unit
                    }
                }
            }
            
            ChatType.GROUP -> {
                AnimatedContent(
                    targetState = uiState.isOwner || uiState.isJoined, transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                    }, modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) { showInputField ->
                    if (showInputField) {
                        InputMessage(uiState = uiState, chatViewModel = chatViewModel)
                    } else {
                        JoinButton(onClick = chatViewModel::onJoinClicked)
                    }
                }
            }
            
            ChatType.PRIVATE -> {
                AnimatedContent(
                    targetState = when {
                        uiState.isBlockedByThem -> "blocked"
                        uiState.isBlocked -> "unblock"
                        else -> "input"
                    },
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { state ->
                    when (state) {
                        "blocked" -> {
                            Text(
                                text = "Отправка сообщений ограничена",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        "unblock" -> {
                            Text(
                                text = buildAnnotatedString {
                                    append(stringResource(R.string.user_blocked))
                                    append(". ")
                                    withLink(
                                        LinkAnnotation.Clickable(
                                            tag = "unblock", styles = TextLinkStyles(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textDecoration = TextDecoration.None
                                                ), pressedStyle = SpanStyle(
                                                    background = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.4f
                                                    )
                                                )
                                            ), linkInteractionListener = {
                                                chatViewModel.showBlockDialog()
                                            })
                                    ) {
                                        append(stringResource(R.string.unblock))
                                    }
                                    append("?")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        "input" -> {
                            InputMessage(uiState = uiState, chatViewModel = chatViewModel)
                        }
                    }
                }
            }
            
            else -> {}
        }
    }
}

@Composable
private fun JoinButton(onClick: () -> Unit) {
    TextButton(
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Text(
            text = stringResource(R.string.join).uppercase(),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InputMessage(
    uiState: ChatUiState, chatViewModel: ChatViewModel
) {
    var attachmentModal by remember { mutableStateOf(DialogController()) }
    var micTranslationX by remember { mutableFloatStateOf(0f) }
    var micTranslationY by remember { mutableFloatStateOf(0f) }
    
    val focusRequester = remember { FocusRequester() }
    
    val mediaPickerViewModel: MediaPickerViewModel = hiltViewModel()
    
    val stickersViewModel: ChatStickersViewModel = hiltViewModel()
    val stickersState by stickersViewModel.uiState.collectAsState()
    
    val textFieldState = rememberTextFieldState(initialText = uiState.messageText)
    
    LaunchedEffect(textFieldState, chatViewModel) {
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            chatViewModel.changeText(text)
        }
    }
    
    LaunchedEffect(uiState.messageText) {
        if (uiState.messageText != textFieldState.text.toString()) {
            textFieldState.setTextAndPlaceCursorAtEnd(uiState.messageText)
        }
    }
    
    LaunchedEffect(uiState.editingMessageId) {
        if (uiState.editingMessageId == null) return@LaunchedEffect
        
        textFieldState.setTextAndPlaceCursorAtEnd(uiState.messageText)
        focusRequester.requestFocus()
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                attachmentModal.hide()
                
                mediaPickerViewModel.sendUris(
                    chatId = uiState.chatId,
                    uris = uris,
                    caption = uiState.messageText,
                    replyTo = uiState.replyToMessage
                )
                
                chatViewModel.changeText("")
                chatViewModel.cancelReply()
            }
        })
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            chatViewModel.startRecording()
        } else {
            chatViewModel.onMicrophonePermissionDenied()
        }
    }
    
    val animatedAmplitude by animateFloatAsState(
        targetValue = uiState.recordingAmplitude,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "amplitude_animation"
    )
    
    val swipeScale = 1f - (abs(micTranslationX) / 250f).coerceIn(0f, 1f) * 0.5f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) {}
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(24.dp)
            )) {
        AnimatedVisibility(
            visible = uiState.editingMessageId != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.edit_message),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = chatViewModel::cancelEditing) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = uiState.replyToMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
        ) {
            val preview = uiState.replyToMessage
            
            if (preview != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = chatViewModel::onReplyPanelClicked)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reply_to, preview.title.orEmpty()),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = replyPreviewText(preview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    IconButton(onClick = chatViewModel::cancelReply) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedVisibility(
                visible = !uiState.isRecording,
                enter = expressiveScaleIn,
                exit = expressiveScaleOut
            ) {
                IconButton(onClick = stickersViewModel::togglePanel) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEmotions,
                        contentDescription = null,
                        tint = if (stickersState.isPanelVisible) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val textFieldAlpha by animateFloatAsState(
                        targetValue = if (uiState.isRecording) 0f else 1f,
                        animationSpec = tween(200)
                    )
                    
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(textFieldAlpha)
                            .focusRequester(focusRequester)
                            .contentReceiver { content ->
                                if (content.hasMediaType(MediaType.Image)) {
                                    content.consume { item ->
                                        val uri = item.uri
                                        
                                        if (uri != null) {
                                            chatViewModel.sendFiles(listOf(uri))
                                        }
                                        
                                        uri != null
                                    }
                                } else {
                                    content
                                }
                            },
                        textStyle = TextStyle.Default.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp,
                            fontSize = 16.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 1, maxHeightInLines = 5
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorator = TextFieldDecorator { innerTextField ->
                            Box(
                                modifier = Modifier.padding(
                                    top = 12.dp, bottom = 12.dp
                                )
                            ) {
                                if (textFieldState.text.isEmpty() && !uiState.isRecording) {
                                    Text(
                                        text = stringResource(R.string.message),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        })
                    
                    VoiceRecordingStatus(
                        uiState = uiState,
                        micTranslationX = micTranslationX,
                        onCancelRecording = chatViewModel::cancelRecording
                    )
                }
            }
            
            AnimatedVisibility(
                visible = !uiState.isRecording && uiState.editingMessageId == null,
                enter = expressiveScaleIn,
                exit = expressiveScaleOut
            ) {
                IconButton(onClick = attachmentModal::show) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(225f)
                    )
                }
            }
            
            AnimatedContent(
                targetState = uiState.messageText.trim().isEmpty() || uiState.isRecording,
                transitionSpec = {
                    expressiveScaleIn togetherWith expressiveScaleOut
                }
            ) { showMic ->
                if (showMic) {
                    Box(
                        modifier = Modifier
                            .zIndex(if (uiState.isRecording) 10f else 0f)
                            .pointerInput(uiState.isRecordingLocked) {
                                if (uiState.isRecordingLocked) {
                                    detectTapGestures(
                                        onTap = {
                                            chatViewModel.stopRecordingAndSend()
                                        })
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
                                                val releasedBeforeLongPress =
                                                    withTimeoutOrNull(200L) {
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
                                                    if (deltaX < -20f && abs(deltaX) > abs(
                                                            deltaY
                                                        )
                                                    ) {
                                                        lockedAxis = "X"
                                                    } else if (deltaY < -20f && abs(deltaY) > abs(
                                                            deltaX
                                                        )
                                                    ) {
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
                            }, contentAlignment = Alignment.Center
                    ) {
                        VoiceRecordingLockedIcon(uiState, micTranslationY)
                        
                        Box(
                            modifier = Modifier.graphicsLayer {
                                translationX = micTranslationX
                                scaleX = swipeScale
                                scaleY = swipeScale
                            }) {
                            if (uiState.isRecording) {
                                VoiceRecordingAmplitudeEffect(animatedAmplitude)
                            }
                            
                            val micBackColor by animateColorAsState(
                                targetValue = if (uiState.isRecording) MaterialTheme.colorScheme.primary else Color.Transparent,
                                animationSpec = tween(
                                    durationMillis = 200, easing = FastOutSlowInEasing
                                )
                            )
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(micBackColor), contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = uiState.isRecordingLocked,
                                    transitionSpec = { scaleIn() togetherWith scaleOut() }) { isLocked ->
                                    Icon(
                                        imageVector = if (isLocked) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                                        contentDescription = null,
                                        tint = if (uiState.isRecording) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    IconButton(onClick = chatViewModel::onSendMessageClicked) {
                        AnimatedContent(
                            targetState = uiState.editingMessageId != null,
                            transitionSpec = { expressiveScaleIn togetherWith expressiveScaleOut }) { isEditing ->
                            if (isEditing) {
                                Icon(
                                    imageVector = Icons.Rounded.Done,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(visible = stickersState.isPanelVisible && !uiState.isRecording) {
            StickerInputPanel(
                packs = stickersState.addedPacks,
                onStickerClick = { sticker ->
                    stickersViewModel.sendSticker(uiState.chatId, sticker.id)
                })
        }
    }
    
    if (attachmentModal.isVisible) {
        MediaPickerBottomSheet(
            chatId = uiState.chatId,
            replyTo = uiState.replyToMessage,
            caption = uiState.messageText,
            onCaptionChange = chatViewModel::changeText,
            onDismissRequest = attachmentModal::hide,
            onFileSystemClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onSent = {
                attachmentModal.hide()
                chatViewModel.changeText("")
                chatViewModel.cancelReply()
            })
    }
}

@Composable
private fun VoiceRecordingStatus(
    uiState: ChatUiState, micTranslationX: Float, onCancelRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_dot_transition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(800), repeatMode = RepeatMode.Reverse
        ), label = "recording_dot_alpha"
    )
    
    androidx.compose.animation.AnimatedVisibility(
        visible = uiState.isRecording,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
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
            
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = uiState.isRecordingLocked, transitionSpec = {
                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    }, label = "recording_hint_animation", contentAlignment = Alignment.Center
                ) { isLocked ->
                    if (isLocked) {
                        TextButton(onClick = onCancelRecording) {
                            Text(stringResource(R.string.cancel).uppercase())
                        }
                    } else {
                        val infiniteTransition = rememberInfiniteTransition(label = "shake")
                        
                        val offsetX by infiniteTransition.animateFloat(
                            initialValue = -4f,
                            targetValue = 4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 1000, easing = LinearEasing
                                ), repeatMode = RepeatMode.Reverse
                            ),
                            label = "offsetX"
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = micTranslationX * 0.5f
                                    alpha = (1f - (abs(micTranslationX) / 250f)).coerceIn(
                                        0.2f, 1f
                                    )
                                }
                                .offset {
                                    IntOffset(x = offsetX.dp.roundToPx(), y = 0)
                                }
                                .padding(horizontal = 2.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.swipe_to_cancel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRecordingLockedIcon(
    uiState: ChatUiState, micTranslationY: Float
) {
    val micIconPosition by animateFloatAsState(
        targetValue = -70f + if (!uiState.isRecordingLocked) {
            micTranslationY * 0.3f
        } else {
            0f
        }
    )
    
    androidx.compose.animation.AnimatedVisibility(
        visible = uiState.isRecording,
        enter = fadeIn() + scaleIn() + slideInVertically { it / 2 },
        exit = fadeOut() + scaleOut() + slideOutVertically { it / 2 },
        modifier = Modifier.offset {
            IntOffset(
                x = 0, y = micIconPosition.dp.roundToPx()
            )
        }) {
        val isNearLock = uiState.isRecordingLocked || micTranslationY < -150f
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp)
        ) {
            AnimatedContent(
                targetState = isNearLock,
                transitionSpec = { fadeIn() togetherWith fadeOut() }) { isNearLock ->
                Icon(
                    imageVector = if (isNearLock) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceRecordingAmplitudeEffect(amplitude: Float) {
    val maxBackgroundScale = 2.2f
    val currentScale = 1f + ((amplitude * 2.5f).coerceAtMost(1f) * (maxBackgroundScale - 1f))
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
}
