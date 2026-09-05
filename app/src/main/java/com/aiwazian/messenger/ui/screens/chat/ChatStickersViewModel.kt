package com.aiwazian.messenger.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.repository.StickerRepository
import com.aiwazian.messenger.usecase.SendStickerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatStickersUiState(
    val addedPacks: List<StickerPack> = emptyList(),
    val isPanelVisible: Boolean = false,
    val openedPack: StickerPack? = null,
    val packsById: Map<Long, StickerPack> = emptyMap()
) {
    fun sticker(packId: Long, stickerId: Long): Sticker? =
        packsById[packId]?.stickers?.firstOrNull { it.id == stickerId }
}

@HiltViewModel
class ChatStickersViewModel @Inject constructor(
    private val stickerRepository: StickerRepository,
    private val sendStickerUseCase: SendStickerUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatStickersUiState())
    val uiState = _uiState.asStateFlow()
    
    private val requestedPacks = mutableSetOf<Long>()
    
    fun togglePanel() {
        val isVisible = _uiState.value.isPanelVisible
        
        if (!isVisible) {
            loadAddedPacks()
        }
        
        _uiState.update { it.copy(isPanelVisible = !isVisible) }
    }
    
    fun hidePanel() {
        _uiState.update { it.copy(isPanelVisible = false) }
    }
    
    fun openPackByUsername(username: String) {
        viewModelScope.launch {
            stickerRepository.getPackByUsername(username).onSuccess { pack ->
                _uiState.update {
                    it.copy(
                        openedPack = pack,
                        packsById = it.packsById + (pack.id to pack)
                    )
                }
            }
        }
    }
    
    fun openPack(packId: Long) {
        val cached = _uiState.value.packsById[packId]
        
        if (cached != null) {
            _uiState.update { it.copy(openedPack = cached) }
        }
        
        viewModelScope.launch {
            stickerRepository.getPack(packId).onSuccess { pack ->
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
    
    fun requestPack(packId: Long) {
        if (packId == 0L || !requestedPacks.add(packId)) {
            return
        }
        
        viewModelScope.launch {
            stickerRepository.getPack(packId).onSuccess { pack ->
                _uiState.update { it.copy(packsById = it.packsById + (pack.id to pack)) }
            }.onFailure {
                requestedPacks.remove(packId)
            }
        }
    }
    
    fun installOpenedPack() {
        val pack = _uiState.value.openedPack ?: return
        
        viewModelScope.launch {
            stickerRepository.installPack(pack.id).onSuccess {
                updateInstalled(pack.id, true)
                loadAddedPacks()
            }
        }
    }
    
    fun uninstallOpenedPack() {
        val pack = _uiState.value.openedPack ?: return
        
        viewModelScope.launch {
            stickerRepository.uninstallPack(pack.id).onSuccess {
                updateInstalled(pack.id, false)
                loadAddedPacks()
            }
        }
    }
    
    fun sendSticker(chatId: Long, stickerId: Long) {
        viewModelScope.launch {
            sendStickerUseCase(chatId = chatId, stickerId = stickerId)
        }
    }
    
    private fun updateInstalled(packId: Long, isInstalled: Boolean) {
        _uiState.update { state ->
            val opened = state.openedPack?.takeIf { it.id == packId }?.copy(isInstalled = isInstalled)
            val cached = state.packsById[packId]?.copy(isInstalled = isInstalled)
            
            state.copy(
                openedPack = opened ?: state.openedPack,
                packsById = if (cached == null) state.packsById else state.packsById + (packId to cached)
            )
        }
    }
    
    private fun loadAddedPacks() {
        viewModelScope.launch {
            stickerRepository.getAddedPacks().onSuccess { packs ->
                val detailed = packs.map { pack ->
                    if (pack.stickers.isNotEmpty()) {
                        pack
                    } else {
                        stickerRepository.getPack(pack.id).getOrNull() ?: pack
                    }
                }
                
                _uiState.update { state ->
                    state.copy(
                        addedPacks = detailed,
                        packsById = state.packsById + detailed.associateBy { it.id }
                    )
                }
            }
        }
    }
}
