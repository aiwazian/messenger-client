/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

private val ExpressiveSpatialSpec: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** Пружинистое появление: масштаб от 0.5 + проявление. */
val expressiveScaleIn: EnterTransition =
    scaleIn(initialScale = 0.5f, animationSpec = ExpressiveSpatialSpec) + fadeIn()

/** Исчезновение: масштаб до 0.5 + затухание. */
val expressiveScaleOut: ExitTransition =
    scaleOut(targetScale = 0.5f) + fadeOut()
