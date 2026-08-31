/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import com.aiwazian.messenger.utils.UiText

sealed interface StorageUiEvent {
    /**
     * Кэш очищен.
     *
     * @param freedBytes сколько реально освободилось на диске, а не сколько
     * числилось за выбранными категориями. Всегда больше нуля.
     */
    data class CacheCleared(val freedBytes: Long) : StorageUiEvent
    
    /** Удалять было нечего: выбранные категории оказались пусты. */
    data object CacheAlreadyEmpty : StorageUiEvent
    
    data object DatabaseCleared : StorageUiEvent
    
    data class Error(val message: UiText) : StorageUiEvent
}
