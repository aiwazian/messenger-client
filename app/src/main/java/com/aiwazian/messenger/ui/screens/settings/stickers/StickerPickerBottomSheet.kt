package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.repository.StickerRepository
import com.aiwazian.messenger.ui.app.AppBottomSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val PICKER_CELL_MIN_SIZE = 72.dp
private val PICKER_GRID_MAX_HEIGHT = 420.dp
private const val PRESSED_STICKER_SCALE = 0.85f
private const val ADDED_STICKER_ALPHA = 0.35f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerBottomSheet(
    onStickerSelected: (Sticker) -> Unit,
    onDismissRequest: () -> Unit,
    addedFileIds: Set<String> = emptySet(),
    viewModel: StickerPickerViewModel = hiltViewModel()
) {
    val packs by viewModel.packs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.load()
    }
    
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        when {
            isLoading && packs.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
            
            packs.isEmpty() -> Text(
                text = stringResource(R.string.sticker_packs_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = PICKER_CELL_MIN_SIZE),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PICKER_GRID_MAX_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                packs.forEach { pack ->
                    item(
                        key = "pack_${pack.id}",
                        span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = pack.name,
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 2.dp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    
                    items(
                        items = pack.stickers,
                        key = { "${pack.id}_${it.fileId}" }) { sticker ->
                        StickerPickerCell(
                            sticker = sticker,
                            isAdded = addedFileIds.contains(sticker.fileId),
                            onClick = { onStickerSelected(sticker) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerPickerCell(
    sticker: Sticker,
    isAdded: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_STICKER_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ),
        label = "sticker_picker_scale"
    )
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(sticker.url)
            .memoryCacheKey(sticker.fileId)
            .diskCacheKey(sticker.fileId)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isAdded) ADDED_STICKER_ALPHA else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isAdded,
                onClick = onClick
            ),
        contentScale = ContentScale.Fit
    )
}

@HiltViewModel
class StickerPickerViewModel @Inject constructor(
    private val stickerRepository: StickerRepository
) : ViewModel() {
    
    private val _packs = MutableStateFlow<List<StickerPack>>(emptyList())
    val packs = _packs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private var isLoaded = false
    
    fun load() {
        if (isLoaded) {
            return
        }
        
        isLoaded = true
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val created = stickerRepository.getCreatedPacks()
                .getOrNull()
                .orEmpty()
            
            val added = stickerRepository.getAddedPacks(includeStickers = true)
                .getOrNull()
                .orEmpty()
            
            val merged = (created + added).distinctBy { it.id }
            
            val detailed = merged.map { pack -> async { withStickers(pack) } }
                .awaitAll()
            
            _packs.value = detailed.filter { it.stickers.isNotEmpty() }
            
            _isLoading.value = false
        }
    }
    
    private suspend fun withStickers(pack: StickerPack): StickerPack {
        if (pack.stickers.isNotEmpty()) {
            return pack
        }
        
        return stickerRepository.getPack(pack.id)
            .getOrNull() ?: pack
    }
}
