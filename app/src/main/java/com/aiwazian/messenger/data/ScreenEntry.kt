package com.aiwazian.messenger.data

import androidx.compose.runtime.Composable

data class ScreenEntry(
    val content: @Composable () -> Unit,
    val canGoBackBySwipe: Boolean = true
)
