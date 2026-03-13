package com.aiwazian.messenger.services

import android.content.Context
import android.content.res.Configuration
import com.aiwazian.messenger.enums.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class LanguageService(private val context: Context) {
    
    private val _languageCode = MutableStateFlow(Language.EN)
    val languageCode = _languageCode.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val dataStorage = DataStoreManager.Companion.getInstance()
    
    init {
        coroutineScope.launch {
            _languageCode.update {
                Language.Companion.fromString(
                    dataStorage.getLanguage().first()
                )
            }
        }
    }
    
    fun selLanguage(language: Language) {
        val languageCode = language.toString().lowercase()
        selLanguage(languageCode)
    }
    
    fun selLanguage(language: String): Context? {
        val languageCode = language.lowercase()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
    
    suspend fun saveLanguage(language: Language) {
        dataStorage.saveLanguage(language)
    }
}