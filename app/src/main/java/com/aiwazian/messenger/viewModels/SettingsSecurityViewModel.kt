package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.services.AppLockService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsSecurityViewModel @Inject constructor(appLockService: AppLockService) :
    ViewModel() {
    
    private val _deviceCount = MutableStateFlow(1)
    val deviceCount = _deviceCount.asStateFlow()
    
    val isEnablePasscode = appLockService.hasPasscode
    
    fun init() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getDeviceCount()
                
                if (response.isSuccessful) {
                    _deviceCount.update { response.body() ?: 1 }
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsSecurityViewModel",
                    "Error init",
                    e
                )
            }
        }
    }
}