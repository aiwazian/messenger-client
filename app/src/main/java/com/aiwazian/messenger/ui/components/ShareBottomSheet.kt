/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.utils.UiText

data class ShareItem(
    val id: Long,
    val name: UiText,
    val isSelected: Boolean,
    val avatarUri: Uri? = null,
    val isSavedMessages: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    items: List<ShareItem>,
    onItemClick: (Long) -> Unit,
    onSendClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val hasSelected = remember(items) { items.any { it.isSelected } }
    val visibleItems = remember(items, searchQuery, context) {
        val ordered = items.sortedByDescending { it.isSavedMessages }
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            ordered
        } else {
            ordered.filter { it.name.asString(context).contains(query, ignoreCase = true) }
        }
    }
    
    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentPadding = PaddingValues()
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                state = rememberLazyGridState(),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stickyHeader {
                    val interactionSource = remember { MutableInteractionSource() }
                    
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                sheetState.expand()
                            }
                        }
                    }
                    
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = searchQuery.isNotEmpty(),
                                enter = expressiveScaleIn,
                                exit = expressiveScaleOut
                            ) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.statusBarsPadding(),
                        interactionSource = interactionSource
                    )
                }
                
                items(visibleItems, key = { it.id }) { item ->
                    ShareChatCard(item) {
                        onItemClick(item.id)
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .imePadding()
                    .offset { IntOffset(x = 0, y = -sheetState.requireOffset().toInt()) }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(0.8f),
                            )
                        )
                    )
            )
            
            androidx.compose.animation.AnimatedVisibility(
                visible = hasSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                TextButton(
                    onClick = onSendClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(10.dp)
                        .offset { IntOffset(x = 0, y = -sheetState.requireOffset().toInt()) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text(stringResource(R.string.send))
                }
            }
        }
    }
}

@Composable
fun ShareChatCard(item: ShareItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                val borderColor by animateColorAsState(targetValue = if (item.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .border(width = 2.dp, color = borderColor, shape = CircleShape)
                )
                
                val avatarSize by animateDpAsState(
                    targetValue = if (item.isSelected) 40.dp else 50.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                
                if (item.isSavedMessages) {
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(avatarSize * 0.56f)
                        )
                    }
                } else {
                    ChatAvatar(
                        id = item.id,
                        chatName = item.name.asString(),
                        avatarUri = item.avatarUri,
                        size = avatarSize
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = item.isSelected,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enter = expressiveScaleIn,
                    exit = expressiveScaleOut
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = item.name.asString(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 12.sp,
            )
        }
    }
}
