/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import com.aiwazian.messenger.enums.ChatType

sealed class ProfileUiEffect {
    data object NavigateBack : ProfileUiEffect()
    data object NavigateToMain : ProfileUiEffect()
    data object NavigateToUserSettings : ProfileUiEffect()
    data class NavigateToGroupSettings(val chatId: Long) : ProfileUiEffect()
    data class NavigateToChannelSettings(val chatId: Long) : ProfileUiEffect()
    data class ShowLeaveDialog(
        val profileName: String,
        val chatType: ChatType
    ) : ProfileUiEffect()
    
    data object HideLeaveDialog : ProfileUiEffect()
}