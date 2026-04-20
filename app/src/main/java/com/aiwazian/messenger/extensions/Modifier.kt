/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.aiwazian.messenger.ui.components.navigation.LocalSharedTransitionScope

/**
 * Применяет анимацию shared element к composable.
 *
 * Оба composable, которые должны быть «shared», обязаны использовать одинаковый [key].
 * Используй [sharedElement] когда контент на обоих экранах визуально идентичен
 * (например, одна и та же картинка).
 *
 * @param key уникальный ключ, связывающий элементы на двух экранах
 * @param zIndexInOverlay z-порядок элемента в оверлее во время перехода
 */
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

/**
 * Применяет анимацию shared bounds к composable.
 *
 * Оба composable, которые должны делить bounds, обязаны использовать одинаковый [key].
 * Используй [sharedBounds] когда контент на экранах визуально разный, но занимает одну область
 * (например, карточка → полноэкранный экран / container transform).
 *
 * @param key уникальный ключ, связывающий области на двух экранах
 * @param enter анимация появления контента нового экрана внутри shared bounds
 * @param exit анимация исчезновения контента старого экрана внутри shared bounds
 * @param zIndexInOverlay z-порядок элемента в оверлее во время перехода
 * @param resizeMode режим изменения размера во время анимации
 */
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