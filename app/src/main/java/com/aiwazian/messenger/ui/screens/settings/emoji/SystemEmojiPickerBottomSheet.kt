package com.aiwazian.messenger.ui.screens.settings.emoji

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.SystemEmojiRepository
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.utils.EmojiInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PRESSED_SYSTEM_EMOJI_SCALE = 0.85f
private const val SELECTED_SYSTEM_EMOJI_ALPHA = 0.35f
private val SYSTEM_EMOJI_SIZE = 26.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemEmojiPickerBottomSheet(
    selectedEmojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    onEmojiRemoved: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onDismissRequest: () -> Unit,
    viewModel: SystemEmojiPickerViewModel = hiltViewModel()
) {
    val emojis by viewModel.emojis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.load()
    }
    
    AppBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val symbols = EmojiInput.format(selectedEmojis)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = symbols.ifEmpty { stringResource(R.string.emoji_symbols) },
                    modifier = Modifier.weight(1f),
                    color = if (symbols.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
                
                IconButton(
                    onClick = onBackspaceClick,
                    enabled = symbols.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Backspace,
                        contentDescription = null
                    )
                }
            }
            
            if (emojis.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularWavyProgressIndicator()
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = EMOJI_CELL_MIN_SIZE),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = EMOJI_GRID_MAX_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING),
                    verticalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING)
                ) {
                    items(
                        items = emojis,
                        key = { it }) { emoji ->
                        val isSelected = selectedEmojis.contains(emoji)
                        
                        SystemEmojiCell(
                            emoji = emoji,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    onEmojiRemoved(emoji)
                                } else {
                                    onEmojiSelected(emoji)
                                }
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemEmojiCell(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SYSTEM_EMOJI_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ),
        label = "system_emoji_picker_scale"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isSelected) SELECTED_SYSTEM_EMOJI_ALPHA else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = SYSTEM_EMOJI_SIZE
        )
    }
}

@HiltViewModel
class SystemEmojiPickerViewModel @Inject constructor(
    private val systemEmojiRepository: SystemEmojiRepository
) : ViewModel() {
    
    private val _emojis = MutableStateFlow<List<String>>(emptyList())
    val emojis = _emojis.asStateFlow()
    
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
            
            val loaded = systemEmojiRepository.getEmojis()
            
            if (loaded.isEmpty()) {
                isLoaded = false
            }
            
            _emojis.value = loaded
            _isLoading.value = false
        }
    }
}
