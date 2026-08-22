/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.screens.chat.MediaPickerViewModel
import java.util.Locale

/**
 * Шторка вложений: лента фото и видео устройства сеткой в три столбца.
 *
 * Выбор нумерованный, и порядок номеров — это порядок вложений в сообщении.
 * Подпись и отправка живут в нижней панели, а до первого выбора там же лежит
 * переключение между галереей и файлами.
 *
 * Подпись — это черновик чата, а не отдельное поле: текст, набранный в чате до
 * нажатия на скрепку, уже лежит в ней, а правки видны в поле ввода, если
 * шторку закрыли, ничего не отправив.
 *
 * Нижняя панель приклеена к низу экрана, а не к низу шторки: её поднимают на
 * текущее смещение шторки, поэтому и на половину экрана, и на весь экран она
 * стоит на одном месте. Так же сделана кнопка отправки в ShareBottomSheet.
 *
 * Доступ к галерее запрашивается при открытии. Если система больше не покажет
 * диалог, кнопка в заглушке ведёт в настройки приложения: повторный запрос там
 * вернулся бы мгновенно и с тем же ответом, а кнопка выглядела бы сломанной.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerBottomSheet(
    chatId: Long,
    replyTo: MessageReplyPreview?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onFileSystemClick: () -> Unit,
    onSent: () -> Unit
) {
    val viewModel: MediaPickerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasMediaPermission()) }
    var wasAsked by remember { mutableStateOf(false) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = context.hasMediaPermission()
        wasAsked = true
        
        if (hasPermission) {
            viewModel.loadMedia()
        }
    }
    
    /* Доступ могли выдать руками в настройках — ловим возвращение в приложение. */
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!hasPermission && context.hasMediaPermission()) {
            hasPermission = true
            viewModel.loadMedia()
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.reset()
        
        if (hasPermission) {
            viewModel.loadMedia()
        } else {
            permissionLauncher.launch(mediaPermissions())
        }
    }
    
    AppBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                !hasPermission -> {
                    MediaPickerNotice(
                        text = stringResource(R.string.media_picker_permission),
                        actionText = stringResource(R.string.media_picker_permission_action),
                        onActionClick = {
                            if (context.canRequestMediaPermission(wasAsked)) {
                                permissionLauncher.launch(mediaPermissions())
                            } else {
                                context.openAppSettings()
                            }
                        })
                }
                
                uiState.isLoading && uiState.media.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 104.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }
                
                uiState.media.isEmpty() -> {
                    MediaPickerNotice(text = stringResource(R.string.media_picker_empty))
                }
                
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 2.dp, top = 2.dp, end = 2.dp, bottom = 70.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                            )
                        }
                        itemsIndexed(
                            items = uiState.media,
                            key = { _, item -> item.id }) { index, item ->
                            MediaGridItem(
                                item = item,
                                number = uiState.selected.indexOf(item.uri) + 1,
                                loadThumbnail = { uri -> viewModel.thumbnail(uri) },
                                onClick = { previewIndex = index },
                                onToggleSelection = { viewModel.toggleSelection(item.uri) })
                        }
                        item(span = { GridItemSpan(GRID_COLUMNS) }) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            )
                        }
                    }
                }
            }
            
            MediaPickerToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(x = 0, y = -sheetState.requireOffset().toInt()) }
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(10.dp)
            ) {
                AnimatedContent(
                    targetState = uiState.selected.isNotEmpty(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "media_picker_toolbar"
                ) { hasSelection ->
                    if (hasSelection) {
                        CaptionRow(
                            caption = caption,
                            onCaptionChange = onCaptionChange,
                            onSendClick = {
                                viewModel.send(
                                    chatId = chatId, replyTo = replyTo, caption = caption
                                )
                                onSent()
                            })
                    } else {
                        SourceRow(onFileClick = onFileSystemClick)
                    }
                }
            }
        }
    }
    
    val index = previewIndex
    
    if (index != null) {
        MediaPickerPreview(
            media = uiState.media,
            initialIndex = index,
            selectionNumber = { item -> uiState.selected.indexOf(item.uri) + 1 },
            onToggleSelection = { item -> viewModel.toggleSelection(item.uri) },
            onDismiss = { previewIndex = null })
    }
}

/**
 * Нижняя панель шторки: плавающая плашка, размер которой задаём мы сами.
 *
 * Готовый HorizontalFloatingToolbar рассчитан на ряд кнопок и берёт тот размер,
 * который запросило содержимое, а поле ввода запрашивает всё, что дают: панель
 * раздувало на весь экран, и обратно она уже не собиралась.
 */
@Composable
private fun MediaPickerToolbar(
    modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = TOOLBAR_ELEVATION, shape = TOOLBAR_SHAPE)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer, shape = TOOLBAR_SHAPE
            )
            .clip(TOOLBAR_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Кружок выбора: пустой, а у выбранного внутри его порядковый номер.
 */
@Composable
internal fun MediaSelectionBadge(
    number: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null
) {
    val isSelected = number > 0
    
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        }, label = "media_badge_color"
    )
    
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ), contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text(
                text = number.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun MediaGridItem(
    item: DeviceMediaItem,
    number: Int,
    loadThumbnail: suspend (Uri) -> Bitmap?,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val context = LocalContext.current
    
    val scale by animateFloatAsState(
        targetValue = if (number > 0) SELECTED_SCALE else 1f, label = "media_item_scale"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (item.isVideo) {
                VideoThumbnail(item = item, loadThumbnail = loadThumbnail)
                
                Text(
                    text = formatDuration(item.durationMs),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        MediaSelectionBadge(
            number = number, modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp), onClick = onToggleSelection
        )
    }
}

/**
 * Кадр видео из MediaStore: Coil без coil-video превью не соберёт.
 */
@Composable
private fun VideoThumbnail(
    item: DeviceMediaItem, loadThumbnail: suspend (Uri) -> Bitmap?
) {
    var bitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(item.uri) {
        bitmap = loadThumbnail(item.uri)
    }
    
    val thumbnail = bitmap
    
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SourceRow(onFileClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {},
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Photo,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = stringResource(R.string.gallery))
        }
        
        TextButton(onClick = onFileClick, shape = CircleShape) {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = stringResource(R.string.media_picker_file))
        }
    }
}

@Composable
private fun CaptionRow(
    caption: String, onCaptionChange: (String) -> Unit, onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = caption,
            onValueChange = onCaptionChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            ),
            maxLines = 5,
            minLines = 1,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(
                        vertical = 12.dp, horizontal = 14.dp
                    )
                ) {
                    if (caption.isEmpty()) {
                        Text(
                            text = stringResource(R.string.media_picker_caption),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    innerTextField()
                }
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )
        
        IconButton(onClick = onSendClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MediaPickerNotice(
    text: String, actionText: String? = null, onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick, shape = CircleShape) {
                Text(text = actionText)
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
    
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
    )
    
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

/**
 * На Android 14 и новее пользователь может открыть доступ только к части
 * галереи: тогда READ_MEDIA_IMAGES не выдаётся, а MediaStore отдаёт выбранное.
 */
private fun Context.hasMediaPermission(): Boolean = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
        isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) || (isGranted(
            Manifest.permission.READ_MEDIA_IMAGES
        ) && isGranted(Manifest.permission.READ_MEDIA_VIDEO))
    }
    
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        isGranted(Manifest.permission.READ_MEDIA_IMAGES) && isGranted(Manifest.permission.READ_MEDIA_VIDEO)
    }
    
    else -> isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Покажет ли система диалог доступа ещё раз.
 *
 * После отказа «Больше не спрашивать» лаунчер молча возвращает прежний ответ:
 * диалога нет, экран не меняется, кнопка выглядит сломанной. Признак того, что
 * диалог ещё будет, — shouldShowRequestPermissionRationale хотя бы по одному
 * разрешению. До первого запроса он тоже false, поэтому первый раз спрашиваем
 * в любом случае.
 */
private fun Context.canRequestMediaPermission(wasAsked: Boolean): Boolean {
    if (!wasAsked) return true
    
    val activity = findActivity() ?: return false
    
    return mediaPermissions().any {
        ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }
}

/** Сведения о приложении: единственный путь к доступу после «Больше не спрашивать». */
private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    
    startActivity(intent)
}

/** Activity под Compose-контекстом: она нужна для проверки rationale. */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    
    return null
}

private val TOOLBAR_SHAPE = RoundedCornerShape(28.dp)
private val TOOLBAR_ELEVATION = 3.dp
private const val GRID_COLUMNS = 3
private const val SELECTED_SCALE = 0.9f
