/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class CodeInputStatus {
    Default,
    Error,
    Success
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val DefaultAllowedShapes = listOf(
    MaterialShapes.Arch,
    MaterialShapes.Arrow,
    MaterialShapes.Diamond,
    MaterialShapes.Pentagon,
    MaterialShapes.Gem,
    MaterialShapes.VerySunny,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover8Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Ghostish,
)

@Composable
fun CodeInputBlocks(
    value: String,
    status: CodeInputStatus = CodeInputStatus.Default,
    length: Int = 4,
    allowedShapes: List<RoundedPolygon> = DefaultAllowedShapes,
    cellSpacing: Dp = 8.dp,
    onStatusShown: (CodeInputStatus) -> Unit = {},
) {
    val cellPolygons = remember(length) {
        mutableStateListOf<RoundedPolygon?>().apply { repeat(length) { add(null) } }
    }
    
    LaunchedEffect(value, allowedShapes) {
        for (i in 0 until length) {
            when {
                i < value.length && cellPolygons[i] == null -> {
                    val used = cellPolygons.filterNotNull().toSet()
                    val available = allowedShapes.filter { it !in used }
                    cellPolygons[i] = available.ifEmpty { allowedShapes }.random()
                }
                
                i >= value.length ->
                    cellPolygons[i] = null
            }
        }
    }
    
    LaunchedEffect(status) {
        if (status != CodeInputStatus.Default) {
            delay(300.milliseconds)
            onStatusShown(status)
        }
    }
    
    Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
        repeat(length) { index ->
            CodeCell(
                filled = index < value.length,
                polygon = cellPolygons.getOrNull(index),
                status = status
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CodeCell(
    filled: Boolean,
    polygon: RoundedPolygon?,
    status: CodeInputStatus,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val error = MaterialTheme.colorScheme.error
    
    val targetColor = when (status) {
        CodeInputStatus.Error -> error
        CodeInputStatus.Success -> primary
        CodeInputStatus.Default -> if (filled) primary else outline
    }
    
    val accentColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "accentColor"
    )
    
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(36.dp)
            .clip(MaterialTheme.shapes.small)
            .border(2.dp, accentColor, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        var lastPolygon by remember { mutableStateOf(polygon) }
        if (polygon != null) lastPolygon = polygon
        
        AnimatedVisibility(
            visible = filled && polygon != null,
            enter = scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut()
        ) {
            lastPolygon?.let { p ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(p.toShape())
                        .background(accentColor)
                )
            }
        }
    }
}
