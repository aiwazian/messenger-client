/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main.search

import com.aiwazian.messenger.domain.DownloadItem
import com.aiwazian.messenger.domain.Search

data class SearchUiState(
    val query: String = "",
    val chatResults: List<Search> = emptyList(),
    val isChatLoading: Boolean = false,
    val hasMoreChats: Boolean = true,
)
