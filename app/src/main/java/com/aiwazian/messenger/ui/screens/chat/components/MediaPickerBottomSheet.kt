/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaPickerBottomSheet(
    chatId: Long,
    replyTo: MessageReplyPreview?,
    onDismissRequest: () -> Unit,
    onFileSystemClick: () -> Unit,
    onSent: () -> Unit
) {
    val viewModel: MediaPickerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasMediaPermission()) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = context.hasMediaPermission()
        
        if (hasPermission) {
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
        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                !hasPermission -> {
                    MediaPickerNotice(
                        text = stringResource(R.string.media_picker_permission),
                        actionText = stringResource(R.string.media_picker_permission_action),
                        onActionClick = { permissionLauncher.launch(mediaPermissions()) })
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
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 2.dp, top = 2.dp, end = 2.dp, bottom = 88.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
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
                    }
                }
            }
            
            HorizontalFloatingToolbar(
                contentPadding = PaddingValues(0.dp),
                expanded = true, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                AnimatedContent(
                    targetState = uiState.selected.isNotEmpty(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "media_picker_toolbar"
                ) { hasSelection ->
                    if (hasSelection) {
                        CaptionRow(
                            caption = uiState.caption,
                            onCaptionChange = viewModel::changeCaption,
                            onSendClick = {
                                viewModel.send(chatId = chatId, replyTo = replyTo)
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
            .size(26.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape)
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
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = caption,
            onValueChange = onCaptionChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp
            ),
            maxLines = 3,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
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
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
            .padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

private const val GRID_COLUMNS = 3
private const val SELECTED_SCALE = 0.9f
