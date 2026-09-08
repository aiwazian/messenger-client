package com.aiwazian.messenger.ui.screens.settings.stickers.created

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerDraft
import com.aiwazian.messenger.repository.StickerRepository
import com.aiwazian.messenger.utils.EmojiInput
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
    
    val emojis: List<String>
    
    fun withEmojis(emojis: List<String>): StickerSlot
    
    data class Remote(
        val fileId: String,
        val url: String,
        override val emojis: List<String> = emptyList()
    ) : StickerSlot {
        override val key: String get() = fileId
        
        override fun withEmojis(emojis: List<String>): StickerSlot = copy(emojis = emojis)
    }
    
    data class Local(
        val sticker: EncodedSticker,
        override val emojis: List<String> = emptyList(),
        val fileId: String? = null
    ) : StickerSlot {
        override val key: String get() = sticker.uri.toString()
        
        override fun withEmojis(emojis: List<String>): StickerSlot = copy(emojis = emojis)
    }
}

sealed interface StickerPackCover {
    
    val key: String
    
    data class Remote(
        val fileId: String,
        val url: String
    ) : StickerPackCover {
        override val key: String get() = fileId
    }
    
    data class Local(
        val sticker: EncodedSticker,
        val fileId: String? = null
    ) : StickerPackCover {
        override val key: String get() = sticker.uri.toString()
    }
}

data class StickerSnapshot(
    val key: String,
    val emojis: List<String>
)

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
    val cover: StickerPackCover? = null,
    val focusedStickerKey: String? = null,
    val usernameStatus: UsernameStatus = UsernameStatus.Empty,
    val isLoading: Boolean = false,
    val isAddingSticker: Boolean = false,
    val isChangingCover: Boolean = false,
    val isSaving: Boolean = false,
    val savedName: String = "",
    val savedUsername: String = "",
    val savedCoverKey: String? = null,
    val savedStickers: List<StickerSnapshot> = emptyList()
) {
    val isNameValid: Boolean get() = name.trim().isNotEmpty()
    
    val focusedSticker: StickerSlot?
        get() = stickers.firstOrNull { it.key == focusedStickerKey }
    
    val canSave: Boolean
        get() = isNameValid &&
                usernameStatus == UsernameStatus.Available &&
                stickers.isNotEmpty() &&
                stickers.all { it.emojis.isNotEmpty() } &&
                !isSaving &&
                !isAddingSticker &&
                !isChangingCover
    
    val hasChanges: Boolean
        get() = name.trim() != savedName ||
                username != savedUsername ||
                cover?.key != savedCoverKey ||
                stickers.map { StickerSnapshot(it.key, it.emojis) } != savedStickers
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
                    StickerSlot.Remote(
                        fileId = sticker.fileId,
                        url = sticker.url,
                        emojis = sticker.emojis
                    )
                }
                
                val coverFileId = pack.coverFileId
                val coverUrl = pack.coverUrl
                
                val cover = if (coverFileId != null && coverUrl != null) {
                    StickerPackCover.Remote(fileId = coverFileId, url = coverUrl)
                } else {
                    null
                }
                
                _uiState.update { state ->
                    state.copy(
                        packId = pack.id,
                        name = pack.name,
                        username = pack.username,
                        usernameStatus = UsernameStatus.Available,
                        stickers = slots,
                        cover = cover,
                        isLoading = false,
                        savedName = pack.name.trim(),
                        savedUsername = pack.username,
                        savedCoverKey = cover?.key,
                        savedStickers = slots.map { StickerSnapshot(it.key, it.emojis) }
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
            
            val slot = StickerSlot.Local(
                sticker = encoded,
                emojis = listOf(EmojiInput.DEFAULT_EMOJI)
            )
            
            _uiState.update { state ->
                state.copy(
                    stickers = state.stickers + slot,
                    focusedStickerKey = slot.key,
                    isAddingSticker = false
                )
            }
        }
    }
    
    fun addStickerFromExisting(sticker: Sticker) {
        val state = _uiState.value
        
        if (state.stickers.any { it is StickerSlot.Remote && it.fileId == sticker.fileId }) {
            return
        }
        
        val slot = StickerSlot.Remote(
            fileId = sticker.fileId,
            url = sticker.url,
            emojis = sticker.emojis.ifEmpty { listOf(EmojiInput.DEFAULT_EMOJI) }
        )
        
        _uiState.update { current ->
            current.copy(stickers = current.stickers + slot)
        }
    }
    
    fun setCoverFromFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingCover = true) }
            
            val encoded = stickerEncoder.encode(uri)
            
            if (encoded == null) {
                _uiState.update { it.copy(isChangingCover = false) }
                
                _uiEffect.emit(StickerPackEditorEffect.ShowMessage(R.string.sticker_add_error))
                
                return@launch
            }
            
            _uiState.update { state ->
                state.copy(
                    cover = StickerPackCover.Local(sticker = encoded),
                    isChangingCover = false
                )
            }
        }
    }
    
    fun setCoverFromSticker(sticker: Sticker) {
        _uiState.update { state ->
            state.copy(
                cover = StickerPackCover.Remote(
                    fileId = sticker.fileId,
                    url = sticker.url
                )
            )
        }
    }
    
    fun focusSticker(key: String) {
        _uiState.update { state ->
            if (state.stickers.none { it.key == key }) {
                state
            } else {
                state.copy(focusedStickerKey = key)
            }
        }
    }
    
    fun clearFocus() {
        _uiState.update { it.copy(focusedStickerKey = null) }
    }
    
    fun onStickerEmojisChange(key: String, value: String) {
        val emojis = EmojiInput.parse(value)
        
        _uiState.update { state ->
            state.copy(
                stickers = state.stickers.map { slot ->
                    if (slot.key == key) {
                        slot.withEmojis(emojis)
                    } else {
                        slot
                    }
                }
            )
        }
    }
    
    fun removeSticker(key: String) {
        val index = _uiState.value.stickers.indexOfFirst { it.key == key }
        
        if (index < 0) {
            return
        }
        
        removedSlot = index to _uiState.value.stickers[index]
        
        _uiState.update { state ->
            state.copy(
                stickers = state.stickers.filterNot { it.key == key },
                focusedStickerKey = if (state.focusedStickerKey == key) {
                    null
                } else {
                    state.focusedStickerKey
                }
            )
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
            
            val existingPackId = state.packId
            
            val packId = existingPackId ?: stickerRepository.reservePackId()
                .getOrNull()
            
            if (packId == null) {
                _uiState.update { current -> current.copy(isSaving = false) }
                
                _uiEffect.emit(
                    StickerPackEditorEffect.ShowMessage(R.string.sticker_pack_save_error)
                )
                
                return@launch
            }
            
            val slots = state.stickers.toMutableList()
            val drafts = mutableListOf<StickerDraft>()
            
            slots.forEachIndexed { index, slot ->
                when (slot) {
                    is StickerSlot.Remote -> drafts.add(
                        StickerDraft(fileId = slot.fileId, emojis = slot.emojis)
                    )
                    
                    is StickerSlot.Local -> {
                        val known = slot.fileId
                        
                        if (known != null) {
                            drafts.add(StickerDraft(fileId = known, emojis = slot.emojis))
                        } else {
                            val uploaded = stickerRepository.uploadSticker(slot.sticker, packId)
                                .getOrNull()
                            
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
                            
                            drafts.add(StickerDraft(fileId = uploaded, emojis = slot.emojis))
                        }
                    }
                }
            }
            
            var cover = state.cover
            
            when (val current = cover) {
                is StickerPackCover.Local -> {
                    val known = current.fileId
                    
                    if (known == null) {
                        val uploaded = stickerRepository.uploadSticker(current.sticker, packId)
                            .getOrNull()
                        
                        if (uploaded == null) {
                            _uiState.update { state ->
                                state.copy(stickers = slots, isSaving = false)
                            }
                            
                            _uiEffect.emit(
                                StickerPackEditorEffect.ShowMessage(R.string.sticker_upload_error)
                            )
                            
                            return@launch
                        }
                        
                        cover = current.copy(fileId = uploaded)
                    }
                }
                
                else -> Unit
            }
            
            val coverFileId = when (val ready = cover) {
                is StickerPackCover.Remote -> ready.fileId
                is StickerPackCover.Local -> ready.fileId
                null -> null
            }
            
            val name = state.name.trim()
            
            val result = if (existingPackId == null) {
                stickerRepository.createPack(
                    packId = packId,
                    name = name,
                    username = state.username,
                    stickers = drafts,
                    coverFileId = coverFileId
                )
            } else {
                stickerRepository.updatePack(
                    packId = packId,
                    name = name,
                    username = state.username,
                    stickers = drafts,
                    coverFileId = coverFileId
                )
            }
            
            result.onSuccess { pack ->
                val saved = pack.stickers.map { sticker ->
                    StickerSlot.Remote(
                        fileId = sticker.fileId,
                        url = sticker.url,
                        emojis = sticker.emojis
                    )
                }
                
                val savedCoverFileId = pack.coverFileId
                val savedCoverUrl = pack.coverUrl
                
                val savedCover = if (savedCoverFileId != null && savedCoverUrl != null) {
                    StickerPackCover.Remote(fileId = savedCoverFileId, url = savedCoverUrl)
                } else {
                    null
                }
                
                removedSlot = null
                
                _uiState.update { current ->
                    current.copy(
                        packId = pack.id,
                        name = pack.name,
                        username = pack.username,
                        usernameStatus = UsernameStatus.Available,
                        stickers = saved,
                        cover = savedCover,
                        focusedStickerKey = null,
                        isSaving = false,
                        savedName = pack.name.trim(),
                        savedUsername = pack.username,
                        savedCoverKey = savedCover?.key,
                        savedStickers = saved.map { StickerSnapshot(it.key, it.emojis) }
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
                _uiState.update { current ->
                    current.copy(
                        stickers = slots,
                        cover = cover,
                        isSaving = false
                    )
                }
                
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
