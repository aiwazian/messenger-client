/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.MessageSearchHit
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.screens.chat.MessageSearchSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun ChatViewModel.startMessageSearch() = _uiState.update {
    it.copy(isMessageSearchActive = true, isMessageSearchListMode = false)
}

internal fun ChatViewModel.stopMessageSearch() {
    searchJob?.cancel()
    searchCursorId = null
    _uiState.update {
        it.copy(
            isMessageSearchActive = false,
            isMessageSearchListMode = false,
            messageSearchQuery = "",
            messageSearchResults = emptyList(),
            isSearchingMessages = false,
            hasMoreSearchResults = false,
            messageSearchTotal = 0,
            isMessageSearchTotalExact = true,
            messageSearchIndex = -1,
            messageSearchSenders = emptyMap()
        )
    }
}

internal fun ChatViewModel.changeMessageSearchQuery(query: String) {
    _uiState.update { it.copy(messageSearchQuery = query) }
    searchJob?.cancel()

    if (query.isBlank()) {
        clearMessageSearchResults()
        return
    }

    searchJob = viewModelScope.launch {
        delay(350.milliseconds)
        searchCursorId = null
        runMessageSearch(query, reset = true)
    }
}

internal fun ChatViewModel.clearMessageSearchQuery() {
    searchJob?.cancel()
    _uiState.update { it.copy(messageSearchQuery = "") }
    clearMessageSearchResults()
}

private fun ChatViewModel.clearMessageSearchResults() {
    searchCursorId = null
    _uiState.update {
        it.copy(
            messageSearchResults = emptyList(),
            hasMoreSearchResults = false,
            isSearchingMessages = false,
            messageSearchTotal = 0,
            isMessageSearchTotalExact = true,
            messageSearchIndex = -1
        )
    }
}

internal fun ChatViewModel.toggleMessageSearchDisplayMode() {
    val goingToChat = _uiState.value.isMessageSearchListMode
    _uiState.update { it.copy(isMessageSearchListMode = !goingToChat) }

    if (goingToChat && _uiState.value.messageSearchIndex < 0) selectSearchResult(0)
}

internal fun ChatViewModel.loadMoreSearchResults() {
    val query = _uiState.value.messageSearchQuery
    if (query.isBlank() || searchCursorId == null || _uiState.value.isSearchingMessages) return
    searchJob?.cancel()
    searchJob = viewModelScope.launch { runMessageSearch(query, reset = false) }
}

internal fun ChatViewModel.goToOlderSearchResult() {
    val state = _uiState.value
    val target = state.messageSearchIndex + 1

    if (target < state.messageSearchResults.size) {
        selectSearchResult(target)
        return
    }

    if (!state.hasMoreSearchResults || state.isSearchingMessages) return
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        runMessageSearch(state.messageSearchQuery, reset = false)
        if (target < _uiState.value.messageSearchResults.size) selectSearchResult(target)
    }
}

internal fun ChatViewModel.goToNewerSearchResult() {
    val target = _uiState.value.messageSearchIndex - 1
    if (target < 0) return
    selectSearchResult(target)
}

internal fun ChatViewModel.onSearchResultClicked(hit: MessageSearchHit) {
    val index = _uiState.value.messageSearchResults.indexOfFirst { it.id == hit.id }
    if (index < 0) {
        _uiState.update { it.copy(isMessageSearchListMode = false) }
        jumpToMessage(hit.id)
        return
    }
    selectSearchResult(index)
}

private fun ChatViewModel.selectSearchResult(index: Int) {
    val hit = _uiState.value.messageSearchResults.getOrNull(index) ?: return
    _uiState.update { it.copy(messageSearchIndex = index, isMessageSearchListMode = false) }
    jumpToMessage(hit.id)
}

private suspend fun ChatViewModel.runMessageSearch(query: String, reset: Boolean) {
    _uiState.update { it.copy(isSearchingMessages = true) }
    chatRepository.searchMessages(
        chatId = _uiState.value.chatId,
        query = query,
        cursorId = if (reset) null else searchCursorId,
        limit = ChatViewModel.SEARCH_PAGE_SIZE
    ).onSuccess { page ->
        searchCursorId = page.nextCursorId
        _uiState.update {
            it.copy(
                messageSearchResults = if (reset) page.items else it.messageSearchResults + page.items,
                hasMoreSearchResults = page.nextCursorId != null,
                isSearchingMessages = false,
                messageSearchTotal = page.total ?: it.messageSearchTotal,
                isMessageSearchTotalExact = if (reset) page.totalIsExact
                else it.isMessageSearchTotalExact,
                messageSearchIndex = if (reset) -1 else it.messageSearchIndex
            )
        }
        loadSearchSenders(page.items)

        if (reset && page.items.isNotEmpty()) {
            if (_uiState.value.isMessageSearchListMode) {
                _uiState.update { it.copy(messageSearchIndex = 0) }
            } else {
                selectSearchResult(0)
            }
        }
    }.onFailure {
        _uiState.update { it.copy(isSearchingMessages = false) }
    }
}

private fun ChatViewModel.loadSearchSenders(hits: List<MessageSearchHit>) {
    if (ChatType.fromId(_uiState.value.chatId) == ChatType.CHANNEL) return

    hits.map { it.senderId }
        .distinct()
        .filter { !_uiState.value.messageSearchSenders.containsKey(it) }
        .forEach { senderId ->
            viewModelScope.launch {
                try {
                    userRepository.getById(senderId).collect { user ->
                        val sender = MessageSearchSender(
                            name = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                            avatarUri = user.avatars.firstOrNull()?.uri
                        )
                        _uiState.update {
                            it.copy(
                                messageSearchSenders = it.messageSearchSenders + (senderId to sender)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error loading search sender", e)
                }
            }
        }
}
