/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.UserHandle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.StorageRepository
import com.aiwazian.messenger.utils.UiText
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
        viewModelScope.launch {
            loadStorageInfo()
        }
    }
    
    private suspend fun loadStorageInfo() {
        val categories = storageRepository.getStorageStats()
        val totalCacheSize = categories.sumOf { it.totalSize }
        
        _uiState.update {
            it.copy(
                categories = categories,
                totalCacheSize = totalCacheSize,
                // Выбор сбрасывается вместе с категориями: getStorageStats возвращает
                // их с isSelected = false, и прежний selectedSize остался бы висеть в
                // выключенной кнопке — размером, которого на диске уже нет.
                selectedSize = 0
            )
        }
        
        getAppSize(context)
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
            val selectedCategories = _uiState.value.selectedCategories.map { it.category }
            
            // Диалог убираем сразу: удаление сотни файлов занимает время, и всё
            // это время он висел бы поверх экрана без единого признака работы.
            hideConfirmDialog()
            
            if (selectedCategories.isEmpty()) {
                _uiEvent.emit(
                    StorageUiEvent.Error(
                        UiText.StringResource(R.string.storage_no_categories_selected)
                    )
                )
                return@launch
            }
            
            val result = try {
                val filesToDelete = storageRepository.getFilesForCategories(selectedCategories)
                storageRepository.clearFiles(filesToDelete)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to clear cache", e)
                _uiEvent.emit(
                    StorageUiEvent.Error(
                        UiText.StringResource(R.string.storage_cache_clear_failed)
                    )
                )
                return@launch
            }
            
            // Цифры на экране обновляем до сообщения: иначе рядом с «41,3 MB
            // очистилось» ещё стояли бы прежние размеры категорий.
            loadStorageInfo()
            
            when {
                // Из непустого списка не удалился ни один файл — это отказ, а не
                // очистка нуля байт.
                result.freedBytes <= 0 && result.failedCount > 0 -> _uiEvent.emit(
                    StorageUiEvent.Error(
                        UiText.StringResource(R.string.storage_cache_clear_failed)
                    )
                )
                
                result.freedBytes <= 0 -> _uiEvent.emit(StorageUiEvent.CacheAlreadyEmpty)
                
                else -> _uiEvent.emit(StorageUiEvent.CacheCleared(result.freedBytes))
            }
        }
    }
    
    fun clearDatabase() {
        viewModelScope.launch {
            try {
                storageRepository.clearDatabaseExceptAccount()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to clear database", e)
                _uiEvent.emit(
                    StorageUiEvent.Error(
                        UiText.StringResource(R.string.storage_database_clear_failed)
                    )
                )
                return@launch
            }
            
            _uiEvent.emit(StorageUiEvent.DatabaseCleared)
            chatRepository.refreshChats()
            
            // Сообщения ушли из базы, а значит изменился размер приложения сверху.
            loadStorageInfo()
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
    
    private companion object {
        const val TAG = "StorageViewModel"
    }
}
