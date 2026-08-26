/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Системный таббар приложения: таблетка со скруглённым индикатором-пилюлей
 * вместо подчёркивания и без разделителя снизу.
 *
 * Вынесен из MainScreen: там он был сшит с папками чатов, и любой второй экран
 * с вкладками пришлось бы переносить те же тридцать строк разметки скопом.
 *
 * Содержимое вкладок остаётся за вызывающей стороной: у папок есть бейдж
 * непрочитанных и долгое нажатие, у галереи чата — просто название.
 * Простой случай закрывает [AppTab].
 */
@Composable
fun AppPrimaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    edgePadding: Dp = 4.dp,
    tabs: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier
                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp)
                .clip(CircleShape)
                .width(IntrinsicSize.Max),
            contentColor = contentColor,
            containerColor = containerColor,
            edgePadding = edgePadding,
            indicator = {
                Column(
                    Modifier
                        .tabIndicatorOffset(selectedTabIndex)
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                ) {}
            },
            divider = {},
            tabs = tabs
        )
    }
}

/**
 * Вкладка для [AppPrimaryScrollableTabRow] без дополнительного содержимого.
 *
 * Цвет и нажатие те же, что у вкладок папок на главном экране, иначе один и
 * тот же элемент вел бы себя в двух местах по-разному.
 */
@Composable
fun AppTab(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.96f else 1f,
        label = "app_tab_scale_animation"
    )
    val accentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
    
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .widthIn(min = TabRowDefaults.ScrollableTabRowMinTabWidth)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = accentColor,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
