/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val PAGE_SIZE = 20
    
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery.normalizeSearchQuery()) }
        resetAndSearch()
    }
    
    private fun resetAndSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                chatResults = emptyList(),
                hasMoreChats = true,
                isChatLoading = false,
            )
        }
        searchJob = viewModelScope.launch {
            delay(300.milliseconds)
            loadMore()
        }
    }
    
    fun loadMore() {
        val state = _uiState.value
        val query = state.query
        if (query.isBlank() || !state.hasMoreChats || state.isChatLoading) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isChatLoading = true) }
            try {
                val results = searchRepository.search(
                    query = query,
                    limit = PAGE_SIZE,
                    offset = state.chatResults.size
                )
                _uiState.update {
                    it.copy(
                        chatResults = it.chatResults + results,
                        isChatLoading = false,
                        hasMoreChats = results.size == PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Failed to load more chats: ${e.message}", e)
                _uiState.update { it.copy(isChatLoading = false) }
            }
        }
    }
}

/**
 * Собачка в начале отбрасывается: username копируют из профиля или
 * ссылки вместе с ней, а сервер ищет по чистому имени, и «@olega» не
 * находил ничего. Внутри строки символ осмысленный и остаётся как есть.
 *
 * Пробелы снимаются с двух сторон и после собачки: клавиатура любит
 * добавлять пробел к подсказке, а вставляют и «@ olega».
 */
private fun String.normalizeSearchQuery(): String = trim().trimStart('@').trim()
