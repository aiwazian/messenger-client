/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.components.PhotoPickerBottomSheet
import com.aiwazian.messenger.utils.EmojiInput
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val STICKER_CELL_MIN_SIZE = 64.dp
private val STICKER_FOCUS_TOP_PADDING = 50.dp
private val DELETE_ICON_SIZE = 18.dp
private val DELETE_ICON_SPACING = 8.dp
private const val FOCUS_SCALE = 2.5f
private const val SCRIM_ALPHA = 0.6f
private const val EMOJI_FIELD_WIDTH_FRACTION = 0.8f
private const val PRESSED_CELL_SCALE = 0.9f

private val FOCUS_OPEN_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 260, easing = FastOutSlowInEasing)

private val FOCUS_CLOSE_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 220, easing = FastOutSlowInEasing)

private object EmojiOnlyTransformation : InputTransformation {
    
    override fun TextFieldBuffer.transformInput() {
        val value = asCharSequence()
            .toString()
        
        val emojis = EmojiInput.format(EmojiInput.parse(value))
        
        if (emojis == value) {
            return
        }
        
        replace(0, length, emojis)
        
        selection = TextRange(length)
    }
}

@Stable
private class StickerFocusCloseState {
    
    var closingKey by mutableStateOf<String?>(null)
        private set
    
    private var pendingAction: (() -> Unit)? = null
    
    fun request(key: String, action: () -> Unit) {
        if (closingKey == key) {
            return
        }
        
        pendingAction = action
        closingKey = key
    }
    
    fun onClosed() {
        val action = pendingAction
        
        pendingAction = null
        closingKey = null
        
        action?.invoke()
    }
}

@Composable
fun StickerPackEditorScreen(
    packId: Long? = null,
    packName: String? = null,
    packUsername: String? = null,
    viewModel: StickerPackEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    var isPickerVisible by remember { mutableStateOf(false) }
    var isExitDialogVisible by remember { mutableStateOf(false) }
    var undoKey by remember { mutableStateOf<String?>(null) }
    
    val stickerBounds = remember { mutableMapOf<String, Rect>() }
    val focusClose = remember { StickerFocusCloseState() }
    
    val goBack: () -> Unit = { navBackStack.removeLastOrNull() }
    
    val closeFocusedSticker: () -> Unit = {
        val focusedKey = uiState.focusedStickerKey
        
        if (focusedKey != null) {
            focusClose.request(focusedKey, viewModel::clearFocus)
        }
    }
    
    val onBackClick: () -> Unit = {
        when {
            uiState.focusedStickerKey != null -> closeFocusedSticker()
            uiState.hasChanges -> isExitDialogVisible = true
            else -> goBack()
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.load(
            packId = packId,
            name = packName,
            username = packUsername
        )
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StickerPackEditorEffect.ShowMessage -> {
                    snackbarJob?.cancel()
                    
                    undoKey = effect.undoKey
                    
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            UiText.StringResource(effect.messageRes)
                                .asString(context)
                        )
                        
                        undoKey = null
                    }
                }
                
                StickerPackEditorEffect.Saved -> goBack()
            }
        }
    }
    
    BackHandler(enabled = uiState.focusedStickerKey != null || uiState.hasChanges) {
        if (uiState.focusedStickerKey != null) {
            closeFocusedSticker()
        } else {
            isExitDialogVisible = true
        }
    }
    
    val isFabVisible = uiState.canSave || uiState.isSaving
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                PageTopBar(
                    title = {
                        Text(
                            stringResource(
                                if (uiState.packId == null) {
                                    R.string.sticker_pack_new
                                } else {
                                    R.string.sticker_pack
                                }
                            )
                        )
                    },
                    onBackClick = onBackClick
                )
            },
            snackbarHost = {
                val pendingUndoKey = undoKey
                
                val undoAction: (@Composable () -> Unit)? = if (pendingUndoKey == null) {
                    null
                } else {
                    {
                        TextButton(
                            onClick = {
                                viewModel.undoRemove(pendingUndoKey)
                                
                                snackbarHostState.currentSnackbarData?.dismiss()
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.sticker_removed_undo))
                        }
                    }
                }
                
                AppSnackbar(
                    hostState = snackbarHostState,
                    trailingIcon = undoAction
                )
            },
            floatingActionButton = {
                if (isFabVisible) {
                    FloatingActionButton(onClick = viewModel::save, shape = CircleShape) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Done,
                                contentDescription = null
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box {
                TopBarScrim(height = innerPadding.calculateTopPadding())
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = STICKER_CELL_MIN_SIZE),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding.plus(PaddingValues(horizontal = 10.dp)),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionContainer(
                            contentPadding = PaddingValues.Zero,
                            footer = {
                                UsernameHint(status = uiState.usernameStatus)
                            }) {
                            FramelessTextBox(
                                placeholder = stringResource(R.string.sticker_pack_name),
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                trailingIcon = {
                                    Text(
                                        text = "${uiState.name.length}/${StickerPackEditorViewModel.MAX_NAME_LENGTH}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                })
                            
                            FramelessTextBox(
                                placeholder = stringResource(R.string.sticker_pack_username),
                                value = uiState.username,
                                onValueChange = viewModel::onUsernameChange,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                            )
                        }
                    }
                    
                    items(
                        items = uiState.stickers,
                        key = { it.key }) { slot ->
                        StickerSlotCell(
                            slot = slot,
                            isHidden = uiState.focusedStickerKey == slot.key,
                            onBoundsChange = { bounds -> stickerBounds[slot.key] = bounds },
                            onClick = { viewModel.focusSticker(slot.key) })
                    }
                    
                    item {
                        AddStickerCell(
                            isBusy = uiState.isAddingSticker,
                            onClick = { isPickerVisible = true })
                    }
                }
                
                BottomBarScrim(height = innerPadding.calculateBottomPadding())
            }
        }
        
        val focusedSticker = uiState.focusedSticker
        
        if (focusedSticker != null) {
            StickerFocusOverlay(
                slot = focusedSticker,
                origin = stickerBounds[focusedSticker.key],
                isClosing = focusClose.closingKey == focusedSticker.key,
                onEmojisChange = { value ->
                    viewModel.onStickerEmojisChange(focusedSticker.key, value)
                },
                onRemove = {
                    focusClose.request(focusedSticker.key) {
                        viewModel.removeSticker(focusedSticker.key)
                    }
                },
                onDismiss = closeFocusedSticker,
                onClosed = focusClose::onClosed
            )
        }
    }
    
    if (isPickerVisible) {
        PhotoPickerBottomSheet(
            maskShape = MaterialTheme.shapes.large,
            onPhotoPicked = viewModel::addSticker,
            onDismissRequest = { isPickerVisible = false },
            clipsToMask = true
        )
    }
    
    if (isExitDialogVisible) {
        AppDialog(
            title = "Несохраненные изменения",
            onDismissRequest = { isExitDialogVisible = false },
            buttons = {
                TextButton(onClick = {
                    isExitDialogVisible = false
                    
                    goBack()
                }) {
                    Text(stringResource(R.string.sticker_pack_unsaved_discard))
                }
                
                TextButton(
                    onClick = {
                        isExitDialogVisible = false
                        
                        viewModel.save(exitAfterSave = true)
                    },
                    enabled = uiState.canSave
                ) {
                    Text(stringResource(R.string.sticker_pack_unsaved_apply))
                }
            }) {
            Text(stringResource(R.string.sticker_pack_unsaved_message))
        }
    }
}

@Composable
private fun UsernameHint(status: UsernameStatus) {
    val text = when (status) {
        UsernameStatus.Empty -> stringResource(R.string.sticker_pack_username_hint)
        UsernameStatus.TooShort -> stringResource(
            R.string.sticker_pack_username_too_short,
            StickerPackEditorViewModel.MIN_USERNAME_LENGTH
        )
        
        UsernameStatus.Checking -> stringResource(R.string.sticker_pack_username_checking)
        UsernameStatus.Available -> stringResource(R.string.sticker_pack_username_available)
        UsernameStatus.Taken -> stringResource(R.string.sticker_pack_username_taken)
        UsernameStatus.Unknown -> stringResource(R.string.sticker_pack_username_unknown)
    }
    
    val color = when (status) {
        UsernameStatus.Available -> MaterialTheme.colorScheme.primary
        UsernameStatus.Taken, UsernameStatus.Unknown -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Text(
        text = text,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 10.dp
        ),
        color = color,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )
}

@Composable
private fun StickerSlotCell(
    slot: StickerSlot,
    isHidden: Boolean,
    onBoundsChange: (Rect) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_CELL_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ),
        label = "sticker_slot_scale"
    )
    
    val model = when (slot) {
        is StickerSlot.Local -> ImageRequest.Builder(context).data(slot.sticker.uri).build()
        
        is StickerSlot.Remote -> ImageRequest.Builder(context)
            .data(slot.url)
            .memoryCacheKey(slot.fileId)
            .diskCacheKey(slot.fileId)
            .build()
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates -> onBoundsChange(coordinates.boundsInRoot()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isHidden) 0f else 1f
            }
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun StickerFocusOverlay(
    slot: StickerSlot,
    origin: Rect?,
    isClosing: Boolean,
    onEmojisChange: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    onClosed: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val focusRequester = remember { FocusRequester() }
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val progress = remember { Animatable(0f) }
    
    var stickerCenter by remember { mutableStateOf(Offset.Zero) }
    
    val emojiState = remember(slot.key) {
        TextFieldState(initialText = EmojiInput.format(slot.emojis))
    }
    
    val scrimColor = MaterialTheme.colorScheme.scrim
    
    val cellSize = origin?.width?.takeIf { it > 0f } ?: with(density) {
        STICKER_CELL_MIN_SIZE.toPx()
    }
    
    val focusedSize = with(density) { (cellSize * FOCUS_SCALE).toDp() }
    
    val model = when (slot) {
        is StickerSlot.Local -> ImageRequest.Builder(context).data(slot.sticker.uri).build()
        
        is StickerSlot.Remote -> ImageRequest.Builder(context)
            .data(slot.url)
            .memoryCacheKey(slot.fileId)
            .diskCacheKey(slot.fileId)
            .build()
    }
    
    LaunchedEffect(slot.key) {
        progress.snapTo(0f)
        
        progress.animateTo(targetValue = 1f, animationSpec = FOCUS_OPEN_SPEC)
    }
    
    LaunchedEffect(isClosing) {
        if (!isClosing) {
            return@LaunchedEffect
        }
        
        progress.animateTo(targetValue = 0f, animationSpec = FOCUS_CLOSE_SPEC)
        
        onClosed()
    }
    
    LaunchedEffect(slot.key) {
        focusRequester.requestFocus()
    }
    
    LaunchedEffect(emojiState) {
        snapshotFlow { emojiState.text.toString() }.collect { value ->
            onEmojisChange(value)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = scrimColor, alpha = SCRIM_ALPHA * progress.value)
            }
            .clickable(
                interactionSource = scrimInteractionSource,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = STICKER_FOCUS_TOP_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(EMOJI_FIELD_WIDTH_FRACTION)
                    .graphicsLayer { alpha = progress.value }
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                FramelessTextBox(
                    placeholder = stringResource(R.string.sticker_emojis),
                    state = emojiState,
                    modifier = Modifier.focusRequester(focusRequester),
                    inputTransformation = EmojiOnlyTransformation,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
            }
            
            Box(
                modifier = Modifier
                    .size(focusedSize)
                    .onGloballyPositioned { coordinates ->
                        stickerCenter = coordinates.boundsInRoot().center
                    }
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val fraction = progress.value
                            
                            val startScale = if (size.width > 0f) {
                                cellSize / size.width
                            } else {
                                1f
                            }
                            
                            val currentScale = lerp(startScale, 1f, fraction)
                            
                            scaleX = currentScale
                            scaleY = currentScale
                            
                            val flightStart =
                                if (origin != null && stickerCenter != Offset.Zero) {
                                    origin.center
                                } else {
                                    stickerCenter
                                }
                            
                            translationX = lerp(
                                flightStart.x - stickerCenter.x,
                                0f,
                                fraction
                            )
                            
                            translationY = lerp(
                                flightStart.y - stickerCenter.y,
                                0f,
                                fraction
                            )
                        },
                    contentScale = ContentScale.Fit
                )
            }
            
            TextButton(
                onClick = onRemove,
                modifier = Modifier.graphicsLayer { alpha = progress.value },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(DELETE_ICON_SIZE)
                )
                
                Spacer(modifier = Modifier.width(DELETE_ICON_SPACING))
                
                Text(stringResource(R.string.sticker_delete))
            }
        }
    }
}

@Composable
private fun AddStickerCell(isBusy: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_CELL_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ),
        label = "add_sticker_scale"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isBusy,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBusy) {
            CircularWavyProgressIndicator(modifier = Modifier.size(28.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.sticker_add),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
