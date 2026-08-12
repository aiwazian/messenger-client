/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import com.aiwazian.messenger.ui.components.ShareItem

data class ShareUiState(
    val sharedText: String = "",
    val targets: List<ShareItem> = emptyList(),
    val selectedChatIds: Set<Long> = emptySet(),
    val isSending: Boolean = false
)
