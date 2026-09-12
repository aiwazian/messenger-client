package com.aiwazian.messenger.ui.screens.settings.emoji.created

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
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
class CreatedEmojiPacksViewModel @Inject constructor(
    private val emojiRepository: EmojiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EmojiPackListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<EmojiPackListEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            emojiRepository.getCreatedPacks().onSuccess { packs ->
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
    
    fun delete(packId: Long) {
        viewModelScope.launch {
            emojiRepository.deletePack(packId).onSuccess {
                _uiState.update { state ->
                    state.copy(packs = state.packs.filterNot { it.id == packId })
                }
                
                _uiEffect.emit(EmojiPackListEffect.ShowMessage(R.string.emoji_pack_deleted))
            }.onFailure {
                _uiEffect.emit(EmojiPackListEffect.ShowMessage(R.string.emoji_pack_delete_error))
            }
        }
    }
}
