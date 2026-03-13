package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.data.PrivacySettings
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.PrivacyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsPrivacyViewModel @Inject constructor(private val privacyService: PrivacyService) :
    ViewModel() {
    
    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings = _privacySettings.asStateFlow()
    
    val deleteAccountDialog = DialogController()
    
    init {
        tryLoadValues()
    }
    
    fun updateBioValue(privacyLevel: PrivacyLevel) {
        val newValue = _privacySettings.value.copy(bio = privacyLevel.ordinal)
        _privacySettings.update { newValue }
    }
    
    fun updateDateOfBirthValue(privacyLevel: PrivacyLevel) {
        val newValue = _privacySettings.value.copy(dateOfBirth = privacyLevel.ordinal)
        _privacySettings.update { newValue }
    }
    
    fun tryLoadValues() {
        viewModelScope.launch {
            try {
                val myPrivacy = privacyService.getMyPrivacy()
                
                if (myPrivacy != null) {
                    _privacySettings.update { myPrivacy }
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingPrivacyViewModel",
                    "Ошибка при получении настроек конфиденциальности",
                    e
                )
            }
        }
    }
}
