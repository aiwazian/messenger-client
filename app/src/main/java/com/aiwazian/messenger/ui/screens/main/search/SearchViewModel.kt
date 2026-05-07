/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.enums.SearchResultType
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val fileHandler: FileHandler,
    private val downloaderManager: DownloaderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val PAGE_SIZE = 20

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        resetAndSearch()
    }

    fun onTabChange(newTab: Int) {
        _uiState.update { it.copy(activeTab = newTab) }
    }

    private fun resetAndSearch() {
        searchJob?.cancel()
        _uiState.update { 
            it.copy(
                chatResults = emptyList(),
                fileResults = emptyList(),
                hasMoreChats = true,
                hasMoreFiles = true,
                isChatLoading = false,
                isFileLoading = false
            )
        }
        
        searchJob = viewModelScope.launch {
            delay(300)
            
            launch {
                loadMoreForTab(0)
            }
            
            launch {
                loadMoreForTab(1)
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        viewModelScope.launch {
            loadMoreForTab(state.activeTab)
        }
    }

    private suspend fun loadMoreForTab(tab: Int) {
        val state = _uiState.value
        val query = state.query
        
        if (tab == 0) {
            if (query.isBlank() || !state.hasMoreChats || state.isChatLoading) return
            _uiState.update { it.copy(isChatLoading = true) }
            try {
                val results = searchRepository.search(
                    query = query,
                    type = "chats",
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
                _uiState.update { it.copy(isChatLoading = false) }
            }
        } else {
            if (!state.hasMoreFiles || state.isFileLoading) return
            _uiState.update { it.copy(isFileLoading = true) }
            try {
                val results = searchRepository.search(
                    query = query,
                    type = "files",
                    limit = PAGE_SIZE,
                    offset = state.fileResults.size
                )
                _uiState.update {
                    it.copy(
                        fileResults = it.fileResults + results,
                        isFileLoading = false,
                        hasMoreFiles = results.size == PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFileLoading = false) }
            }
        }
    }

    fun onFileClicked(search: Search) {
        if (search.type != SearchResultType.FILE) return
        viewModelScope.launch {
            val localUri = downloaderManager.getFile(
                search.fileId ?: return@launch, 
                search.name.substringAfterLast('.', "")
            ).let { if (it.exists()) it.absolutePath else null }

//            fileHandler.openFile(
//                localUri = localUri
//            )
        }
    }
}
