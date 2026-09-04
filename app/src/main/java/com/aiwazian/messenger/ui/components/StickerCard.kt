/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.extensions.clickableWithoutRipple

/**
 * Карточка набора в списке.
 *
 * @param deleteMessage текст подтверждения: у своих наборов удаление затрагивает всех,
 * у добавленных — только самого пользователя.
 */
@Composable
fun StickerCard(
    pack: StickerPack,
    deleteMessage: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isDeleteDialogVisible by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { Text(pack.name) },
        modifier = modifier.clickableWithoutRipple(onClick = onClick),
        supportingContent = {
            Text(
                pluralStringResource(
                    R.plurals.sticker_count,
                    pack.stickerCount,
                    pack.stickerCount
                )
            )
        },
        leadingContent = {
            val cover = pack.coverSticker
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (cover == null) {
                    Icon(
                        imageVector = Icons.Rounded.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(cover.url)
                            .memoryCacheKey(cover.fileId)
                            .diskCacheKey(cover.fileId)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null
                    )
                }
                
                AppDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }) {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.delete),
                        onClick = {
                            isMenuExpanded = false
                            isDeleteDialogVisible = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null
                            )
                        })
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )
    
    if (isDeleteDialogVisible) {
        AppDialog(
            title = pack.name,
            onDismissRequest = { isDeleteDialogVisible = false },
            buttons = {
                TextButton(onClick = { isDeleteDialogVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
                
                TextButton(onClick = {
                    isDeleteDialogVisible = false
                    
                    onDelete()
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }) {
            Text(deleteMessage)
        }
    }
}
