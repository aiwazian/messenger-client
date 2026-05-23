/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.os.Build
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val dataStorage: DataStoreManager
) {
    
    private val _currentTheme = MutableStateFlow(ThemeOption.SYSTEM)
    val currentTheme = _currentTheme.asStateFlow()
    
    private val _appPrimaryColor = MutableStateFlow(AppPrimaryColor.Blue)
    val appPrimaryColor = _appPrimaryColor.asStateFlow()
    
    private val _dynamicColor = MutableStateFlow(false)
    val dynamicColor = _dynamicColor.asStateFlow()
    
    init {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        
        coroutineScope.launch {
            val theme = dataStorage.getTheme().first()
            _currentTheme.update { ThemeOption.valueOf(theme) }
        }
        
        coroutineScope.launch {
            val primaryColor = dataStorage.getPrimaryColor().first()
            _appPrimaryColor.update { AppPrimaryColor.valueOf(primaryColor) }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            coroutineScope.launch {
                val dynamicColor = dataStorage.getDynamicColor().first()
                _dynamicColor.update { dynamicColor }
            }
        }
    }
    
    suspend fun setDynamicColor(dynamicColor: Boolean) {
        _dynamicColor.update { dynamicColor }
        dataStorage.saveDynamicColor(dynamicColor)
    }
    
    suspend fun setTheme(theme: ThemeOption) {
        _currentTheme.update { theme }
        dataStorage.saveTheme(theme)
    }
    
    suspend fun setPrimaryColor(color: AppPrimaryColor) {
        _appPrimaryColor.update { color }
        dataStorage.savePrimaryColor(color)
    }
}
