package com.aiwazian.messenger.ui.screens.chat.components

import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppPrimaryScrollableTabRow
import com.aiwazian.messenger.ui.app.AppTab
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.CustomEmojiViewModel
import com.aiwazian.messenger.ui.components.appendCustomEmojiText
import com.aiwazian.messenger.ui.components.rememberCustomEmojiInlineContent
import com.aiwazian.messenger.ui.screens.chat.ChatEmojiViewModel
import com.aiwazian.messenger.ui.screens.chat.ChatStickersViewModel
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import com.aiwazian.messenger.utils.DialogController
import com.aiwazian.messenger.utils.MessageFormatAction
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun ChatInputSection(
    uiState: ChatUiState,
    actions: ChatInputActions,
    modifier: Modifier = Modifier,
    alwaysShowInput: Boolean = false,
    isVoiceEnabled: Boolean = true,
    sendOptionsEnabled: Boolean = true,
    onMediaSheetSend: ((silent: Boolean, scheduledAt: Long?) -> Unit)? = null,
    onContactSend: ((MessageContact) -> Unit)? = null,
    sendIcon: ImageVector = Icons.AutoMirrored.Rounded.Send
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    val stickersViewModel: ChatStickersViewModel = hiltViewModel()
    val stickersState by stickersViewModel.uiState.collectAsState()
    
    val emojiViewModel: ChatEmojiViewModel = hiltViewModel()
    val emojiState by emojiViewModel.uiState.collectAsState()
    
    val customEmojiViewModel: CustomEmojiViewModel = hiltViewModel()
    
    var messageInputView by remember { mutableStateOf<EditText?>(null) }
    
    var selectionBounds by remember { mutableStateOf<IntRect?>(null) }
    var isFormattingMenuShown by remember { mutableStateOf(false) }
    var inputMethodMenuPoint by remember { mutableStateOf<IntOffset?>(null) }
    var isSelectionDragging by remember { mutableStateOf(false) }
    var lastSelectionUpdateAt by remember { mutableStateOf(0L) }
    
    LaunchedEffect(selectionBounds) {
        if (selectionBounds == null) {
            isSelectionDragging = false
            
            return@LaunchedEffect
        }
        
        val now = SystemClock.elapsedRealtime()
        
        if (now - lastSelectionUpdateAt < SELECTION_DRAG_DETECT_MS) {
            isSelectionDragging = true
        }
        
        lastSelectionUpdateAt = now
        delay(SELECTION_TOOLBAR_RESHOW_DELAY_MS)
        
        isSelectionDragging = false
    }
    
    val panelPagerState = rememberPagerState(
        initialPage = EMOJI_PANEL_PAGE, pageCount = { PANEL_PAGE_COUNT })
    
    val bottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val imeInsets = WindowInsets.ime
    val imeHeightDp = with(density) {
        imeInsets.getBottom(density).toDp()
    }
    
    val stickerPanelHeight = remember { Animatable(bottomPadding, Dp.VectorConverter) }
    
    var measuredKeyboardHeight by remember { mutableStateOf(0.dp) }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var isStickersVisible by remember { mutableStateOf(false) }
    
    val keyboardHeight = if (measuredKeyboardHeight > 0.dp) {
        measuredKeyboardHeight
    } else {
        uiState.keyboardHeight.dp
    }
    
    LaunchedEffect(imeInsets, density) {
        snapshotFlow { imeInsets.getBottom(density) }.debounce(KEYBOARD_MEASURE_DELAY_MS.milliseconds)
            .distinctUntilChanged()
            .collect { imeBottomPx ->
                if (imeBottomPx <= 0) return@collect
                
                val height = with(density) { imeBottomPx.toDp() }
                
                measuredKeyboardHeight = height
                actions.onKeyboardHeightChanged(height.value)
            }
    }
    
    LaunchedEffect(imeHeightDp, keyboardHeight) {
        if (imeHeightDp > keyboardHeight) {
            measuredKeyboardHeight = imeHeightDp
        }
        
        isKeyboardVisible = imeHeightDp > 0.dp && imeHeightDp >= keyboardHeight
        
        if (isKeyboardVisible) {
            stickerPanelHeight.snapTo(imeHeightDp)
            isStickersVisible = false
        } else if (!isStickersVisible) {
            stickerPanelHeight.snapTo(imeHeightDp.coerceAtLeast(bottomPadding))
        }
    }
    
    LaunchedEffect(isKeyboardVisible, isStickersVisible) {
        if (isKeyboardVisible || isStickersVisible) {
            stickersViewModel.preloadPacks()
            emojiViewModel.preloadPacks()
        }
    }
    
    BackHandler(enabled = isKeyboardVisible) {
        isStickersVisible = false
        keyboardController?.hide()
    }
    
    BackHandler(enabled = !isKeyboardVisible && isStickersVisible) {
        scope.launch {
            stickerPanelHeight.animateTo(bottomPadding)
            isStickersVisible = false
        }
    }
    
    val onSelectionBoundsChanged: (IntRect?) -> Unit = { bounds ->
        if (bounds != null && selectionBounds == null) {
            isFormattingMenuShown = false
        }
        
        selectionBounds = bounds
    }
    
    val onCutSelectionClicked: () -> Unit = {
        messageInputView?.onTextContextMenuItem(android.R.id.cut)
    }
    
    val onCopySelectionClicked: () -> Unit = {
        messageInputView?.onTextContextMenuItem(android.R.id.copy)
    }
    
    val onFormatAction: (MessageFormatAction) -> Unit = { action ->
        messageInputView?.let { view ->
            val applied = applyMessageFormatAction(
                editText = view, action = action, onTextChange = actions::changeText
            )
            
            if (applied) {
                isFormattingMenuShown = false
                selectionBounds = null
            }
        }
    }
    
    val showStickerPanel: () -> Unit = {
        keyboardController?.hide()
        isStickersVisible = true
        
        scope.launch {
            stickerPanelHeight.animateTo(keyboardHeight.coerceAtLeast(bottomPadding))
        }
    }
    
    val onInputViewReady: (EditText) -> Unit = { view ->
        messageInputView = view
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            when (ChatType.fromId(uiState.chatId)) {
                ChatType.CHANNEL -> {
                    AnimatedContent(
                        targetState = when {
                            alwaysShowInput -> "input"
                            uiState.isOwner -> "input"
                            !uiState.isJoined -> "join"
                            else -> "none"
                        }, modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) { state ->
                        when (state) {
                            "input" -> InputMessage(
                                uiState = uiState,
                                actions = actions,
                                isVoiceEnabled = isVoiceEnabled,
                                sendOptionsEnabled = sendOptionsEnabled,
                                onMediaSheetSend = onMediaSheetSend,
                                onContactSend = onContactSend,
                                isStickerPanelVisible = isStickersVisible,
                                isKeyboardVisible = isKeyboardVisible,
                                keyboardHeight = keyboardHeight,
                                onShowStickerPanel = showStickerPanel,
                                onInputViewReady = onInputViewReady,
                                onResolveEmoji = customEmojiViewModel::resolveEmoji,
                                onSelectionChanged = onSelectionBoundsChanged,
                                onInputMethodMenu = { inputMethodMenuPoint = it },
                                sendIcon = sendIcon
                            )
                            
                            "join" -> JoinButton(onClick = actions::onJoinClicked)
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
                            InputMessage(
                                uiState = uiState,
                                actions = actions,
                                isVoiceEnabled = isVoiceEnabled,
                                sendOptionsEnabled = sendOptionsEnabled,
                                onMediaSheetSend = onMediaSheetSend,
                                onContactSend = onContactSend,
                                isStickerPanelVisible = isStickersVisible,
                                isKeyboardVisible = isKeyboardVisible,
                                keyboardHeight = keyboardHeight,
                                onShowStickerPanel = showStickerPanel,
                                onInputViewReady = onInputViewReady,
                                onResolveEmoji = customEmojiViewModel::resolveEmoji,
                                onSelectionChanged = onSelectionBoundsChanged,
                                onInputMethodMenu = { inputMethodMenuPoint = it },
                                sendIcon = sendIcon
                            )
                        } else {
                            JoinButton(onClick = actions::onJoinClicked)
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
                                    text = stringResource(R.string.sending_messages_restricted),
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
                                                    actions.showBlockDialog()
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
                                InputMessage(
                                    uiState = uiState,
                                    actions = actions,
                                    isVoiceEnabled = isVoiceEnabled,
                                    sendOptionsEnabled = sendOptionsEnabled,
                                    onMediaSheetSend = onMediaSheetSend,
                                    onContactSend = onContactSend,
                                    isStickerPanelVisible = isStickersVisible,
                                    isKeyboardVisible = isKeyboardVisible,
                                    keyboardHeight = keyboardHeight,
                                    onShowStickerPanel = showStickerPanel,
                                    onInputViewReady = onInputViewReady,
                                    onResolveEmoji = customEmojiViewModel::resolveEmoji,
                                    onSelectionChanged = onSelectionBoundsChanged,
                                    onInputMethodMenu = { inputMethodMenuPoint = it },
                                    sendIcon = sendIcon
                                )
                            }
                        }
                    }
                }
                
                else -> {}
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stickerPanelHeight.value.coerceAtLeast(bottomPadding))
        ) {
            if (isStickersVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}) {
                    HorizontalPager(
                        state = panelPagerState
                    ) { page ->
                        if (page == EMOJI_PANEL_PAGE) {
                            EmojiInputPanel(
                                packs = emojiState.addedPacks,
                                systemEmojis = emojiState.systemEmojis,
                                onEmojiClick = { pack, emoji ->
                                    messageInputView?.let { view ->
                                        scope.launch {
                                            insertCustomEmoji(view, pack.id, emoji)
                                        }
                                    }
                                },
                                onSystemEmojiClick = { emoji ->
                                    messageInputView?.let { view ->
                                        insertSystemEmoji(view, emoji)
                                    }
                                })
                        } else {
                            StickerInputPanel(
                                packs = stickersState.addedPacks, onStickerClick = { sticker ->
                                    actions.sendSticker(sticker.id)
                                })
                        }
                    }
                    
                    AppPrimaryScrollableTabRow(
                        selectedTabIndex = panelPagerState.currentPage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        AppTab(
                            selected = panelPagerState.currentPage == EMOJI_PANEL_PAGE,
                            text = stringResource(R.string.emoji),
                            onClick = {
                                scope.launch {
                                    panelPagerState.animateScrollToPage(EMOJI_PANEL_PAGE)
                                }
                            })
                        
                        AppTab(
                            selected = panelPagerState.currentPage == STICKER_PANEL_PAGE,
                            text = stringResource(R.string.stickers),
                            onClick = {
                                scope.launch {
                                    panelPagerState.animateScrollToPage(STICKER_PANEL_PAGE)
                                }
                            })
                    }
                    
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        this@Column.AnimatedVisibility(
                            visible = panelPagerState.currentPage == EMOJI_PANEL_PAGE,
                            enter = expressiveScaleIn,
                            exit = expressiveScaleOut
                        ) {
                            IconButton(
                                onClick = {
                                    messageInputView?.let { view -> deleteBeforeCursor(view) }
                                },
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .scale(0.8f),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Backspace, null)
                            }
                        }
                    }
                    
                    BottomBarScrim(height = bottomPadding)
                }
            }
        }
        
        selectionBounds?.let { bounds ->
            if (!isSelectionDragging) {
                MessageSelectionToolbar(
                    selectionBoundsProvider = { selectionBounds ?: bounds },
                    isFormattingShown = isFormattingMenuShown,
                    onSelectAllClick = { messageInputView?.selectAll() },
                    onCutClick = onCutSelectionClicked,
                    onCopyClick = onCopySelectionClicked,
                    onMoreClick = { isFormattingMenuShown = true },
                    onFormatAction = onFormatAction,
                    onBackClick = { isFormattingMenuShown = false })
            }
        }
    }
    
    BackHandler(enabled = selectionBounds != null) {
        if (isFormattingMenuShown) {
            isFormattingMenuShown = false
        } else {
            selectionBounds = null
        }
    }
    
    inputMethodMenuPoint?.let { point ->
        InputMethodMenu(
            pressPoint = point,
            onPasteClick = {
                messageInputView?.onTextContextMenuItem(android.R.id.paste)
            },
            onDismiss = { inputMethodMenuPoint = null }
        )
    }
    
    BackHandler(enabled = inputMethodMenuPoint != null) {
        inputMethodMenuPoint = null
    }
}

@Composable
private fun InputMethodMenu(
    pressPoint: IntOffset,
    onPasteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val gapPx = with(density) { SELECTION_MENU_GAP_DP.dp.roundToPx() }
    val marginPx = with(density) { SELECTION_MENU_MARGIN_DP.dp.roundToPx() }
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val canPaste = clipboardManager.hasPrimaryClip() &&
            (clipboardManager.primaryClipDescription?.mimeTypeCount ?: 0) > 0
    
    Popup(
        popupPositionProvider = WindowTopLeftPositionProvider,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
        )
    }
    
    Popup(
        popupPositionProvider = SelectionMenuPositionProvider(
            selectionBounds = { IntRect(pressPoint, IntSize.Zero) },
            gapPx = gapPx,
            marginPx = marginPx
        ),
        properties = PopupProperties(focusable = false)
    ) {
        Box(modifier = Modifier.padding(SELECTION_MENU_SHADOW_PADDING_DP.dp)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = SELECTION_MENU_SHADOW_ELEVATION_DP.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canPaste) {
                        SelectionActionItem(
                            labelRes = R.string.paste,
                            onClick = {
                                onPasteClick()
                                onDismiss()
                            }
                        )
                    }
                    
                    SelectionActionItem(
                        labelRes = R.string.input_method,
                        onClick = {
                            val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                                    as InputMethodManager
                            
                            manager.showInputMethodPicker()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

private object WindowTopLeftPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ) = IntOffset.Zero
}

@Composable
private fun InputMessage(
    uiState: ChatUiState,
    actions: ChatInputActions,
    isVoiceEnabled: Boolean,
    sendOptionsEnabled: Boolean,
    onMediaSheetSend: ((silent: Boolean, scheduledAt: Long?) -> Unit)?,
    onContactSend: ((MessageContact) -> Unit)?,
    sendIcon: ImageVector,
    isStickerPanelVisible: Boolean,
    isKeyboardVisible: Boolean,
    keyboardHeight: Dp,
    onShowStickerPanel: () -> Unit,
    onInputViewReady: (EditText) -> Unit,
    onResolveEmoji: suspend (Long) -> CustomEmoji?,
    onSelectionChanged: (IntRect?) -> Unit,
    onInputMethodMenu: (IntOffset) -> Unit
) {
    var attachmentModal by remember { mutableStateOf(DialogController()) }
    var micTranslationX by remember { mutableFloatStateOf(0f) }
    var micTranslationY by remember { mutableFloatStateOf(0f) }
    
    var inputView by remember { mutableStateOf<EditText?>(null) }
    
    val inputHint = stringResource(R.string.message)
    
    LaunchedEffect(uiState.editingMessageId) {
        if (uiState.editingMessageId == null) return@LaunchedEffect
        
        inputView?.let { view -> focusMessageInput(view) }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                attachmentModal.hide()
                
                actions.sendMediaUris(
                    uris = uris, caption = uiState.messageText, replyTo = uiState.replyToMessage
                )
                
                actions.changeText("")
                actions.cancelReply()
            }
        })
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            actions.startRecording()
        } else {
            actions.onMicrophonePermissionDenied()
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
                IconButton(onClick = actions::cancelEditing) {
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
                val previewText = replyPreviewText(preview)
                val previewInlineContent = rememberCustomEmojiInlineContent(
                    text = previewText, emojiSize = REPLY_PREVIEW_EMOJI_SIZE
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = actions::onReplyPanelClicked)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reply_to, preview.title.orEmpty()),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildAnnotatedString { appendCustomEmojiText(previewText) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            inlineContent = previewInlineContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    IconButton(onClick = actions::cancelReply) {
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
            AnimatedContent(
                targetState = uiState.isRecording, transitionSpec = {
                    expressiveScaleIn togetherWith expressiveScaleOut
                }, contentAlignment = Alignment.Center
            ) { isRecording ->
                if (isRecording) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "recording_dot_transition")
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
                            animation = tween(800), repeatMode = RepeatMode.Reverse
                        ), label = "recording_dot_alpha"
                    )
                    IconButton(onClick = {}, enabled = false) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = dotAlpha))
                        )
                    }
                } else {
                    IconButton(onClick = {
                        if (isKeyboardVisible || !isStickerPanelVisible) {
                            onShowStickerPanel()
                            inputView?.let { view -> hideKeyboardKeepFocus(view) }
                        } else {
                            inputView?.let { view -> focusMessageInput(view) }
                        }
                    }) {
                        AnimatedContent(
                            targetState = isStickerPanelVisible && !isKeyboardVisible,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInVertically { -it } + fadeIn() + scaleIn() togetherWith slideOutVertically { it } + fadeOut() + scaleOut()
                                } else {
                                    slideInVertically { it } + fadeIn() + scaleIn() togetherWith slideOutVertically { -it } + fadeOut() + scaleOut()
                                }
                            }) { isPanelVisible ->
                            Icon(
                                imageVector = if (isPanelVisible) {
                                    Icons.Outlined.Keyboard
                                } else {
                                    Icons.Outlined.EmojiEmotions
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center
                ) {
                    val textFieldAlpha by animateFloatAsState(
                        targetValue = if (uiState.isRecording) 0f else 1f,
                        animationSpec = tween(200)
                    )
                    
                    CustomEmojiTextField(
                        text = uiState.messageText,
                        hint = inputHint,
                        onTextChange = actions::changeText,
                        onResolveEmoji = onResolveEmoji,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(textFieldAlpha),
                        onReceiveUris = { uris -> actions.sendFiles(uris) },
                        onViewReady = { view ->
                            inputView = view
                            
                            onInputViewReady(view)
                        },
                        onSelectionChanged = onSelectionChanged,
                        onEmptySpaceLongPress = onInputMethodMenu,
                        isFormattingEnabled = true
                    )
                    
                    VoiceRecordingStatus(
                        uiState = uiState,
                        micTranslationX = micTranslationX,
                        onCancelRecording = actions::cancelRecording
                    )
                }
            }
            
            AnimatedVisibility(
                visible = uiState.hasScheduledMessages && uiState.messageText.trim()
                    .isEmpty() && !uiState.isRecording && uiState.editingMessageId == null,
                enter = expressiveScaleIn,
                exit = expressiveScaleOut
            ) {
                IconButton(onClick = actions::onScheduledMessagesClicked) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                }) { showMic ->
                if (showMic && isVoiceEnabled) {
                    Box(
                        modifier = Modifier
                            .zIndex(if (uiState.isRecording) 10f else 0f)
                            .pointerInput(uiState.isRecordingLocked) {
                                if (uiState.isRecordingLocked) {
                                    detectTapGestures(
                                        onTap = {
                                            actions.stopRecordingAndSend()
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
                                                
                                                actions.startRecording()
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
                                                    actions.lockRecording()
                                                    isLocked = true
                                                    micTranslationX = 0f
                                                    micTranslationY = 0f
                                                } else if (deltaX < -250f && !isCanceled && !isLocked) {
                                                    actions.cancelRecording()
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
                                                actions.stopRecordingAndSend()
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
                    SendMessageOptionsButton(
                        onClick = actions::onSendMessageClicked,
                        onSendSilently = actions::onSendWithoutSoundClicked,
                        onSendScheduled = actions::onScheduleSendOptionsClicked,
                        optionsEnabled = sendOptionsEnabled && uiState.editingMessageId == null
                    ) {
                        AnimatedContent(
                            targetState = uiState.editingMessageId != null,
                            transitionSpec = { expressiveScaleIn togetherWith expressiveScaleOut }) { isEditing ->
                            Icon(
                                imageVector = if (isEditing) Icons.Rounded.Done else sendIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (attachmentModal.isVisible) {
        MediaPickerBottomSheet(
            chatId = uiState.chatId,
            replyTo = uiState.replyToMessage,
            caption = uiState.messageText,
            allowViewOnce = uiState.chatId != uiState.myId && ChatType.fromId(uiState.chatId) == ChatType.PRIVATE && uiState.myId > 0,
            allowSpoiler = true,
            keyboardHeight = keyboardHeight,
            onCaptionChange = actions::changeText,
            onDismissRequest = attachmentModal::hide,
            onFileSystemClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            onContactSend = onContactSend?.let { send ->
                { contact ->
                    attachmentModal.hide()
                    send(contact)
                }
            },
            onSent = {
                attachmentModal.hide()
                actions.changeText("")
                actions.cancelReply()
            },
            silentSendEnabled = sendOptionsEnabled,
            onSend = onMediaSheetSend
        )
    }
}

@Composable
private fun MessageSelectionToolbar(
    selectionBoundsProvider: () -> IntRect,
    isFormattingShown: Boolean,
    onSelectAllClick: () -> Unit,
    onCutClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMoreClick: () -> Unit,
    onFormatAction: (MessageFormatAction) -> Unit,
    onBackClick: () -> Unit
) {
    val density = LocalDensity.current
    val gapPx = with(density) { SELECTION_MENU_GAP_DP.dp.roundToPx() }
    val marginPx = with(density) { SELECTION_MENU_MARGIN_DP.dp.roundToPx() }
    
    val positionProvider = remember(gapPx, marginPx) {
        SelectionMenuPositionProvider(
            selectionBounds = selectionBoundsProvider, gapPx = gapPx, marginPx = marginPx
        )
    }
    
    Popup(
        popupPositionProvider = positionProvider, properties = PopupProperties(focusable = false)
    ) {
        Box(modifier = Modifier.padding(SELECTION_MENU_SHADOW_PADDING_DP.dp)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = SELECTION_MENU_SHADOW_ELEVATION_DP.dp
            ) {
                if (isFormattingShown) {
                    SelectionFormatItems(
                        onFormatAction = onFormatAction, onBackClick = onBackClick
                    )
                } else {
                    SelectionActionItems(
                        onSelectAllClick = onSelectAllClick,
                        onCutClick = onCutClick,
                        onCopyClick = onCopyClick,
                        onMoreClick = onMoreClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionActionItems(
    onSelectAllClick: () -> Unit,
    onCutClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val density = LocalDensity.current
    var panelHeightPx by remember { mutableIntStateOf(0) }
    
    Row(
        modifier = Modifier.onSizeChanged { size -> panelHeightPx = size.height },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionActionItem(
            labelRes = R.string.selection_select_all, onClick = onSelectAllClick
        )
        
        SelectionActionItem(
            labelRes = R.string.selection_cut, onClick = onCutClick
        )
        
        SelectionActionItem(
            labelRes = R.string.selection_copy, onClick = onCopyClick
        )
        
        Box(
            modifier = Modifier
                .height(with(density) { panelHeightPx.toDp() })
                .aspectRatio(1f)
                .clickable(onClick = onMoreClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SelectionActionItem(
    labelRes: Int, onClick: () -> Unit
) {
    Text(
        text = stringResource(labelRes),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
private fun SelectionFormatItems(
    onFormatAction: (MessageFormatAction) -> Unit, onBackClick: () -> Unit
) {
    val density = LocalDensity.current
    var backAreaHeightPx by remember { mutableIntStateOf(0) }
    
    Box(
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = with(density) { backAreaHeightPx.toDp() })
        ) {
            SelectionFormatItem(
                labelRes = R.string.format_bold,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                onClick = { onFormatAction(MessageFormatAction.ToggleBold) })
            
            SelectionFormatItem(
                labelRes = R.string.format_italic,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                onClick = { onFormatAction(MessageFormatAction.ToggleItalic) })
            
            SelectionFormatItem(
                labelRes = R.string.format_underline,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                ),
                onClick = { onFormatAction(MessageFormatAction.ToggleUnderline) })
            
            SelectionFormatItem(
                labelRes = R.string.format_strikethrough,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.LineThrough
                ),
                onClick = { onFormatAction(MessageFormatAction.ToggleStrikethrough) })
            
            SelectionFormatItem(
                labelRes = R.string.format_monospace,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                onClick = { onFormatAction(MessageFormatAction.ToggleMono) })
            
            SelectionFormatItem(
                labelRes = R.string.format_plain,
                textStyle = MaterialTheme.typography.bodyMedium,
                onClick = { onFormatAction(MessageFormatAction.Clear) })
        }
        
        SelectionFormatBackButton(
            labelRes = R.string.back,
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> backAreaHeightPx = size.height }
                .background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

@Composable
private fun SelectionFormatBackButton(
    labelRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            Text(
                text = stringResource(labelRes), style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SelectionFormatItem(
    labelRes: Int, textStyle: TextStyle, onClick: () -> Unit
) {
    Text(
        text = stringResource(labelRes),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = textStyle
    )
}

internal class SelectionMenuPositionProvider(
    private val selectionBounds: () -> IntRect, private val gapPx: Int, private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val bounds = selectionBounds()
        val maxLeft = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val left = (bounds.center.x - popupContentSize.width / 2).coerceIn(marginPx, maxLeft)
        val aboveTop = bounds.top - popupContentSize.height - gapPx
        val top = if (aboveTop < marginPx) bounds.bottom + gapPx else aboveTop
        
        return IntOffset(left, top)
    }
}

private val REPLY_PREVIEW_EMOJI_SIZE = 14.sp
private const val KEYBOARD_MEASURE_DELAY_MS = 300L
private const val EMOJI_PANEL_PAGE = 0
private const val STICKER_PANEL_PAGE = 1
private const val PANEL_PAGE_COUNT = 2
private const val SELECTION_MENU_GAP_DP = 4
private const val SELECTION_MENU_MARGIN_DP = 8
private const val SELECTION_MENU_SHADOW_PADDING_DP = 8
private const val SELECTION_MENU_SHADOW_ELEVATION_DP = 6
private const val SELECTION_DRAG_DETECT_MS = 120L
private const val SELECTION_TOOLBAR_RESHOW_DELAY_MS = 250L
