package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack

private val PANEL_HEIGHT = 280.dp
private val PANEL_CELL_MIN_SIZE = 72.dp

@Composable
fun StickerInputPanel(
    packs: List<StickerPack>,
    onStickerClick: (Sticker) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = PANEL_CELL_MIN_SIZE),
        modifier = modifier
            .fillMaxWidth()
            .height(PANEL_HEIGHT),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        packs.forEach { pack ->
            item(
                key = "pack-${pack.id}",
                span = { GridItemSpan(maxLineSpan) }) {
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
                key = { "sticker-${it.id}" }) { sticker ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(sticker.url)
                        .memoryCacheKey(sticker.fileId)
                        .diskCacheKey(sticker.fileId)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onStickerClick(sticker) },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
