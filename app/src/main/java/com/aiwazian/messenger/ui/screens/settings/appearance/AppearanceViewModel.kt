/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val themeManager: ThemeManager) :
    ViewModel() {
    
    val dynamicColor = themeManager.dynamicColor
    
    val primaryColor = themeManager.primaryColor
    
    val currentTheme = themeManager.currentTheme
    
    suspend fun setDynamicColor(isEnable: Boolean) {
        themeManager.setDynamicColor(isEnable)
    }
    
    suspend fun setPrimaryColor(color: AppPrimaryColor) {
        themeManager.setPrimaryColor(color)
    }
    
    suspend fun setTheme(theme: ThemeOption) {
        themeManager.setTheme(theme)
    }
}



