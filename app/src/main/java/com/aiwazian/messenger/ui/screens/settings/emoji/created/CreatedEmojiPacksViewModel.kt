package com.aiwazian.messenger.ui.screens.settings.emoji.created

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.ui.screens.settings.emoji.EmojiPackListEffect
import com.aiwazian.messenger.ui.screens.settings.emoji.EmojiPackListUiState
import com.aiwazian.messenger.usecase.GetShareTargetsUseCase
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.utils.EmojiLink
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
    private val emojiRepository: EmojiRepository,
    private val getShareTargetsUseCase: GetShareTargetsUseCase,
    private val sendMessageUseCase: SendMessageUseCase
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
    
    fun share(pack: EmojiPack) {
        viewModelScope.launch {
            val targets = getShareTargetsUseCase()
            
            _uiState.update {
                it.copy(
                    sharingPack = pack,
                    shareTargets = targets,
                    selectedShareChatIds = emptySet()
                )
            }
        }
    }
    
    fun toggleShareTarget(chatId: Long) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedShareChatIds.toMutableSet()
            
            if (!selected.add(chatId)) {
                selected.remove(chatId)
            }
            
            val targets = getShareTargetsUseCase(selected)
            
            _uiState.update {
                it.copy(
                    selectedShareChatIds = selected,
                    shareTargets = targets
                )
            }
        }
    }
    
    fun sendShare() {
        val state = _uiState.value
        val pack = state.sharingPack ?: return
        val targets = state.selectedShareChatIds
        
        if (targets.isEmpty()) {
            return
        }
        
        dismissShare()
        
        viewModelScope.launch {
            val link = EmojiLink.build(pack.username)
            
            targets.forEach { chatId ->
                sendMessageUseCase(chatId = chatId, message = link)
            }
        }
    }
    
    fun dismissShare() {
        _uiState.update {
            it.copy(
                sharingPack = null,
                shareTargets = emptyList(),
                selectedShareChatIds = emptySet()
            )
        }
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
