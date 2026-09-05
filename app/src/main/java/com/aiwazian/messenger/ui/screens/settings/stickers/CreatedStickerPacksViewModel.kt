package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.repository.StickerRepository
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.usecase.GetShareTargetsUseCase
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.utils.StickerLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StickerPackListUiState(
    val packs: List<StickerPack> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val sharingPack: StickerPack? = null,
    val shareTargets: List<ShareItem> = emptyList(),
    val selectedShareChatIds: Set<Long> = emptySet()
) {
    val visiblePacks: List<StickerPack>
        get() {
            val trimmed = query.trim()
            
            if (trimmed.isEmpty()) {
                return packs
            }
            
            return packs.filter { pack ->
                pack.name.contains(trimmed, ignoreCase = true) ||
                        pack.username.contains(trimmed, ignoreCase = true)
            }
        }
}

sealed interface StickerPackListEffect {
    data class ShowMessage(@param:StringRes val messageRes: Int) : StickerPackListEffect
}

@HiltViewModel
class CreatedStickerPacksViewModel @Inject constructor(
    private val stickerRepository: StickerRepository,
    private val getShareTargetsUseCase: GetShareTargetsUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StickerPackListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<StickerPackListEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            stickerRepository.getCreatedPacks().onSuccess { packs ->
                _uiState.update { it.copy(packs = packs, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                
                _uiEffect.emit(StickerPackListEffect.ShowMessage(R.string.sticker_packs_load_error))
            }
        }
    }
    
    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }
    
    fun share(pack: StickerPack) {
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
            val link = StickerLink.build(pack.username)
            
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
            stickerRepository.deletePack(packId).onSuccess {
                _uiState.update { state ->
                    state.copy(packs = state.packs.filterNot { it.id == packId })
                }
                
                _uiEffect.emit(StickerPackListEffect.ShowMessage(R.string.sticker_pack_deleted))
            }.onFailure {
                _uiEffect.emit(StickerPackListEffect.ShowMessage(R.string.sticker_pack_delete_error))
            }
        }
    }
}
