package com.aiwazian.messenger.ui.screens.settings.storage

import com.aiwazian.messenger.utils.UiText

sealed interface StorageUiEvent {
    val message: UiText
    
    data class CacheCleared(override val message: UiText) : StorageUiEvent
    
    data class CacheAlreadyEmpty(override val message: UiText) : StorageUiEvent
    
    data class DatabaseCleared(override val message: UiText) : StorageUiEvent
    
    data class Error(override val message: UiText) : StorageUiEvent
}
