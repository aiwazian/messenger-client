/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.UserHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<StorageUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
    
    var appSize: Long = 0
        private set
    
    fun loadStorageInfo(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val categories = storageRepository.getStorageStats()
            val totalCacheSize = categories.sumOf { it.totalSize }
            
            _uiState.update {
                it.copy(
                    categories = categories,
                    totalCacheSize = totalCacheSize,
                    isLoading = false
                )
            }
            
            getAppSize(context)
        }
    }
    
    fun toggleCategory(category: FileCategory) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedCategories = currentState.categories.map { cat ->
                if (cat.category == category) {
                    cat.copy(isSelected = !cat.isSelected)
                } else {
                    cat
                }
            }
            
            val selectedSize = updatedCategories
                .filter { it.isSelected }
                .sumOf { it.totalSize }
            
            _uiState.update {
                it.copy(
                    categories = updatedCategories,
                    selectedSize = selectedSize
                )
            }
        }
    }
    
    fun selectAllCategories() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedCategories = currentState.categories.map { it.copy(isSelected = true) }
            val selectedSize = currentState.totalCacheSize
            
            _uiState.update {
                it.copy(
                    categories = updatedCategories,
                    selectedSize = selectedSize
                )
            }
        }
    }
    
    fun deselectAllCategories() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedCategories = currentState.categories.map { it.copy(isSelected = false) }
            
            _uiState.update {
                it.copy(
                    categories = updatedCategories,
                    selectedSize = 0
                )
            }
        }
    }
    
    fun showConfirmDialog() {
        val state = _uiState.value
        if (state.selectedCategories.isNotEmpty()) {
            _uiState.update { it.copy(showConfirmDialog = true) }
        }
    }
    
    fun hideConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }
    
    fun clearSelectedCache(context: Context) {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedCategories = state.selectedCategories.map { it.category }
            
            if (selectedCategories.isEmpty()) {
                _uiEvent.emit(StorageUiEvent.Error("Нет выбранных категорий"))
                return@launch
            }
            
            val filesToDelete = storageRepository.getFilesForCategories(selectedCategories)
            
            storageRepository.clearFiles(filesToDelete)
            
            // Перезагружаем информацию о хранилище
            loadStorageInfo(context)
            
            hideConfirmDialog()
            _uiEvent.emit(StorageUiEvent.CacheCleared)
        }
    }
    
    private fun getAppSize(context: Context) {
        val storageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val appInfo = context.applicationInfo
        val user = UserHandle.getUserHandleForUid(appInfo.uid)
        
        try {
            val stats = storageStatsManager.queryStatsForPackage(
                appInfo.storageUuid,
                context.packageName,
                user
            )
            appSize = stats.appBytes + stats.dataBytes + stats.cacheBytes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
