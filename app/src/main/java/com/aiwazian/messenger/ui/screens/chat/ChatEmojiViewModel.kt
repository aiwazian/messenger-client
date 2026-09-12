package com.aiwazian.messenger.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.repository.EmojiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatEmojiUiState(
    val addedPacks: List<EmojiPack> = emptyList(),
    val packsById: Map<Long, EmojiPack> = emptyMap()
)

@HiltViewModel
class ChatEmojiViewModel @Inject constructor(
    private val emojiRepository: EmojiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatEmojiUiState())
    val uiState = _uiState.asStateFlow()
    
    private var isAddedPacksRequested = false
    
    fun preloadPacks() {
        if (isAddedPacksRequested) {
            return
        }
        
        loadAddedPacks()
    }
    
    private fun loadAddedPacks() {
        isAddedPacksRequested = true
        
        viewModelScope.launch {
            emojiRepository.getAddedPacks(includeEmojis = true).onSuccess { packs ->
                val detailed = packs.map { pack ->
                    if (pack.emojis.isNotEmpty()) {
                        pack
                    } else {
                        emojiRepository.getPack(pack.id).getOrNull() ?: pack
                    }
                }
                
                _uiState.update { state ->
                    state.copy(
                        addedPacks = detailed,
                        packsById = state.packsById + detailed.associateBy { it.id }
                    )
                }
            }.onFailure {
                isAddedPacksRequested = false
            }
        }
    }
}
