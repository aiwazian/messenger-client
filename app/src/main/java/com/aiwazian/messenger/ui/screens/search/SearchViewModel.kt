/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Search>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.update { newQuery }

        if (_query.value.isBlank()) {
            _searchResults.update { emptyList() }
            return
        }

        viewModelScope.launch {
            search()
        }
    }

    private suspend fun search() {
        try {
            val searchResult = searchRepository.search(_query.value.trim())
            _searchResults.update { searchResult }
        } catch (e: Exception) {
            Log.e("SearchViewModel", e.toString())
        }
    }
}


