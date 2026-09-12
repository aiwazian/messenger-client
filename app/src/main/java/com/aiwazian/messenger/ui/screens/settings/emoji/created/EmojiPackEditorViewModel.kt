package com.aiwazian.messenger.ui.screens.settings.emoji.created

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.CustomEmojiDraft
import com.aiwazian.messenger.repository.EmojiRepository
import com.aiwazian.messenger.utils.EmojiInput
import com.aiwazian.messenger.utils.media.EmojiEncoder
import com.aiwazian.messenger.utils.media.EncodedEmoji
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

sealed interface EmojiSlot {
    
    val key: String
    
    val emojis: List<String>
    
    fun withEmojis(emojis: List<String>): EmojiSlot
    
    data class Remote(
        val fileId: String,
        val url: String,
        override val emojis: List<String> = emptyList()
    ) : EmojiSlot {
        override val key: String get() = fileId
        
        override fun withEmojis(emojis: List<String>): EmojiSlot = copy(emojis = emojis)
    }
    
    data class Local(
        val emoji: EncodedEmoji,
        override val emojis: List<String> = emptyList(),
        val fileId: String? = null
    ) : EmojiSlot {
        override val key: String get() = emoji.uri.toString()
        
        override fun withEmojis(emojis: List<String>): EmojiSlot = copy(emojis = emojis)
    }
}

sealed interface EmojiPackCover {
    
    val key: String
    
    data class Remote(
        val fileId: String,
        val url: String
    ) : EmojiPackCover {
        override val key: String get() = fileId
    }
    
    data class Local(
        val emoji: EncodedEmoji,
        val fileId: String? = null
    ) : EmojiPackCover {
        override val key: String get() = emoji.uri.toString()
    }
}

data class EmojiSnapshot(
    val key: String,
    val emojis: List<String>
)

enum class EmojiUsernameStatus {
    Empty,
    TooShort,
    Checking,
    Available,
    Taken,
    Unknown
}

data class EmojiPackEditorUiState(
    val packId: Long? = null,
    val name: String = "",
    val username: String = "",
    val emojis: List<EmojiSlot> = emptyList(),
    val cover: EmojiPackCover? = null,
    val focusedEmojiKey: String? = null,
    val usernameStatus: EmojiUsernameStatus = EmojiUsernameStatus.Empty,
    val isLoading: Boolean = false,
    val isAddingEmoji: Boolean = false,
    val isChangingCover: Boolean = false,
    val isSaving: Boolean = false,
    val savedName: String = "",
    val savedUsername: String = "",
    val savedCoverKey: String? = null,
    val savedEmojis: List<EmojiSnapshot> = emptyList()
) {
    val isNameValid: Boolean get() = name.trim().isNotEmpty()
    
    val focusedEmoji: EmojiSlot?
        get() = emojis.firstOrNull { it.key == focusedEmojiKey }
    
    val canSave: Boolean
        get() = isNameValid &&
                usernameStatus == EmojiUsernameStatus.Available &&
                emojis.isNotEmpty() &&
                emojis.all { it.emojis.isNotEmpty() } &&
                !isSaving &&
                !isAddingEmoji &&
                !isChangingCover
    
    val hasChanges: Boolean
        get() = name.trim() != savedName ||
                username != savedUsername ||
                cover?.key != savedCoverKey ||
                emojis.map { EmojiSnapshot(it.key, it.emojis) } != savedEmojis
}

sealed interface EmojiPackEditorEffect {
    data class ShowMessage(
        @param:StringRes val messageRes: Int,
        val undoKey: String? = null
    ) : EmojiPackEditorEffect
    
    data object Saved : EmojiPackEditorEffect
}

@HiltViewModel
class EmojiPackEditorViewModel @Inject constructor(
    private val emojiRepository: EmojiRepository,
    private val emojiEncoder: EmojiEncoder
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EmojiPackEditorUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<EmojiPackEditorEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private var usernameJob: Job? = null
    private var isLoaded = false
    private var removedSlot: Pair<Int, EmojiSlot>? = null
    
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
                        EmojiUsernameStatus.Available
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
            
            emojiRepository.getPack(packId).onSuccess { pack ->
                val slots = pack.emojis.map { emoji ->
                    EmojiSlot.Remote(
                        fileId = emoji.fileId,
                        url = emoji.url,
                        emojis = emoji.emojis
                    )
                }
                
                val coverFileId = pack.coverFileId
                val coverUrl = pack.coverUrl
                
                val cover = if (coverFileId != null && coverUrl != null) {
                    EmojiPackCover.Remote(fileId = coverFileId, url = coverUrl)
                } else {
                    null
                }
                
                _uiState.update { state ->
                    state.copy(
                        packId = pack.id,
                        name = pack.name,
                        username = pack.username,
                        usernameStatus = EmojiUsernameStatus.Available,
                        emojis = slots,
                        cover = cover,
                        isLoading = false,
                        savedName = pack.name.trim(),
                        savedUsername = pack.username,
                        savedCoverKey = cover?.key,
                        savedEmojis = slots.map { EmojiSnapshot(it.key, it.emojis) }
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                
                _uiEffect.emit(
                    EmojiPackEditorEffect.ShowMessage(R.string.emoji_pack_load_error)
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
            cleaned.isEmpty() -> EmojiUsernameStatus.Empty
            cleaned.length < MIN_USERNAME_LENGTH -> EmojiUsernameStatus.TooShort
            else -> EmojiUsernameStatus.Checking
        }
        
        _uiState.update { it.copy(username = cleaned, usernameStatus = status) }
        
        if (status != EmojiUsernameStatus.Checking) {
            return
        }
        
        usernameJob = viewModelScope.launch {
            delay(USERNAME_CHECK_DELAY_MS)
            
            emojiRepository.isUsernameAvailable(cleaned, _uiState.value.packId)
                .onSuccess { available ->
                    _uiState.update { state ->
                        if (state.username != cleaned) {
                            state
                        } else {
                            state.copy(
                                usernameStatus = if (available) {
                                    EmojiUsernameStatus.Available
                                } else {
                                    EmojiUsernameStatus.Taken
                                }
                            )
                        }
                    }
                }.onFailure {
                    _uiState.update { state ->
                        if (state.username != cleaned) {
                            state
                        } else {
                            state.copy(usernameStatus = EmojiUsernameStatus.Unknown)
                        }
                    }
                }
        }
    }
    
    fun addEmoji(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingEmoji = true) }
            
            val encoded = emojiEncoder.encode(uri)
            
            if (encoded == null) {
                _uiState.update { it.copy(isAddingEmoji = false) }
                
                _uiEffect.emit(EmojiPackEditorEffect.ShowMessage(R.string.emoji_add_error))
                
                return@launch
            }
            
            val slot = EmojiSlot.Local(
                emoji = encoded,
                emojis = listOf(EmojiInput.DEFAULT_EMOJI)
            )
            
            _uiState.update { state ->
                state.copy(
                    emojis = state.emojis + slot,
                    focusedEmojiKey = slot.key,
                    isAddingEmoji = false
                )
            }
        }
    }
    
    fun addEmojiFromExisting(emoji: CustomEmoji) {
        val state = _uiState.value
        
        if (state.emojis.any { it is EmojiSlot.Remote && it.fileId == emoji.fileId }) {
            return
        }
        
        val slot = EmojiSlot.Remote(
            fileId = emoji.fileId,
            url = emoji.url,
            emojis = emoji.emojis.ifEmpty { listOf(EmojiInput.DEFAULT_EMOJI) }
        )
        
        _uiState.update { current ->
            current.copy(emojis = current.emojis + slot)
        }
    }
    
    fun setCoverFromFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingCover = true) }
            
            val encoded = emojiEncoder.encode(uri)
            
            if (encoded == null) {
                _uiState.update { it.copy(isChangingCover = false) }
                
                _uiEffect.emit(EmojiPackEditorEffect.ShowMessage(R.string.emoji_add_error))
                
                return@launch
            }
            
            _uiState.update { state ->
                state.copy(
                    cover = EmojiPackCover.Local(emoji = encoded),
                    isChangingCover = false
                )
            }
        }
    }
    
    fun setCoverFromEmoji(emoji: CustomEmoji) {
        _uiState.update { state ->
            state.copy(
                cover = EmojiPackCover.Remote(
                    fileId = emoji.fileId,
                    url = emoji.url
                )
            )
        }
    }
    
    fun removeCover() {
        _uiState.update { state -> state.copy(cover = null) }
    }
    
    fun focusEmoji(key: String) {
        _uiState.update { state ->
            if (state.emojis.none { it.key == key }) {
                state
            } else {
                state.copy(focusedEmojiKey = key)
            }
        }
    }
    
    fun clearFocus() {
        _uiState.update { it.copy(focusedEmojiKey = null) }
    }
    
    fun onEmojiSymbolsChange(key: String, value: String) {
        val symbols = EmojiInput.parse(value)
        
        _uiState.update { state ->
            state.copy(
                emojis = state.emojis.map { slot ->
                    if (slot.key == key) {
                        slot.withEmojis(symbols)
                    } else {
                        slot
                    }
                }
            )
        }
    }
    
    fun removeEmoji(key: String) {
        val index = _uiState.value.emojis.indexOfFirst { it.key == key }
        
        if (index < 0) {
            return
        }
        
        removedSlot = index to _uiState.value.emojis[index]
        
        _uiState.update { state ->
            state.copy(
                emojis = state.emojis.filterNot { it.key == key },
                focusedEmojiKey = if (state.focusedEmojiKey == key) {
                    null
                } else {
                    state.focusedEmojiKey
                }
            )
        }
        
        viewModelScope.launch {
            _uiEffect.emit(
                EmojiPackEditorEffect.ShowMessage(
                    messageRes = R.string.emoji_removed,
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
            if (state.emojis.any { it.key == key }) {
                state
            } else {
                val restored = state.emojis.toMutableList()
                
                restored.add(removed.first.coerceIn(0, restored.size), removed.second)
                
                state.copy(emojis = restored)
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
            
            val packId = existingPackId ?: emojiRepository.reservePackId()
                .getOrNull()
            
            if (packId == null) {
                _uiState.update { current -> current.copy(isSaving = false) }
                
                _uiEffect.emit(
                    EmojiPackEditorEffect.ShowMessage(R.string.emoji_pack_save_error)
                )
                
                return@launch
            }
            
            val slots = state.emojis.toMutableList()
            val drafts = mutableListOf<CustomEmojiDraft>()
            
            slots.forEachIndexed { index, slot ->
                when (slot) {
                    is EmojiSlot.Remote -> drafts.add(
                        CustomEmojiDraft(fileId = slot.fileId, emojis = slot.emojis)
                    )
                    
                    is EmojiSlot.Local -> {
                        val known = slot.fileId
                        
                        if (known != null) {
                            drafts.add(CustomEmojiDraft(fileId = known, emojis = slot.emojis))
                        } else {
                            val uploaded = emojiRepository.uploadEmoji(slot.emoji, packId)
                                .getOrNull()
                            
                            if (uploaded == null) {
                                _uiState.update { current ->
                                    current.copy(emojis = slots, isSaving = false)
                                }
                                
                                _uiEffect.emit(
                                    EmojiPackEditorEffect.ShowMessage(R.string.emoji_upload_error)
                                )
                                
                                return@launch
                            }
                            
                            slots[index] = slot.copy(fileId = uploaded)
                            
                            drafts.add(CustomEmojiDraft(fileId = uploaded, emojis = slot.emojis))
                        }
                    }
                }
            }
            
            var cover = state.cover
            
            when (val current = cover) {
                is EmojiPackCover.Local -> {
                    val known = current.fileId
                    
                    if (known == null) {
                        val uploaded = emojiRepository.uploadEmoji(current.emoji, packId)
                            .getOrNull()
                        
                        if (uploaded == null) {
                            _uiState.update { state ->
                                state.copy(emojis = slots, isSaving = false)
                            }
                            
                            _uiEffect.emit(
                                EmojiPackEditorEffect.ShowMessage(R.string.emoji_upload_error)
                            )
                            
                            return@launch
                        }
                        
                        cover = current.copy(fileId = uploaded)
                    }
                }
                
                else -> Unit
            }
            
            val coverFileId = when (val ready = cover) {
                is EmojiPackCover.Remote -> ready.fileId
                is EmojiPackCover.Local -> ready.fileId
                null -> null
            }
            
            val name = state.name.trim()
            
            val result = if (existingPackId == null) {
                emojiRepository.createPack(
                    packId = packId,
                    name = name,
                    username = state.username,
                    emojis = drafts,
                    coverFileId = coverFileId
                )
            } else {
                emojiRepository.updatePack(
                    packId = packId,
                    name = name,
                    username = state.username,
                    emojis = drafts,
                    coverFileId = coverFileId,
                    removeCover = coverFileId == null
                )
            }
            
            result.onSuccess { pack ->
                val saved = pack.emojis.map { emoji ->
                    EmojiSlot.Remote(
                        fileId = emoji.fileId,
                        url = emoji.url,
                        emojis = emoji.emojis
                    )
                }
                
                val savedCoverFileId = pack.coverFileId
                val savedCoverUrl = pack.coverUrl
                
                val savedCover = if (savedCoverFileId != null && savedCoverUrl != null) {
                    EmojiPackCover.Remote(fileId = savedCoverFileId, url = savedCoverUrl)
                } else {
                    null
                }
                
                removedSlot = null
                
                _uiState.update { current ->
                    current.copy(
                        packId = pack.id,
                        name = pack.name,
                        username = pack.username,
                        usernameStatus = EmojiUsernameStatus.Available,
                        emojis = saved,
                        cover = savedCover,
                        focusedEmojiKey = null,
                        isSaving = false,
                        savedName = pack.name.trim(),
                        savedUsername = pack.username,
                        savedCoverKey = savedCover?.key,
                        savedEmojis = saved.map { EmojiSnapshot(it.key, it.emojis) }
                    )
                }
                
                if (exitAfterSave) {
                    _uiEffect.emit(EmojiPackEditorEffect.Saved)
                } else {
                    _uiEffect.emit(
                        EmojiPackEditorEffect.ShowMessage(R.string.emoji_pack_saved)
                    )
                }
            }.onFailure {
                _uiState.update { current ->
                    current.copy(
                        emojis = slots,
                        cover = cover,
                        isSaving = false
                    )
                }
                
                _uiEffect.emit(
                    EmojiPackEditorEffect.ShowMessage(R.string.emoji_pack_save_error)
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
