/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.annotation.StringRes
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

data class StickerPackListUiState(
    val packs: List<StickerPack> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false
) {
    /** Поиск идёт по уже загруженному списку: наборов мало, сеть тут не нужна. */
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

/** Собственные наборы пользователя. */
@HiltViewModel
class CreatedStickerPacksViewModel @Inject constructor(
    private val stickerRepository: StickerRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StickerPackListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<StickerPackListEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    /**
     * Перечитывает список.
     *
     * Вызывается и при возврате с редактора: только что созданный набор должен
     * сразу появиться в списке.
     */
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
    
    /** Удаляет набор у всех: возврата нет, поэтому вызов только после подтверждения. */
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
