package com.aiwazian.messenger.ui.screens.settings.emoji.added

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.ui.screens.settings.emoji.EmojiPackListEffect
import com.aiwazian.messenger.ui.screens.settings.emoji.EmojiPackListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddedEmojiPacksViewModel @Inject constructor(
    private val emojiRepository: EmojiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EmojiPackListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<EmojiPackListEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val _openedPack = MutableStateFlow<EmojiPack?>(null)
    val openedPack = _openedPack.asStateFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            emojiRepository.getAddedPacks().onSuccess { packs ->
                _uiState.update { it.copy(packs = packs, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                
                _uiEffect.emit(EmojiPackListEffect.ShowMessage(R.string.emoji_packs_load_error))
            }
        }
    }
    
    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }
    
    fun open(packId: Long) {
        viewModelScope.launch {
            emojiRepository.getPack(packId).onSuccess { pack ->
                _openedPack.value = pack
            }.onFailure {
                _uiEffect.emit(EmojiPackListEffect.ShowMessage(R.string.emoji_pack_load_error))
            }
        }
    }
    
    fun close() {
        _openedPack.value = null
    }
    
    fun remove(packId: Long) {
        viewModelScope.launch {
            emojiRepository.uninstallPack(packId).onSuccess {
                _uiState.update { state ->
                    state.copy(packs = state.packs.filterNot { it.id == packId })
                }
                
                _uiEffect.emit(EmojiPackListEffect.ShowMessage(R.string.emoji_pack_removed))
            }.onFailure {
                _uiEffect.emit(EmojiPackListEffect.ShowMessage(R.string.emoji_pack_remove_error))
            }
        }
    }
}
