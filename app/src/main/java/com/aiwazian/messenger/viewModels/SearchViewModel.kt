package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.data.SearchInfo
import com.aiwazian.messenger.services.SearchService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val searchService: SearchService) : ViewModel() {
    
    private val _searchResults = MutableStateFlow<List<SearchInfo>>(emptyList())
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
            val searchResult = searchService.searchUserByUsername(_query.value.trim())
            _searchResults.update { searchResult ?: emptyList() }
        } catch (e: Exception) {
            Log.e(
                "SearchViewModel",
                "${e.message}"
            )
        }
    }
}
