/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val themeManager: ThemeManager) :
    ViewModel() {
    
    val dynamicColor = themeManager.dynamicColor
    
    val primaryColor = themeManager.appPrimaryColor
    
    val currentTheme = themeManager.currentTheme
    
    fun setDynamicColor(isEnable: Boolean) {
        viewModelScope.launch {
            themeManager.setDynamicColor(isEnable)
        }
    }
    
    fun setPrimaryColor(color: AppPrimaryColor) {
        viewModelScope.launch {
            themeManager.setPrimaryColor(color)
        }
    }
    
    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            themeManager.setTheme(theme)
        }
    }
}
