/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.repository.PrivacyRepository
import com.aiwazian.messenger.utils.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsPrivacyViewModel @Inject constructor(
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings = _privacySettings.asStateFlow()

    val deleteAccountDialog = DialogController()

    init {
        tryLoadValues()
    }

    fun updateBioValue(privacyLevel: Int) {
        val newValue = _privacySettings.value.copy(bio = privacyLevel)
        _privacySettings.update { newValue }
    }

    fun updateDateOfBirthValue(privacyLevel: Int) {
        val newValue = _privacySettings.value.copy(dateOfBirth = privacyLevel)
        _privacySettings.update { newValue }
    }

    fun tryLoadValues() {
        viewModelScope.launch {
            try {
                val myPrivacy = privacyRepository.getPrivacySettings()

                if (myPrivacy != null) {
                    _privacySettings.update { myPrivacy }
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsPrivacyViewModel",
                    "Ошибка при получении настроек конфиденциальности",
                    e
                )
            }
        }
    }
}



