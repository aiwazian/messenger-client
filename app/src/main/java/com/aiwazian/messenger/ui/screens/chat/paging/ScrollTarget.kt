/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.paging

data class ScrollTarget(
    val messageId: Long?,
    val highlight: Boolean = false,
    val animate: Boolean = false,
    val viewportFraction: Float = 1f / 3f,
    val requestId: Long = System.nanoTime()
)
