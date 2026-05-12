/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.net.Uri
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.UiText

data class ProfileUiState(
    val id: Long = -1,
    val profile: Profile? = null,
    val title: UiText = UiText.DynamicString(""),
    val subTitle: UiText = UiText.DynamicString(""),
    val avatars: List<Uri?> = emptyList(),
    val actions: List<TopBarAction> = emptyList(),
    val showLeaveDialog: Boolean = false,
    val myId: Long = -1,
)
