/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import android.net.Uri
import com.aiwazian.messenger.ui.components.ShareItem

data class ShareUiState(
    val sharedText: String = "",
    val sharedFiles: List<Uri> = emptyList(),
    val targets: List<ShareItem> = emptyList(),
    val selectedChatIds: Set<Long> = emptySet(),
    val isSending: Boolean = false
)
