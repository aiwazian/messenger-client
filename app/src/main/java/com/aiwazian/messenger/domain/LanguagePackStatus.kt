/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

sealed interface LanguagePackStatus {
    data object Downloading : LanguagePackStatus
    data object Installing : LanguagePackStatus
    data object RequiresConfirmation : LanguagePackStatus
    data object Installed : LanguagePackStatus
    data class Failed(val errorCode: Int) : LanguagePackStatus
    data object Canceled : LanguagePackStatus
}
