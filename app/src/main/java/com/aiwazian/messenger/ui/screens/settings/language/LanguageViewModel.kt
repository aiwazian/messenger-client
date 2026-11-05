/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.language

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.LanguagePackManager
import com.aiwazian.messenger.domain.LanguagePackStatus
import com.aiwazian.messenger.enums.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languagePackManager: LanguagePackManager
) : ViewModel() {
    
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromString(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    )
    val currentLanguage = _currentLanguage.asStateFlow()
    
    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState = _uiState.asStateFlow()
    
    private var downloadJob: Job? = null
    
    fun selectLanguage(language: AppLanguage) {
        if (_currentLanguage.value == language) {
            return
        }
        if (languagePackManager.isLanguageInstalled(language.code)) {
            applyLanguage(language)
            return
        }
        startDownload(language)
    }
    
    fun showStopDialog() {
        _uiState.update { it.copy(showStopDialog = true) }
    }
    
    fun hideStopDialog() {
        _uiState.update { it.copy(showStopDialog = false) }
    }
    
    fun confirmStopDownload() {
        languagePackManager.cancelDownload()
        downloadJob?.cancel()
        _uiState.update {
            it.copy(
                downloadingLanguage = null,
                showStopDialog = false,
                requiresConfirmation = false
            )
        }
    }
    
    fun launchUserConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (languagePackManager.startUserConfirmation(launcher)) {
            _uiState.update { it.copy(requiresConfirmation = false) }
        }
    }
    
    fun onConfirmationResult() {
        _uiState.update { it.copy(requiresConfirmation = false) }
    }
    
    private fun startDownload(language: AppLanguage) {
        downloadJob?.cancel()
        _uiState.update {
            it.copy(
                downloadingLanguage = language,
                showStopDialog = false,
                requiresConfirmation = false
            )
        }
        downloadJob = viewModelScope.launch {
            languagePackManager.downloadLanguage(language.code).collect { status ->
                when (status) {
                    is LanguagePackStatus.Downloading -> Unit
                    is LanguagePackStatus.Installing -> Unit
                    is LanguagePackStatus.RequiresConfirmation -> _uiState.update {
                        it.copy(requiresConfirmation = true)
                    }
                    
                    is LanguagePackStatus.Installed -> onLanguageInstalled(language)
                    is LanguagePackStatus.Canceled -> hideDownloadOverlay()
                    is LanguagePackStatus.Failed -> onDownloadFailed(language)
                }
            }
        }
    }
    
    private fun onLanguageInstalled(language: AppLanguage) {
        hideDownloadOverlay()
        applyLanguage(language)
    }
    
    private fun onDownloadFailed(language: AppLanguage) {
        hideDownloadOverlay()
        applyLanguage(language)
    }
    
    private fun hideDownloadOverlay() {
        _uiState.update {
            it.copy(downloadingLanguage = null, requiresConfirmation = false)
        }
    }
    
    private fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        _currentLanguage.update { language }
    }
}
