/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.EmojiPack
import kotlinx.coroutines.launch

private val PANEL_EMOJI_MIN_SIZE = 40.dp
private val EMOJI_PACK_LOGO_SIZE = 30.dp

@Composable
fun EmojiInputPanel(
    packs: List<EmojiPack>,
    onEmojiClick: (EmojiPack, CustomEmoji) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    
    val headerIndices = remember(packs) {
        var nextIndex = 0
        
        packs.map { pack ->
            val headerIndex = nextIndex
            
            nextIndex += pack.emojis.size + 1
            
            headerIndex
        }
    }
    
    var pendingPackIndex by remember(packs) { mutableStateOf<Int?>(null) }
    
    val visiblePackIndex by remember(headerIndices) {
        derivedStateOf {
            headerIndices.indexOfLast { it <= gridState.firstVisibleItemIndex }
        }
    }
    
    val selectedPackIndex = (pendingPackIndex ?: visiblePackIndex).coerceAtLeast(0)
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (packs.isNotEmpty()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedPackIndex,
                modifier = Modifier.zIndex(1f),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                edgePadding = 4.dp,
                indicator = {},
                divider = {},
                minTabWidth = 0.dp
            ) {
                packs.forEachIndexed { index, pack ->
                    EmojiPackTab(
                        pack = pack,
                        onClick = {
                            scope.launch {
                                pendingPackIndex = index
                                
                                gridState.animateScrollToItem(headerIndices[index])
                                
                                pendingPackIndex = null
                            }
                        })
                }
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(PANEL_EMOJI_MIN_SIZE),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 4.dp, top = 40.dp, end = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            packs.forEach { pack ->
                item(key = "pack-${pack.id}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = pack.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                items(
                    items = pack.emojis,
                    key = { emoji -> "emoji-${pack.id}-${emoji.id}" }) { emoji ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        targetValue = if (isPressed) 0.9f else 1f,
                        label = "custom_emoji_button_scale_animation"
                    )
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(emoji.url)
                            .memoryCacheKey(emoji.fileId)
                            .diskCacheKey(emoji.fileId)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .aspectRatio(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onEmojiClick(pack, emoji) },
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun EmojiPackTab(
    pack: EmojiPack,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.96f else 1f,
        label = "app_tab_scale_animation"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .zIndex(1f)
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(pack.coverImageUrl)
                .memoryCacheKey(pack.coverCacheKey)
                .diskCacheKey(pack.coverCacheKey)
                .build(),
            contentDescription = pack.name,
            modifier = Modifier
                .size(EMOJI_PACK_LOGO_SIZE)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Fit
        )
    }
}
