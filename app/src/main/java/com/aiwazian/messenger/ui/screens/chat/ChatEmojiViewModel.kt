package com.aiwazian.messenger.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.repository.EmojiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatEmojiUiState(
    val addedPacks: List<EmojiPack> = emptyList(),
    val packsById: Map<Long, EmojiPack> = emptyMap()
)

@HiltViewModel
class ChatEmojiViewModel @Inject constructor(
    private val emojiRepository: EmojiRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatEmojiUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _resolvedEmojis = MutableStateFlow<Map<Long, CustomEmoji>>(emptyMap())
    val resolvedEmojis = _resolvedEmojis.asStateFlow()
    
    private val requestedEmojiIds = mutableSetOf<Long>()
    
    private var isAddedPacksRequested = false
    
    fun preloadPacks() {
        if (isAddedPacksRequested) {
            return
        }
        
        loadAddedPacks()
    }
    
    fun requestEmojis(emojiIds: List<Long>) {
        val missingIds = emojiIds
            .filter { it > 0L }
            .distinct()
            .filterNot { id ->
                requestedEmojiIds.contains(id) || _resolvedEmojis.value.containsKey(id)
            }
        
        if (missingIds.isEmpty()) {
            return
        }
        
        requestedEmojiIds.addAll(missingIds)
        
        viewModelScope.launch {
            emojiRepository.resolveEmojis(missingIds).onSuccess { emojis ->
                cacheEmojis(emojis)
            }.onFailure {
                requestedEmojiIds.removeAll(missingIds.toSet())
            }
        }
    }
    
    suspend fun resolveEmoji(emojiId: Long): CustomEmoji? {
        if (emojiId <= 0L) {
            return null
        }
        
        _resolvedEmojis.value[emojiId]?.let { return it }
        
        val emoji = emojiRepository.resolveEmojis(listOf(emojiId))
            .getOrNull()
            ?.firstOrNull { it.id == emojiId }
            ?: return null
        
        cacheEmojis(listOf(emoji))
        
        return emoji
    }
    
    private fun loadAddedPacks() {
        isAddedPacksRequested = true
        
        viewModelScope.launch {
            emojiRepository.getAddedPacks(includeEmojis = true).onSuccess { packs ->
                val detailed = packs.map { pack ->
                    if (pack.emojis.isNotEmpty()) {
                        pack
                    } else {
                        emojiRepository.getPack(pack.id).getOrNull() ?: pack
                    }
                }
                
                cacheEmojis(detailed.flatMap { it.emojis })
                
                _uiState.update { state ->
                    state.copy(
                        addedPacks = detailed,
                        packsById = state.packsById + detailed.associateBy { it.id }
                    )
                }
            }.onFailure {
                isAddedPacksRequested = false
            }
        }
    }
    
    private fun cacheEmojis(emojis: List<CustomEmoji>) {
        if (emojis.isEmpty()) {
            return
        }
        
        requestedEmojiIds.addAll(emojis.map { it.id })
        
        _resolvedEmojis.update { cache -> cache + emojis.associateBy { it.id } }
    }
}
