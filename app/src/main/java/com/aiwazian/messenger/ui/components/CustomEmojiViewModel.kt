/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.repository.EmojiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomEmojiViewModel @Inject constructor(
    private val emojiRepository: EmojiRepository
) : ViewModel() {
    
    private val _emojis = MutableStateFlow<Map<Long, CustomEmoji>>(emptyMap())
    val emojis = _emojis.asStateFlow()
    
    private val requestedEmojiIds = mutableSetOf<Long>()
    
    fun requestEmojis(emojiIds: List<Long>) {
        val missingIds = emojiIds
            .filter { it > 0L }
            .distinct()
            .filterNot { id ->
                requestedEmojiIds.contains(id) || _emojis.value.containsKey(id)
            }
        
        if (missingIds.isEmpty()) {
            return
        }
        
        requestedEmojiIds.addAll(missingIds)
        
        viewModelScope.launch {
            emojiRepository.resolveEmojis(missingIds).onSuccess { loaded ->
                cacheEmojis(loaded)
            }.onFailure {
                requestedEmojiIds.removeAll(missingIds.toSet())
            }
        }
    }
    
    suspend fun resolveEmoji(emojiId: Long): CustomEmoji? {
        if (emojiId <= 0L) {
            return null
        }
        
        _emojis.value[emojiId]?.let { return it }
        
        val emoji = emojiRepository.resolveEmojis(listOf(emojiId))
            .getOrNull()
            ?.firstOrNull { it.id == emojiId }
            ?: return null
        
        cacheEmojis(listOf(emoji))
        
        return emoji
    }
    
    private fun cacheEmojis(emojis: List<CustomEmoji>) {
        if (emojis.isEmpty()) {
            return
        }
        
        requestedEmojiIds.addAll(emojis.map { it.id })
        
        _emojis.update { cache -> cache + emojis.associateBy { it.id } }
    }
}
