package com.aiwazian.messenger.services

import com.aiwazian.messenger.enums.PrimaryColorOption
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
class ThemeService @Inject constructor() {
    
    private val _currentTheme = MutableStateFlow(ThemeOption.SYSTEM)
    val currentTheme = _currentTheme.asStateFlow()
    
    private val _primaryColor = MutableStateFlow(PrimaryColorOption.Blue)
    val primaryColor = _primaryColor.asStateFlow()
    
    private val _dynamicColor = MutableStateFlow(false)
    val dynamicColor = _dynamicColor.asStateFlow()
    
    private val dataStorage = DataStoreManager.Companion.getInstance()
    
    init {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        
        coroutineScope.launch {
            val theme = dataStorage.getTheme().first()
            _currentTheme.update { ThemeOption.fromString(theme) }
        }
        
        coroutineScope.launch {
            val primaryColor = dataStorage.getPrimaryColor().first()
            _primaryColor.update { PrimaryColorOption.fromString(primaryColor) }
        }
        
        coroutineScope.launch {
            val dynamicColor = dataStorage.getDynamicColor().first()
            _dynamicColor.update { dynamicColor }
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
    
    suspend fun setPrimaryColor(color: PrimaryColorOption) {
        _primaryColor.update { color }
        dataStorage.savePrimaryColor(color.name)
    }
}