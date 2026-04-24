/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

sealed class StorageUiEvent {
    data object CacheCleared : StorageUiEvent()
    data object DatabaseCleared : StorageUiEvent()
    data class Error(val message: String) : StorageUiEvent()
}
