/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.paging

import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MessageWindowBounds(
    val fromId: Long = 0L,
    val toId: Long = Long.MAX_VALUE,
    val includePending: Boolean = true
)

data class MessageWindowState(
    val bounds: MessageWindowBounds = MessageWindowBounds(),
    val hasMoreBefore: Boolean = true,
    val hasMoreAfter: Boolean = false,
    val isLoadingBefore: Boolean = false,
    val isLoadingAfter: Boolean = false,
    val isRelocating: Boolean = false,
    val isAtLive: Boolean = true,
    val isInitialized: Boolean = false
)

class MessageWindowPager(
    private val chatRepository: ChatRepository,
    private val userId: Long,
    private val chatId: Long,
    private val pageSize: Int = 50,
    private val aroundRadius: Int = 25,
    private val maxWindowMessages: Int = 400
) {
    private val _state = MutableStateFlow(MessageWindowState())
    val state = _state.asStateFlow()
    
    private val mutex = Mutex()
    
    private val loadedIds = ArrayDeque<Long>()
    
    fun containsMessage(messageId: Long): Boolean = loadedIds.contains(messageId)

    suspend fun openAtLatest(): Boolean = mutex.withLock { openAtLatestLocked() }
    
    private suspend fun openAtLatestLocked(): Boolean {
        _state.update { it.copy(isRelocating = true) }
        
        val page = chatRepository.fetchMessagesWindow(chatId = chatId, limit = pageSize).getOrNull()
        
        if (page == null) {
            val cachedIds = chatRepository.getLastMessageIds(userId, chatId, pageSize).sorted()
            replaceIds(cachedIds)
            _state.update {
                it.copy(
                    bounds = MessageWindowBounds(oldestId(), Long.MAX_VALUE, true),
                    hasMoreBefore = cachedIds.size >= pageSize,
                    hasMoreAfter = false,
                    isAtLive = true,
                    isRelocating = false,
                    isLoadingBefore = false,
                    isLoadingAfter = false,
                    isInitialized = true
                )
            }
            return false
        }
        
        replaceIds(page.messages.map { it.id }.filter { it > 0 }.sorted())
        _state.update {
            it.copy(
                bounds = MessageWindowBounds(oldestId(), Long.MAX_VALUE, true),
                hasMoreBefore = page.hasMoreBefore,
                hasMoreAfter = false,
                isAtLive = true,
                isRelocating = false,
                isLoadingBefore = false,
                isLoadingAfter = false,
                isInitialized = true
            )
        }
        return true
    }

    suspend fun openAtFirstUnread(): Long? = mutex.withLock {
        _state.update { it.copy(isRelocating = true) }
        
        val page = chatRepository.fetchMessagesWindow(
            chatId = chatId,
            anchor = ANCHOR_FIRST_UNREAD,
            limit = pageSize
        ).getOrNull()
        
        val firstUnreadId = page?.firstUnreadMessageId?.takeIf { page.unreadCount > 0 }
        
        if (page == null || page.messages.isEmpty() || firstUnreadId == null) {
            openAtLatestLocked()
            return@withLock null
        }
        
        replaceIds(page.messages.map { it.id }.filter { it > 0 }.sorted())
        
        val atLive = !page.hasMoreAfter
        _state.update {
            it.copy(
                bounds = MessageWindowBounds(
                    fromId = oldestId(),
                    toId = if (atLive) Long.MAX_VALUE else newestId(),
                    includePending = atLive
                ),
                hasMoreBefore = page.hasMoreBefore,
                hasMoreAfter = page.hasMoreAfter,
                isAtLive = atLive,
                isRelocating = false,
                isLoadingBefore = false,
                isLoadingAfter = false,
                isInitialized = true
            )
        }
        firstUnreadId
    }

    suspend fun jumpTo(messageId: Long): Boolean = mutex.withLock {
        if (loadedIds.contains(messageId)) return@withLock true
        
        _state.update { it.copy(isRelocating = true) }
        
        val page = chatRepository.fetchMessagesWindow(
            chatId = chatId,
            anchorId = messageId,
            limit = aroundRadius
        ).getOrNull()
        
        if (page == null || page.messages.isEmpty()) {
            _state.update { it.copy(isRelocating = false) }
            return@withLock false
        }
        
        replaceIds(page.messages.map { it.id }.filter { it > 0 }.sorted())
        
        val atLive = !page.hasMoreAfter
        _state.update {
            it.copy(
                bounds = MessageWindowBounds(
                    fromId = oldestId(),
                    toId = if (atLive) Long.MAX_VALUE else newestId(),
                    includePending = atLive
                ),
                hasMoreBefore = page.hasMoreBefore,
                hasMoreAfter = page.hasMoreAfter,
                isAtLive = atLive,
                isRelocating = false,
                isLoadingBefore = false,
                isLoadingAfter = false,
                isInitialized = true
            )
        }
        true
    }
    
    suspend fun loadBefore() {
        val snapshot = _state.value
        if (snapshot.isLoadingBefore || snapshot.isRelocating || !snapshot.hasMoreBefore) return
        
        mutex.withLock {
            val current = _state.value
            if (current.isLoadingBefore || current.isRelocating || !current.hasMoreBefore) return
            
            if (loadedIds.isEmpty()) {
                openAtLatestLocked()
                return
            }
            
            _state.update { it.copy(isLoadingBefore = true) }
            
            val cursor = oldestId()
            val page = chatRepository.fetchMessagesWindow(
                chatId = chatId,
                beforeId = cursor,
                limit = pageSize
            ).getOrNull()
            
            if (page == null) {
                _state.update { it.copy(isLoadingBefore = false) }
                return
            }
            
            val newIds = page.messages.map { it.id }.filter { it in 1 until cursor }.sorted()
            newIds.asReversed().forEach { loadedIds.addFirst(it) }
            
            val trimmedNewest = trimNewestIfNeeded()
            
            _state.update {
                it.copy(
                    bounds = MessageWindowBounds(
                        fromId = oldestId(),
                        toId = if (trimmedNewest) newestId() else it.bounds.toId,
                        includePending = if (trimmedNewest) false else it.bounds.includePending
                    ),
                    hasMoreBefore = page.hasMoreBefore && newIds.isNotEmpty(),
                    hasMoreAfter = if (trimmedNewest) true else it.hasMoreAfter,
                    isAtLive = if (trimmedNewest) false else it.isAtLive,
                    isLoadingBefore = false
                )
            }
        }
    }
    
    suspend fun loadAfter() {
        val snapshot = _state.value
        if (snapshot.isLoadingAfter || snapshot.isRelocating || !snapshot.hasMoreAfter) return
        
        mutex.withLock {
            val current = _state.value
            if (current.isLoadingAfter || current.isRelocating || !current.hasMoreAfter) return
            if (loadedIds.isEmpty()) return
            
            _state.update { it.copy(isLoadingAfter = true) }
            
            val cursor = newestId()
            val page = chatRepository.fetchMessagesWindow(
                chatId = chatId,
                afterId = cursor,
                limit = pageSize
            ).getOrNull()
            
            if (page == null) {
                _state.update { it.copy(isLoadingAfter = false) }
                return
            }
            
            val newIds = page.messages.map { it.id }.filter { it > cursor }.sorted()
            newIds.forEach { loadedIds.addLast(it) }
            
            val trimmedOldest = trimOldestIfNeeded()
            val atLive = !page.hasMoreAfter
            
            _state.update {
                it.copy(
                    bounds = MessageWindowBounds(
                        fromId = oldestId(),
                        toId = if (atLive) Long.MAX_VALUE else newestId(),
                        includePending = atLive
                    ),
                    hasMoreBefore = if (trimmedOldest) true else it.hasMoreBefore,
                    hasMoreAfter = page.hasMoreAfter,
                    isAtLive = atLive,
                    isLoadingAfter = false
                )
            }
        }
    }
    
    private fun replaceIds(ids: List<Long>) {
        loadedIds.clear()
        loadedIds.addAll(ids)
    }
    
    private fun oldestId(): Long = loadedIds.firstOrNull() ?: 0L
    
    private fun newestId(): Long = loadedIds.lastOrNull() ?: Long.MAX_VALUE
    
    private fun trimNewestIfNeeded(): Boolean {
        var trimmed = false
        while (loadedIds.size > maxWindowMessages) {
            loadedIds.removeLast()
            trimmed = true
        }
        return trimmed
    }
    
    private fun trimOldestIfNeeded(): Boolean {
        var trimmed = false
        while (loadedIds.size > maxWindowMessages) {
            loadedIds.removeFirst()
            trimmed = true
        }
        return trimmed
    }
    
    companion object {
        const val ANCHOR_FIRST_UNREAD = "first_unread"
    }
}
