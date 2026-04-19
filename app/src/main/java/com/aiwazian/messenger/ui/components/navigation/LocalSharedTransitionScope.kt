/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
    error(
        "LocalSharedTransitionScope не предоставлен. Убедитесь, что NavDisplay обёрнут в SharedTransitionLayout."
    )
}
