package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction

private val STICKER_MESSAGE_SIZE = 180.dp
private const val FOOTER_SCRIM_ALPHA = 0.2f

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
    
    val interactionSource = remember { MutableInteractionSource() }
    
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
            
            Box(modifier = Modifier.size(STICKER_MESSAGE_SIZE)) {
                if (sticker != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(sticker.url)
                            .memoryCacheKey(sticker.fileId)
                            .diskCacheKey(sticker.fileId)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onLongClick = { isMenuExpanded = true },
                                onClick = onStickerClick
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
                
                StickerMessageFooter(
                    time = time,
                    isRead = isRead,
                    status = status,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
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
private fun StickerMessageFooter(
    time: String,
    isRead: Boolean?,
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = FOOTER_SCRIM_ALPHA))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            color = Color.White
        )
        
        when (status) {
            MessageStatus.SENDING -> {
                Spacer(modifier = Modifier.size(4.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(8.dp),
                    strokeWidth = 1.dp,
                    color = Color.White
                )
            }
            
            MessageStatus.ERROR -> {
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            MessageStatus.SENT -> {
                if (isRead != null) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        imageVector = if (isRead) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }
        }
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
