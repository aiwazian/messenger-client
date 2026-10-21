/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.aiwazian.messenger.ui.components.navigation.LocalSharedTransitionScope

@Composable
fun Modifier.sharedElement(
    key: Any,
    zIndexInOverlay: Float = 0f,
    placeHolderSize: SharedTransitionScope.PlaceholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    return with(sharedTransitionScope) {
        this@sharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedContentScope,
            zIndexInOverlay = zIndexInOverlay,
            placeholderSize = placeHolderSize
        )
    }
}

@Composable
fun Modifier.sharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    zIndexInOverlay: Float = 0f,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
    placeHolderSize: SharedTransitionScope.PlaceholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    return with(sharedTransitionScope) {
        this@sharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedContentScope,
            enter = enter,
            exit = exit,
            zIndexInOverlay = zIndexInOverlay,
            resizeMode = resizeMode,
            placeholderSize = placeHolderSize
        )
    }
}


fun Modifier.parallax(scrollState: ScrollState, rate: Int = 2): Modifier = this.graphicsLayer {
    translationY = if (rate > 0) scrollState.value.toFloat() / rate else scrollState.value.toFloat()
}
