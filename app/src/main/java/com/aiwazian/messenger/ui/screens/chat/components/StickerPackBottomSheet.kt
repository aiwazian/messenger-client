/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem

private const val SHEET_GRID_COLUMNS = 5
private val SHEET_GRID_MAX_HEIGHT = 380.dp
private const val PRESSED_CELL_SCALE = 0.9f
private const val SCRIM_ALPHA = 0.6f
private const val FOCUS_SIZE_FRACTION = 0.62f
private val FOCUS_OPEN_SPEC = tween<Float>(260, easing = FastOutSlowInEasing)
private val FOCUS_CLOSE_SPEC = tween<Float>(220, easing = FastOutSlowInEasing)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPackBottomSheet(
    pack: StickerPack,
    onDismiss: () -> Unit,
    onSendSticker: (Sticker) -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit
) {
    var focusedSticker by remember { mutableStateOf<Sticker?>(null) }
    var focusedBounds by remember { mutableStateOf(Rect.Zero) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    AppBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pack.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                AppDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }) {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.share),
                        onClick = {
                            isMenuExpanded = false
                            onShare()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null
                            )
                        })
                    
                    AppDropdownMenuItem(
                        text = stringResource(R.string.copy_link),
                        onClick = {
                            isMenuExpanded = false
                            onCopyLink()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null
                            )
                        })
                }
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(SHEET_GRID_COLUMNS),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = SHEET_GRID_MAX_HEIGHT),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = pack.stickers,
                key = { it.id }) { sticker ->
                StickerGridCell(
                    sticker = sticker,
                    isHidden = focusedSticker?.id == sticker.id,
                    onClick = { bounds ->
                        focusedBounds = bounds
                        focusedSticker = sticker
                    })
            }
        }
        
        TextButton(
            onClick = if (pack.isInstalled) onUninstall else onInstall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (pack.isInstalled) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Text(
                text = if (pack.isInstalled) {
                    stringResource(R.string.sticker_pack_remove_stickers)
                } else {
                    stringResource(R.string.sticker_pack_add_stickers)
                }
            )
        }
    }
    
    focusedSticker?.let { sticker ->
        StickerFocusOverlay(
            sticker = sticker,
            origin = focusedBounds,
            onClose = { focusedSticker = null },
            onSend = {
                focusedSticker = null
                onSendSticker(sticker)
            })
    }
}

@Composable
private fun StickerGridCell(
    sticker: Sticker,
    isHidden: Boolean,
    onClick: (Rect) -> Unit
) {
    val context = LocalContext.current
    
    var bounds by remember { mutableStateOf(Rect.Zero) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) PRESSED_CELL_SCALE else 1f)
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(sticker.url)
            .memoryCacheKey(sticker.fileId)
            .diskCacheKey(sticker.fileId)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(1f)
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isHidden) 0f else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick(bounds) },
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun StickerFocusOverlay(
    sticker: Sticker,
    origin: Rect,
    onClose: () -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val progress = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        progress.animateTo(1f, FOCUS_OPEN_SPEC)
    }
    
    LaunchedEffect(isClosing) {
        if (isClosing) {
            progress.animateTo(0f, FOCUS_CLOSE_SPEC)
            onClose()
        }
    }
    
    Dialog(
        onDismissRequest = { isClosing = true },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0f)
        }
        
        val value = progress.value
        val interactionSource = remember { MutableInteractionSource() }
        
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA * value))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { isClosing = true },
            contentAlignment = Alignment.Center
        ) {
            val targetSize =
                (if (maxWidth < maxHeight) maxWidth else maxHeight) * FOCUS_SIZE_FRACTION
            val targetSizePx = with(density) { targetSize.toPx() }
            val centerX = with(density) { maxWidth.toPx() } / 2f
            val centerY = with(density) { maxHeight.toPx() } / 2f
            val startScale = if (targetSizePx > 0f && origin.width > 0f) {
                origin.width / targetSizePx
            } else {
                PRESSED_CELL_SCALE
            }
            val scale = startScale + (1f - startScale) * value
            val translationX =
                if (origin.width > 0f) (origin.center.x - centerX) * (1f - value) else 0f
            val translationY =
                if (origin.width > 0f) (origin.center.y - centerY) * (1f - value) else 0f
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = sticker.emojis.joinToString(" "),
                    modifier = Modifier.graphicsLayer { alpha = value },
                    color = Color.White,
                    fontSize = 24.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(sticker.url)
                        .memoryCacheKey(sticker.fileId)
                        .diskCacheKey(sticker.fileId)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(targetSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.translationX = translationX
                            this.translationY = translationY
                        },
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onSend,
                    modifier = Modifier.graphicsLayer { alpha = value },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text(text = stringResource(R.string.send_sticker))
                }
            }
        }
    }
}
