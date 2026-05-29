/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.UserHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val storageRepository: StorageRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<StorageUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
    
    private val storageStatsManager = context.getSystemService(StorageStatsManager::class.java)
    
    init {
        loadStorageInfo()
    }
    
    private fun loadStorageInfo() {
        viewModelScope.launch {
            val categories = storageRepository.getStorageStats()
            val totalCacheSize = categories.sumOf { it.totalSize }
            
            _uiState.update {
                it.copy(
                    categories = categories,
                    totalCacheSize = totalCacheSize
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
    
    fun showConfirmDialog() {
        val state = _uiState.value
        if (state.selectedCategories.isNotEmpty()) {
            _uiState.update { it.copy(showConfirmDialog = true) }
        }
    }
    
    fun hideConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }
    
    fun showClearDatabaseDialog() {
        _uiState.update { it.copy(showClearDatabaseDialog = true) }
    }
    
    fun hideClearDatabaseDialog() {
        _uiState.update { it.copy(showClearDatabaseDialog = false) }
    }
    
    fun clearSelectedCache() {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedCategories = state.selectedCategories.map { it.category }
            
            if (selectedCategories.isEmpty()) {
                _uiEvent.emit(StorageUiEvent.Error("Нет выбранных категорий"))
                return@launch
            }
            
            val filesToDelete = storageRepository.getFilesForCategories(selectedCategories)
            
            storageRepository.clearFiles(filesToDelete)
            
            loadStorageInfo()
            
            hideConfirmDialog()
            _uiEvent.emit(StorageUiEvent.CacheCleared)
        }
    }
    
    fun clearDatabase() {
        viewModelScope.launch {
            storageRepository.clearDatabaseExceptAccount()
            _uiEvent.emit(StorageUiEvent.DatabaseCleared)
            chatRepository.refreshChats()
        }
    }
    
    private fun getAppSize(context: Context) {
        val appInfo = context.applicationInfo
        val user = UserHandle.getUserHandleForUid(appInfo.uid)
        
        try {
            val stats = storageStatsManager.queryStatsForPackage(
                appInfo.storageUuid,
                context.packageName,
                user
            )
            _uiState.update { it.copy(appSize = stats.appBytes + stats.dataBytes + stats.cacheBytes) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
