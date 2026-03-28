/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsSecurityViewModel @Inject constructor(
    appLockManager: AppLockManager,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _deviceCount = MutableStateFlow(1)
    val deviceCount = _deviceCount.asStateFlow()

    val isEnablePasscode = appLockManager.hasPasscode

    fun init() {
        viewModelScope.launch {
            try {
                val count = sessionRepository.getDeviceCount()
                _deviceCount.update { count }
            } catch (e: Exception) {
                Log.e("SettingsSecurityViewModel", "Error init", e)
            }
        }
    }
}
