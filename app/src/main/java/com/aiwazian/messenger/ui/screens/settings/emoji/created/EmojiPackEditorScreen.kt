package com.aiwazian.messenger.ui.screens.settings.emoji.created

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.RectangleShape
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
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.components.PhotoPickerBottomSheet
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_CELL_MIN_SIZE
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_GRID_SPACING
import com.aiwazian.messenger.ui.screens.settings.emoji.EmojiPickerBottomSheet
import com.aiwazian.messenger.utils.EmojiInput
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val EMOJI_FOCUS_TOP_PADDING = 50.dp
private val COVER_SIZE = 96.dp
private const val FOCUS_SCALE = 4f
private const val SCRIM_ALPHA = 0.6f
private const val EMOJI_FIELD_WIDTH_FRACTION = 0.8f
private const val PRESSED_CELL_SCALE = 0.9f
private const val COVER_BUTTON_LABEL = "Выбрать обложку"
private const val PICK_EMOJI_LABEL = "Выбрать эмодзи"
private const val PICK_FILE_LABEL = "Выбрать файл"
private const val REMOVE_COVER_LABEL = "Удалить обложку"

private val FOCUS_OPEN_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 260, easing = FastOutSlowInEasing)

private val FOCUS_CLOSE_SPEC: AnimationSpec<Float> =
    tween(durationMillis = 220, easing = FastOutSlowInEasing)

private enum class EmojiPickTarget {
    Cover,
    Emoji
}

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
private class EmojiFocusCloseState {
    
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
fun EmojiPackEditorScreen(
    packId: Long? = null,
    packName: String? = null,
    packUsername: String? = null,
    viewModel: EmojiPackEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    var photoPickerTarget by remember { mutableStateOf<EmojiPickTarget?>(null) }
    var emojiPickerTarget by remember { mutableStateOf<EmojiPickTarget?>(null) }
    var isCoverMenuExpanded by remember { mutableStateOf(false) }
    var isAddMenuExpanded by remember { mutableStateOf(false) }
    var isExitDialogVisible by remember { mutableStateOf(false) }
    var undoKey by remember { mutableStateOf<String?>(null) }
    
    val emojiBounds = remember { mutableMapOf<String, Rect>() }
    val focusClose = remember { EmojiFocusCloseState() }
    
    val goBack: () -> Unit = { navBackStack.removeLastOrNull() }
    
    val closeFocusedEmoji: () -> Unit = {
        val focusedKey = uiState.focusedEmojiKey
        
        if (focusedKey != null) {
            focusClose.request(focusedKey, viewModel::clearFocus)
        }
    }
    
    val onBackClick: () -> Unit = {
        when {
            uiState.focusedEmojiKey != null -> closeFocusedEmoji()
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
                is EmojiPackEditorEffect.ShowMessage -> {
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
                
                EmojiPackEditorEffect.Saved -> goBack()
            }
        }
    }
    
    BackHandler(enabled = uiState.focusedEmojiKey != null || uiState.hasChanges) {
        if (uiState.focusedEmojiKey != null) {
            closeFocusedEmoji()
        } else {
            isExitDialogVisible = true
        }
    }
    
    val isFabVisible = uiState.hasChanges && (uiState.canSave || uiState.isSaving)
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                PageTopBar(
                    title = {
                        Text(
                            stringResource(
                                if (uiState.packId == null) {
                                    R.string.emoji_pack_new
                                } else {
                                    R.string.emoji_pack
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
                            Text(stringResource(R.string.emoji_removed_undo))
                        }
                    }
                }
                
                AppSnackbar(
                    hostState = snackbarHostState,
                    trailingIcon = undoAction
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = expressiveScaleIn,
                    exit = expressiveScaleOut
                ) {
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
                    columns = GridCells.Adaptive(minSize = EMOJI_CELL_MIN_SIZE),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding.plus(PaddingValues(horizontal = 10.dp)),
                    horizontalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING),
                    verticalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CoverPicker(
                                    cover = uiState.cover,
                                    isBusy = uiState.isChangingCover,
                                    onClick = { isCoverMenuExpanded = true })
                                
                                AppDropdownMenu(
                                    expanded = isCoverMenuExpanded,
                                    onDismissRequest = { isCoverMenuExpanded = false }) {
                                    AppDropdownMenuItem(
                                        text = PICK_EMOJI_LABEL,
                                        onClick = {
                                            isCoverMenuExpanded = false
                                            
                                            emojiPickerTarget = EmojiPickTarget.Cover
                                        })
                                    
                                    AppDropdownMenuItem(
                                        text = PICK_FILE_LABEL,
                                        onClick = {
                                            isCoverMenuExpanded = false
                                            
                                            photoPickerTarget = EmojiPickTarget.Cover
                                        })
                                    
                                    if (uiState.cover != null) {
                                        AppDropdownMenuItem(
                                            text = REMOVE_COVER_LABEL,
                                            onClick = {
                                                isCoverMenuExpanded = false
                                                
                                                viewModel.removeCover()
                                            })
                                    }
                                }
                            }
                            
                            SectionContainer(
                                contentPadding = PaddingValues.Zero,
                                footer = {
                                    UsernameHint(status = uiState.usernameStatus)
                                }) {
                                FramelessTextBox(
                                    placeholder = stringResource(R.string.emoji_pack_name),
                                    value = uiState.name,
                                    onValueChange = viewModel::onNameChange,
                                    trailingIcon = {
                                        Text(
                                            text = "${uiState.name.length}/${EmojiPackEditorViewModel.MAX_NAME_LENGTH}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    })
                                
                                FramelessTextBox(
                                    placeholder = stringResource(R.string.emoji_pack_username),
                                    value = uiState.username,
                                    onValueChange = viewModel::onUsernameChange,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                                )
                            }
                        }
                    }
                    
                    items(
                        items = uiState.emojis,
                        key = { it.key }) { slot ->
                        EmojiSlotCell(
                            slot = slot,
                            isHidden = uiState.focusedEmojiKey == slot.key,
                            onBoundsChange = { bounds -> emojiBounds[slot.key] = bounds },
                            onClick = { viewModel.focusEmoji(slot.key) })
                    }
                    
                    item {
                        Box {
                            AddEmojiCell(
                                isBusy = uiState.isAddingEmoji,
                                onClick = { isAddMenuExpanded = true })
                            
                            AppDropdownMenu(
                                expanded = isAddMenuExpanded,
                                onDismissRequest = { isAddMenuExpanded = false }) {
                                AppDropdownMenuItem(
                                    text = PICK_EMOJI_LABEL,
                                    onClick = {
                                        isAddMenuExpanded = false
                                        
                                        emojiPickerTarget = EmojiPickTarget.Emoji
                                    })
                                
                                AppDropdownMenuItem(
                                    text = PICK_FILE_LABEL,
                                    onClick = {
                                        isAddMenuExpanded = false
                                        
                                        photoPickerTarget = EmojiPickTarget.Emoji
                                    })
                            }
                        }
                    }
                }
                
                BottomBarScrim(height = innerPadding.calculateBottomPadding())
            }
        }
        
        val focusedEmoji = uiState.focusedEmoji
        
        if (focusedEmoji != null) {
            EmojiFocusOverlay(
                slot = focusedEmoji,
                origin = emojiBounds[focusedEmoji.key],
                isClosing = focusClose.closingKey == focusedEmoji.key,
                onEmojisChange = { value ->
                    viewModel.onEmojiSymbolsChange(focusedEmoji.key, value)
                },
                onRemove = {
                    focusClose.request(focusedEmoji.key) {
                        viewModel.removeEmoji(focusedEmoji.key)
                    }
                },
                onDismiss = closeFocusedEmoji,
                onClosed = focusClose::onClosed
            )
        }
    }
    
    val activePhotoTarget = photoPickerTarget
    
    if (activePhotoTarget != null) {
        PhotoPickerBottomSheet(
            maskShape = RectangleShape,
            onPhotoPicked = { uri ->
                when (activePhotoTarget) {
                    EmojiPickTarget.Cover -> viewModel.setCoverFromFile(uri)
                    EmojiPickTarget.Emoji -> viewModel.addEmoji(uri)
                }
            },
            onDismissRequest = { photoPickerTarget = null },
            clipsToMask = true
        )
    }
    
    val activeEmojiTarget = emojiPickerTarget
    
    if (activeEmojiTarget != null) {
        EmojiPickerBottomSheet(
            onEmojiSelected = { emoji ->
                when (activeEmojiTarget) {
                    EmojiPickTarget.Cover -> viewModel.setCoverFromEmoji(emoji)
                    EmojiPickTarget.Emoji -> viewModel.addEmojiFromExisting(emoji)
                }
                
                emojiPickerTarget = null
            },
            onDismissRequest = { emojiPickerTarget = null },
            addedFileIds = if (activeEmojiTarget == EmojiPickTarget.Emoji) {
                uiState.emojis.mapNotNull { slot ->
                    (slot as? EmojiSlot.Remote)?.fileId
                }.toSet()
            } else {
                emptySet()
            })
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
                    Text(stringResource(R.string.emoji_pack_unsaved_discard))
                }
                
                TextButton(
                    onClick = {
                        isExitDialogVisible = false
                        
                        viewModel.save(exitAfterSave = true)
                    },
                    enabled = uiState.canSave
                ) {
                    Text(stringResource(R.string.emoji_pack_unsaved_apply))
                }
            }) {
            Text(stringResource(R.string.emoji_pack_unsaved_message))
        }
    }
}

@Composable
private fun CoverPicker(
    cover: EmojiPackCover?,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    if (cover == null) {
        Button(onClick = onClick, enabled = !isBusy) {
            if (isBusy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(COVER_BUTTON_LABEL)
            }
        }
        
        return
    }
    
    val model = when (cover) {
        is EmojiPackCover.Local -> ImageRequest.Builder(context)
            .data(cover.emoji.uri)
            .build()
        
        is EmojiPackCover.Remote -> ImageRequest.Builder(context)
            .data(cover.url)
            .memoryCacheKey(cover.fileId)
            .diskCacheKey(cover.fileId)
            .build()
    }
    
    Box(
        modifier = Modifier
            .size(COVER_SIZE)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !isBusy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBusy) {
            CircularWavyProgressIndicator(modifier = Modifier.size(28.dp))
        } else {
            AsyncImage(
                model = model,
                contentDescription = COVER_BUTTON_LABEL,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun UsernameHint(status: EmojiUsernameStatus) {
    val text = when (status) {
        EmojiUsernameStatus.Empty -> stringResource(R.string.emoji_pack_username_hint)
        EmojiUsernameStatus.TooShort -> stringResource(
            R.string.emoji_pack_username_too_short,
            EmojiPackEditorViewModel.MIN_USERNAME_LENGTH
        )
        
        EmojiUsernameStatus.Checking -> stringResource(R.string.emoji_pack_username_checking)
        EmojiUsernameStatus.Available -> stringResource(R.string.emoji_pack_username_available)
        EmojiUsernameStatus.Taken -> stringResource(R.string.emoji_pack_username_taken)
        EmojiUsernameStatus.Unknown -> stringResource(R.string.emoji_pack_username_unknown)
    }
    
    val color = when (status) {
        EmojiUsernameStatus.Available -> MaterialTheme.colorScheme.primary
        EmojiUsernameStatus.Taken, EmojiUsernameStatus.Unknown -> MaterialTheme.colorScheme.error
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
private fun EmojiSlotCell(
    slot: EmojiSlot,
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
        label = "emoji_slot_scale"
    )
    
    val model = when (slot) {
        is EmojiSlot.Local -> ImageRequest.Builder(context).data(slot.emoji.uri).build()
        
        is EmojiSlot.Remote -> ImageRequest.Builder(context)
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
private fun EmojiFocusOverlay(
    slot: EmojiSlot,
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
    
    var emojiCenter by remember { mutableStateOf(Offset.Zero) }
    
    val emojiState = remember(slot.key) {
        TextFieldState(initialText = EmojiInput.format(slot.emojis))
    }
    
    val scrimColor = MaterialTheme.colorScheme.scrim
    
    val cellSize = origin?.width?.takeIf { it > 0f } ?: with(density) {
        EMOJI_CELL_MIN_SIZE.toPx()
    }
    
    val focusedSize = with(density) { (cellSize * FOCUS_SCALE).toDp() }
    
    val model = when (slot) {
        is EmojiSlot.Local -> ImageRequest.Builder(context).data(slot.emoji.uri).build()
        
        is EmojiSlot.Remote -> ImageRequest.Builder(context)
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
                .padding(top = EMOJI_FOCUS_TOP_PADDING),
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
                    placeholder = stringResource(R.string.emoji_symbols),
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
                        emojiCenter = coordinates.boundsInRoot().center
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
                                if (origin != null && emojiCenter != Offset.Zero) {
                                    origin.center
                                } else {
                                    emojiCenter
                                }
                            
                            translationX = lerp(
                                flightStart.x - emojiCenter.x,
                                0f,
                                fraction
                            )
                            
                            translationY = lerp(
                                flightStart.y - emojiCenter.y,
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(stringResource(R.string.emoji_delete))
            }
        }
    }
}

@Composable
private fun AddEmojiCell(isBusy: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_CELL_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ),
        label = "add_emoji_scale"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
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
            CircularWavyProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.emoji_add),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
