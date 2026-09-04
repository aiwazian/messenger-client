/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.repository.StickerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Чужие наборы, добавленные себе. */
@HiltViewModel
class AddedStickerPacksViewModel @Inject constructor(
    private val stickerRepository: StickerRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StickerPackListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<StickerPackListEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    /**
     * Набор, раскрытый в шторке.
     *
     * Состав грузится отдельным запросом: в списке сервер отдаёт только
     * количество стикеров.
     */
    private val _openedPack = MutableStateFlow<StickerPack?>(null)
    val openedPack = _openedPack.asStateFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            stickerRepository.getAddedPacks().onSuccess { packs ->
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
    
    fun open(packId: Long) {
        viewModelScope.launch {
            stickerRepository.getPack(packId).onSuccess { pack ->
                _openedPack.value = pack
            }.onFailure {
                _uiEffect.emit(StickerPackListEffect.ShowMessage(R.string.sticker_pack_load_error))
            }
        }
    }
    
    fun close() {
        _openedPack.value = null
    }
    
    /** Убирает набор только у себя: чужой набор удалять нельзя. */
    fun remove(packId: Long) {
        viewModelScope.launch {
            stickerRepository.uninstallPack(packId).onSuccess {
                _uiState.update { state ->
                    state.copy(packs = state.packs.filterNot { it.id == packId })
                }
                
                _uiEffect.emit(StickerPackListEffect.ShowMessage(R.string.sticker_pack_removed))
            }.onFailure {
                _uiEffect.emit(StickerPackListEffect.ShowMessage(R.string.sticker_pack_remove_error))
            }
        }
    }
}
