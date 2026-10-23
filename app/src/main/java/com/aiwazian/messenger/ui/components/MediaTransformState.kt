/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import com.aiwazian.messenger.utils.media.MediaTransform
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Stable
class MediaTransformState internal constructor(
    initial: MediaTransform,
    private val bakesContent: Boolean
) {
    var transform by mutableStateOf(initial)
        private set
    
    var isAnimating by mutableStateOf(false)
        private set
    
    val isChanged: Boolean
        get() = !transform.isIdentity
    
    val contentRotation: Float
        get() = rotation.value
    
    val contentScaleX: Float
        get() = if (bakesContent) flipScale.value else flipScale.value * transform.mirrorScaleX
    
    private val rotation = Animatable(initial.rotationDegrees.toFloat())
    private val flipScale = Animatable(1f)

    suspend fun rotate(onApplied: suspend (MediaTransform) -> Unit = {}) {
        if (isAnimating) {
            return
        }
        
        isAnimating = true
        
        try {
            rotation.animateTo(
                targetValue = rotation.value - MediaTransform.QUARTER_TURN,
                animationSpec = rotationSpec()
            )
            
            transform = transform.rotated()
            onApplied(transform)
            
            if (bakesContent) {
                rotation.snapTo(0f)
            }
        } finally {
            isAnimating = false
        }
    }

    suspend fun flip(onApplied: suspend (MediaTransform) -> Unit = {}) {
        if (isAnimating) {
            return
        }
        
        isAnimating = true
        
        try {
            flipScale.animateTo(targetValue = 0f, animationSpec = flipSpec())
            
            transform = transform.mirrored()
            onApplied(transform)
            
            if (!bakesContent) {
                rotation.snapTo(-rotation.value)
            }
            
            flipScale.animateTo(targetValue = 1f, animationSpec = flipSpec())
        } finally {
            isAnimating = false
        }
    }

    suspend fun reset(onApplied: suspend (MediaTransform) -> Unit = {}) {
        if (isAnimating || !isChanged) {
            return
        }
        
        isAnimating = true
        
        try {
            val wasMirrored = transform.isMirrored
            
            if (wasMirrored) {
                flipScale.animateTo(targetValue = 0f, animationSpec = flipSpec())
            }
            
            transform = MediaTransform.None
            onApplied(MediaTransform.None)
            
            if (bakesContent) {
                rotation.snapTo(0f)
                flipScale.snapTo(1f)
                return
            }
            
            coroutineScope {
                if (wasMirrored) {
                    launch { flipScale.animateTo(1f, flipSpec()) }
                }
                
                launch { rotation.animateTo(nearestZeroTurn(rotation.value), rotationSpec()) }
            }
        } finally {
            isAnimating = false
        }
    }
    
    private fun rotationSpec(): AnimationSpec<Float> = tween(
        durationMillis = ROTATE_DURATION_MS, easing = FastOutSlowInEasing
    )
    
    private fun flipSpec(): AnimationSpec<Float> = tween(
        durationMillis = FLIP_DURATION_MS, easing = FastOutSlowInEasing
    )
    
    private companion object {
        const val ROTATE_DURATION_MS = 300
        const val FLIP_DURATION_MS = 150
    }
}

@Composable
fun rememberMediaTransformState(
    initial: MediaTransform = MediaTransform.None,
    bakesContent: Boolean = false,
    key: Any? = Unit
): MediaTransformState = remember(key) { MediaTransformState(initial, bakesContent) }

fun Modifier.mediaTransform(
    state: MediaTransformState,
    contentSize: Size,
    containerSize: Size? = null
): Modifier =
    this.graphicsLayer {
        val degrees = state.contentRotation
        val fit = fitScaleWhileRotating(containerSize ?: size, contentSize, degrees)

        rotationZ = degrees
        scaleX = fit * state.contentScaleX
        scaleY = fit
    }

@Composable
fun MediaFlipButton(
    state: MediaTransformState,
    modifier: Modifier = Modifier,
    onApplied: suspend (MediaTransform) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    IconButton(
        onClick = { scope.launch { state.flip(onApplied) } },
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Icon(imageVector = Icons.Rounded.Flip, contentDescription = null)
    }
}

@Composable
fun MediaRotateButton(
    state: MediaTransformState,
    modifier: Modifier = Modifier,
    onApplied: suspend (MediaTransform) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    IconButton(
        onClick = { scope.launch { state.rotate(onApplied) } },
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Icon(imageVector = Icons.Rounded.Rotate90DegreesCcw, contentDescription = null)
    }
}

private fun fitScaleWhileRotating(container: Size, content: Size, degrees: Float): Float {
    if (container.width <= 0f || container.height <= 0f) {
        return 1f
    }
    
    if (content.width <= 0f || content.height <= 0f) {
        return 1f
    }
    
    val fit = min(container.width / content.width, container.height / content.height)
    val width = content.width * fit
    val height = content.height * fit
    
    val radians = degrees * PI / STRAIGHT_ANGLE
    val cos = abs(cos(radians)).toFloat()
    val sin = abs(sin(radians)).toFloat()
    
    val rotatedWidth = width * cos + height * sin
    val rotatedHeight = width * sin + height * cos
    
    if (rotatedWidth <= 0f || rotatedHeight <= 0f) {
        return 1f
    }
    
    return min(container.width / rotatedWidth, container.height / rotatedHeight)
        .coerceAtMost(1f)
}

private fun nearestZeroTurn(degrees: Float): Float =
    (degrees / FULL_TURN).roundToInt() * FULL_TURN.toFloat()

private const val FULL_TURN = 360
private const val STRAIGHT_ANGLE = 180
