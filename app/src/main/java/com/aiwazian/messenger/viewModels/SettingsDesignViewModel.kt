package com.aiwazian.messenger.viewModels

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.enums.PrimaryColorOption
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.services.ThemeService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsDesignViewModel @Inject constructor(private val themeService: ThemeService) :
    ViewModel() {
    
    val dynamicColor = themeService.dynamicColor
    
    val primaryColor = themeService.primaryColor
    
    val currentTheme = themeService.currentTheme
    
    suspend fun setDynamicColor(isEnable: Boolean) {
        themeService.setDynamicColor(isEnable)
    }
    
    suspend fun setPrimaryColor(color: PrimaryColorOption) {
        themeService.setPrimaryColor(color)
    }
    
    suspend fun setTheme(theme: ThemeOption) {
        themeService.setTheme(theme)
    }
}
