/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main.search

import com.aiwazian.messenger.domain.Search

data class SearchUiState(
    /**
     * Запрос без ведущей собачки: по нему уходит запрос на сервер и
     * подсвечиваются совпадения в выдаче. В самом поле ввода собачка
     * остаётся: текст там живёт в своём состоянии.
     */
    val query: String = "",
    val chatResults: List<Search> = emptyList(),
    val isChatLoading: Boolean = false,
    val hasMoreChats: Boolean = true,
)
