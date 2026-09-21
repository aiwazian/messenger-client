/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aiwazian.messenger.ui.components.AnimatedStickerImage
import com.aiwazian.messenger.ui.components.isVideoMediaUrl
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_CELL_MIN_SIZE
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_GRID_MAX_HEIGHT
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_GRID_SPACING

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPackBottomSheet(
    pack: EmojiPack,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit
) {
    val context = LocalContext.current
    
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
            columns = GridCells.Adaptive(minSize = EMOJI_CELL_MIN_SIZE),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = EMOJI_GRID_MAX_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING)
        ) {
            items(
                items = pack.emojis,
                key = { it.id }) { emoji ->
                AnimatedStickerImage(
                    data = emoji.url,
                    isVideo = isVideoMediaUrl(emoji.url),
                    cacheKey = emoji.fileId,
                    contentDescription = null,
                    modifier = Modifier.aspectRatio(1f)
                )
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
                    stringResource(R.string.emoji_delete)
                } else {
                    stringResource(R.string.emoji_add)
                }
            )
        }
    }
}
