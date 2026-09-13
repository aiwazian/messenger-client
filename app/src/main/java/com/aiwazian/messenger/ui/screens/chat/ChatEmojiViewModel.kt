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
    val packsById: Map<Long, EmojiPack> = emptyMap(),
    val openedPack: EmojiPack? = null
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
    
    fun openPackByUsername(username: String) {
        viewModelScope.launch {
            emojiRepository.getPackByUsername(username).onSuccess { pack ->
                _uiState.update {
                    it.copy(
                        openedPack = pack,
                        packsById = it.packsById + (pack.id to pack)
                    )
                }
            }
        }
    }
    
    fun closePack() {
        _uiState.update { it.copy(openedPack = null) }
    }
    
    fun installOpenedPack() {
        val pack = _uiState.value.openedPack ?: return
        
        viewModelScope.launch {
            emojiRepository.installPack(pack.id).onSuccess {
                updateInstalled(pack.id, true)
                loadAddedPacks()
                
                _uiState.update { it.copy(openedPack = null) }
            }
        }
    }
    
    fun uninstallOpenedPack() {
        val pack = _uiState.value.openedPack ?: return
        
        viewModelScope.launch {
            emojiRepository.uninstallPack(pack.id).onSuccess {
                updateInstalled(pack.id, false)
                loadAddedPacks()
                
                _uiState.update { it.copy(openedPack = null) }
            }
        }
    }
    
    private fun updateInstalled(packId: Long, isInstalled: Boolean) {
        _uiState.update { state ->
            val opened =
                state.openedPack?.takeIf { it.id == packId }?.copy(isInstalled = isInstalled)
            val cached = state.packsById[packId]?.copy(isInstalled = isInstalled)
            
            state.copy(
                openedPack = opened ?: state.openedPack,
                packsById = if (cached == null) {
                    state.packsById
                } else {
                    state.packsById + (packId to cached)
                }
            )
        }
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
