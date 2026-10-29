/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class SpoilerParticle(
    val initialX: Float,
    val initialY: Float,
    val radius: Float,
    val maxAlpha: Float,
    val animDuration: Int,
    val initialOffset: Int
)

@Composable
fun PhotoSpoilerEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 300,
    baseColor: Color = Color.Transparent
) {
    val particles = remember {
        List(particleCount) {
            SpoilerParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                radius = Random.nextFloat() * 3f + 1.5f,
                maxAlpha = Random.nextFloat() * 0.7f + 0.3f,
                animDuration = Random.nextInt(1200, 2500),
                initialOffset = Random.nextInt(0, 2000)
            )
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "PhotoSpoiler")
    
    val animValues = particles.mapIndexed { index, particle ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.animDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(particle.initialOffset)
            ),
            label = "Particle_$index"
        )
    }
    
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = baseColor)
            
            val width = size.width
            val height = size.height
            
            particles.forEachIndexed { index, particle ->
                val progress = animValues[index].value
                val currentAlpha = (particle.maxAlpha * (0.3f + 0.7f * kotlin.math.sin(progress * Math.PI).toFloat())).coerceIn(0f, 1f)
                val currentRadius = particle.radius * (0.8f + 0.4f * progress)
                
                val offsetX = particle.initialX * width + kotlin.math.sin(progress * 2 * Math.PI + index) * 8f
                val offsetY = particle.initialY * height + kotlin.math.cos(progress * 2 * Math.PI + index) * 8f
                
                drawCircle(
                    color = Color.White.copy(alpha = currentAlpha),
                    radius = currentRadius,
                    center = Offset(offsetX.toFloat(), offsetY.toFloat())
                )
            }
        }
    }
}
