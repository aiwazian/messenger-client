/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay

val PredictiveBackMetadata = metadata {
    put(NavDisplay.TransitionKey) {
        fadeIn(tween(500)) togetherWith fadeOut(tween(500))
    }
    put(NavDisplay.PopTransitionKey) {
        fadeIn(tween(500)) togetherWith fadeOut(tween(500))
    }
    put(NavDisplay.PredictivePopTransitionKey) {
        fadeIn(tween(500)) togetherWith fadeOut(tween(500))
    }
}


val HorizontalMetadata = metadata {
    put(NavDisplay.TransitionKey) {
        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
    }
    put(NavDisplay.PopTransitionKey) {
        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
    }
    put(NavDisplay.PredictivePopTransitionKey) {
        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
    }
}