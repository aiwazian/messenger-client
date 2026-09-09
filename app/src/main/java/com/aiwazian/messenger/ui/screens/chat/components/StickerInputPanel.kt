package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.ui.app.AppPrimaryScrollableTabRow
import kotlinx.coroutines.launch

private const val PANEL_GRID_COLUMNS = 5
private const val INACTIVE_LOGO_ALPHA = 0.5f
private val PACK_LOGO_SIZE = 30.dp

@Composable
fun StickerInputPanel(
    packs: List<StickerPack>,
    height: Dp,
    onStickerClick: (Sticker) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    
    val headerIndices = remember(packs) {
        var nextIndex = 0
        
        packs.map { pack ->
            val headerIndex = nextIndex
            
            nextIndex += pack.stickers.size + 1
            
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
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (packs.isNotEmpty()) {
            AppPrimaryScrollableTabRow(
                selectedTabIndex = selectedPackIndex.coerceAtMost(packs.lastIndex)
            ) {
                packs.forEachIndexed { index, pack ->
                    StickerPackTab(
                        pack = pack,
                        isSelected = index == selectedPackIndex,
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
            columns = GridCells.Fixed(PANEL_GRID_COLUMNS),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            packs.forEach { pack ->
                item(key = "pack-${pack.id}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = pack.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                items(
                    items = pack.stickers,
                    key = { sticker -> "sticker-${pack.id}-${sticker.id}" }) { sticker ->
                    val interactionSource = remember { MutableInteractionSource() }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(sticker.url)
                            .memoryCacheKey(sticker.fileId)
                            .diskCacheKey(sticker.fileId)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onStickerClick(sticker) },
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
private fun StickerPackTab(
    pack: StickerPack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else INACTIVE_LOGO_ALPHA,
        label = "sticker_pack_tab_alpha"
    )
    
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(pack.coverImageUrl)
                    .memoryCacheKey(pack.coverCacheKey)
                    .diskCacheKey(pack.coverCacheKey)
                    .build(),
                contentDescription = pack.name,
                modifier = Modifier
                    .size(PACK_LOGO_SIZE)
                    .clip(MaterialTheme.shapes.small)
                    .alpha(logoAlpha),
                contentScale = ContentScale.Fit
            )
        }
    }
}
