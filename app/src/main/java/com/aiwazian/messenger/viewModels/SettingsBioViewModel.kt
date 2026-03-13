package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.enums.PrivacyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsBioViewModel : ViewModel() {
    
    private val _initialLevel = MutableStateFlow(PrivacyLevel.Everybody)
    
    private val _currentLevel = MutableStateFlow(PrivacyLevel.Everybody)
    val currentLevel = _currentLevel.asStateFlow()
    
    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton = _showSaveButton.asStateFlow()
    
    fun init(initialValue: PrivacyLevel) {
        _initialLevel.update { initialValue }
        _currentLevel.update { initialValue }
        hideSaveButton()
    }
    
    fun selectValue(value: PrivacyLevel) {
        _currentLevel.update { value }
        
        if (_currentLevel.value == _initialLevel.value) {
            hideSaveButton()
        } else {
            showSaveButton()
        }
    }
    
    suspend fun trySave(): Boolean {
        try {
            val response = RetrofitInstance.api.changeBioPrivacy(_currentLevel.value.ordinal)
            
            return response.isSuccessful
        } catch (e: Exception) {
            Log.e(
                "SettingsBioViewModel",
                "Ошибка при отправке настроек конфиденциальности для раздела о себе",
                e
            )
            
            return false
        }
    }
    
    private fun showSaveButton() {
        _showSaveButton.update { true }
    }
    
    private fun hideSaveButton() {
        _showSaveButton.update { false }
    }
}