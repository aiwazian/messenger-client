/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.StickerRepository
import com.aiwazian.messenger.utils.media.EncodedSticker
import com.aiwazian.messenger.utils.media.StickerEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StickerSlot {
    
    val key: String
    
    data class Remote(val fileId: String, val url: String) : StickerSlot {
        override val key: String get() = fileId
    }
    
    data class Local(val sticker: EncodedSticker, val fileId: String? = null) : StickerSlot {
        override val key: String get() = sticker.uri.toString()
    }
}

enum class UsernameStatus {
    Empty,
    TooShort,
    Checking,
    Available,
    Taken,
    Unknown
}

data class StickerPackEditorUiState(
    val packId: Long? = null,
    val name: String = "",
    val username: String = "",
    val stickers: List<StickerSlot> = emptyList(),
    val usernameStatus: UsernameStatus = UsernameStatus.Empty,
    val isLoading: Boolean = false,
    val isAddingSticker: Boolean = false,
    val isSaving: Boolean = false,
    val savedName: String = "",
    val savedUsername: String = "",
    val savedStickerKeys: List<String> = emptyList()
) {
    val isNameValid: Boolean get() = name.trim().isNotEmpty()
    
    val canSave: Boolean
        get() = isNameValid &&
                usernameStatus == UsernameStatus.Available &&
                stickers.isNotEmpty() &&
                !isSaving &&
                !isAddingSticker
    
    val hasChanges: Boolean
        get() = name.trim() != savedName ||
                username != savedUsername ||
                stickers.map { it.key } != savedStickerKeys
}

sealed interface StickerPackEditorEffect {
    data class ShowMessage(
        @param:StringRes val messageRes: Int,
        val undoKey: String? = null
    ) : StickerPackEditorEffect
    
    data object Saved : StickerPackEditorEffect
}

@HiltViewModel
class StickerPackEditorViewModel @Inject constructor(
    private val stickerRepository: StickerRepository,
    private val stickerEncoder: StickerEncoder
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StickerPackEditorUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<StickerPackEditorEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private var usernameJob: Job? = null
    private var isLoaded = false
    private var removedSlot: Pair<Int, StickerSlot>? = null
    
    fun load(packId: Long?, name: String? = null, username: String? = null) {
        if (isLoaded) {
            return
        }
        
        isLoaded = true
        
        val knownName = name?.take(MAX_NAME_LENGTH)
            .orEmpty()
        
        val knownUsername = username?.lowercase()
            ?.take(MAX_USERNAME_LENGTH)
            .orEmpty()
        
        if (packId != null || knownName.isNotEmpty() || knownUsername.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    packId = packId,
                    name = knownName,
                    username = knownUsername,
                    usernameStatus = if (knownUsername.length >= MIN_USERNAME_LENGTH) {
                        UsernameStatus.Available
                    } else {
                        state.usernameStatus
                    },
                    savedName = knownName.trim(),
                    savedUsername = knownUsername
                )
            }
        }
        
        if (packId == null) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            stickerRepository.getPack(packId).onSuccess { pack ->
                val slots = pack.stickers.map { sticker ->
                    StickerSlot.Remote(fileId = sticker.fileId, url = sticker.url)
                }
                
                _uiState.update { state ->
                    state.copy(
                        packId = pack.id,
                        name = pack.name,
                        username = pack.username,
                        usernameStatus = UsernameStatus.Available,
                        stickers = slots,
                        isLoading = false,
                        savedName = pack.name.trim(),
                        savedUsername = pack.username,
                        savedStickerKeys = slots.map { it.key }
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                
                _uiEffect.emit(
                    StickerPackEditorEffect.ShowMessage(R.string.sticker_pack_load_error)
                )
            }
        }
    }
    
    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value.take(MAX_NAME_LENGTH)) }
    }
    
    fun onUsernameChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it in 'a'..'z' || it in 'A'..'Z' || it == '_' }
            .lowercase()
            .take(MAX_USERNAME_LENGTH)
        
        usernameJob?.cancel()
        
        val status = when {
            cleaned.isEmpty() -> UsernameStatus.Empty
            cleaned.length < MIN_USERNAME_LENGTH -> UsernameStatus.TooShort
            else -> UsernameStatus.Checking
        }
        
        _uiState.update { it.copy(username = cleaned, usernameStatus = status) }
        
        if (status != UsernameStatus.Checking) {
            return
        }
        
        usernameJob = viewModelScope.launch {
            delay(USERNAME_CHECK_DELAY_MS)
            
            stickerRepository.isUsernameAvailable(cleaned, _uiState.value.packId)
                .onSuccess { available ->
                    _uiState.update { state ->
                        if (state.username != cleaned) {
                            state
                        } else {
                            state.copy(
                                usernameStatus = if (available) {
                                    UsernameStatus.Available
                                } else {
                                    UsernameStatus.Taken
                                }
                            )
                        }
                    }
                }.onFailure {
                    _uiState.update { state ->
                        if (state.username != cleaned) {
                            state
                        } else {
                            state.copy(usernameStatus = UsernameStatus.Unknown)
                        }
                    }
                }
        }
    }
    
    fun addSticker(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSticker = true) }
            
            val encoded = stickerEncoder.encode(uri)
            
            if (encoded == null) {
                _uiState.update { it.copy(isAddingSticker = false) }
                
                _uiEffect.emit(StickerPackEditorEffect.ShowMessage(R.string.sticker_add_error))
                
                return@launch
            }
            
            _uiState.update { state ->
                state.copy(
                    stickers = state.stickers + StickerSlot.Local(encoded),
                    isAddingSticker = false
                )
            }
        }
    }
    
    fun removeSticker(key: String) {
        val index = _uiState.value.stickers.indexOfFirst { it.key == key }
        
        if (index < 0) {
            return
        }
        
        removedSlot = index to _uiState.value.stickers[index]
        
        _uiState.update { state ->
            state.copy(stickers = state.stickers.filterNot { it.key == key })
        }
        
        viewModelScope.launch {
            _uiEffect.emit(
                StickerPackEditorEffect.ShowMessage(
                    messageRes = R.string.sticker_removed,
                    undoKey = key
                )
            )
        }
    }
    
    fun undoRemove(key: String) {
        val removed = removedSlot ?: return
        
        if (removed.second.key != key) {
            return
        }
        
        removedSlot = null
        
        _uiState.update { state ->
            if (state.stickers.any { it.key == key }) {
                state
            } else {
                val restored = state.stickers.toMutableList()
                
                restored.add(removed.first.coerceIn(0, restored.size), removed.second)
                
                state.copy(stickers = restored)
            }
        }
    }
    
    fun save(exitAfterSave: Boolean = false) {
        val state = _uiState.value
        
        if (!state.canSave) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val slots = state.stickers.toMutableList()
            val fileIds = mutableListOf<String>()
            
            slots.forEachIndexed { index, slot ->
                when (slot) {
                    is StickerSlot.Remote -> fileIds.add(slot.fileId)
                    
                    is StickerSlot.Local -> {
                        val known = slot.fileId
                        
                        if (known != null) {
                            fileIds.add(known)
                        } else {
                            val uploaded =
                                stickerRepository.uploadSticker(slot.sticker).getOrNull()
                            
                            if (uploaded == null) {
                                _uiState.update { current ->
                                    current.copy(stickers = slots, isSaving = false)
                                }
                                
                                _uiEffect.emit(
                                    StickerPackEditorEffect.ShowMessage(R.string.sticker_upload_error)
                                )
                                
                                return@launch
                            }
                            
                            slots[index] = slot.copy(fileId = uploaded)
                            
                            fileIds.add(uploaded)
                        }
                    }
                }
            }
            
            val packId = state.packId
            val name = state.name.trim()
            
            val result = if (packId == null) {
                stickerRepository.createPack(name, state.username, fileIds)
            } else {
                stickerRepository.updatePack(packId, name, state.username, fileIds)
            }
            
            result.onSuccess { pack ->
                val saved = pack.stickers.map { sticker ->
                    StickerSlot.Remote(fileId = sticker.fileId, url = sticker.url)
                }
                
                removedSlot = null
                
                _uiState.update { current ->
                    current.copy(
                        packId = pack.id,
                        name = pack.name,
                        username = pack.username,
                        usernameStatus = UsernameStatus.Available,
                        stickers = saved,
                        isSaving = false,
                        savedName = pack.name.trim(),
                        savedUsername = pack.username,
                        savedStickerKeys = saved.map { it.key }
                    )
                }
                
                if (exitAfterSave) {
                    _uiEffect.emit(StickerPackEditorEffect.Saved)
                } else {
                    _uiEffect.emit(
                        StickerPackEditorEffect.ShowMessage(R.string.sticker_pack_saved)
                    )
                }
            }.onFailure {
                _uiState.update { current -> current.copy(stickers = slots, isSaving = false) }
                
                _uiEffect.emit(
                    StickerPackEditorEffect.ShowMessage(R.string.sticker_pack_save_error)
                )
            }
        }
    }
    
    companion object {
        const val MAX_NAME_LENGTH = 20
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 32
        
        private const val USERNAME_CHECK_DELAY_MS = 400L
    }
}
