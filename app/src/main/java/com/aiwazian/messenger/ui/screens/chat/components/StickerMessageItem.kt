package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction

private val STICKER_MESSAGE_SIZE = 140.dp

@Composable
fun StickerMessageItem(
    sticker: Sticker?,
    time: String,
    isMine: Boolean,
    isRead: Boolean?,
    status: MessageStatus,
    actions: List<DropdownMenuAction>,
    onStickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (isMine) {
                EmptyMenuArea(onClick = { isMenuExpanded = true })
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (sticker == null) {
                    Box(modifier = Modifier.size(STICKER_MESSAGE_SIZE))
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(sticker.url)
                            .memoryCacheKey(sticker.fileId)
                            .diskCacheKey(sticker.fileId)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(STICKER_MESSAGE_SIZE)
                            .combinedClickable(
                                onClick = onStickerClick,
                                onLongClick = { isMenuExpanded = true }),
                        contentScale = ContentScale.Fit
                    )
                }
                
                MessageFooter(
                    time = time,
                    isRead = isRead,
                    status = status
                )
            }
            
            if (!isMine) {
                EmptyMenuArea(onClick = { isMenuExpanded = true })
            }
        }
        
        MessageDropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            actions = actions
        )
    }
}

@Composable
private fun RowScope.EmptyMenuArea(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .weight(1f)
            .height(STICKER_MESSAGE_SIZE)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}
