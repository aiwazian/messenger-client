/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Переходы по результатам поиска.
 *
 * Вверх — к более старому совпадению, вниз — к более новому: направление кнопки
 * совпадает с направлением прокрутки чата, а не с номером результата.
 */
@Composable
fun ChatSearchNavigationButtons(
    canGoOlder: Boolean,
    canGoNewer: Boolean,
    onOlderClick: () -> Unit,
    onNewerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchNavigationButton(
            icon = Icons.Rounded.KeyboardArrowUp,
            enabled = canGoOlder,
            onClick = onOlderClick
        )
        
        SearchNavigationButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            enabled = canGoNewer,
            onClick = onNewerClick
        )
    }
}

@Composable
private fun SearchNavigationButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed && enabled) 0.9f else 1f,
        label = "search_navigation_button_scale_animation"
    )
    
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        /* Кнопка без результатов в свою сторону остаётся на месте, но гаснет. */
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
        shape = CircleShape,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Icon(icon, null)
    }
}

/** Прозрачность содержимого отключённой кнопки по гайдлайнам Material. */
private const val DISABLED_ALPHA = 0.38f
