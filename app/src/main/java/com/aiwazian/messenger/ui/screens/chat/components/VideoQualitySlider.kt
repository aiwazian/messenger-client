/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.utils.media.VideoQuality
import kotlin.math.roundToInt

/**
 * Ступени сжатия видео: дискретный слайдер с подписью над каждым делением.
 *
 * Ступеней всегда столько, сколько помещается в исходное разрешение: для
 * 1280 на 720 их три, для 854 на 480 — две. Меньше двух выбирать не из чего,
 * поэтому такой слайдер не рисуется вовсе.
 *
 * Свой трек, а не штатный: слайдер лежит поверх видео, и на светлом кадре
 * тематический бегунок терялся бы — так же, как у полосы воспроизведения.
 */
@Composable
internal fun VideoQualitySlider(
    stops: List<VideoQuality>,
    selected: VideoQuality,
    onSelect: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    if (stops.size < 2) {
        return
    }
    
    val selectedIndex = stops.indexOf(selected).coerceAtLeast(0)
    val lastIndex = stops.size - 1
    
    Column(modifier = modifier.fillMaxWidth()) {
        StopsRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TRACK_INSET)
        ) {
            stops.forEachIndexed { index, quality ->
                Text(
                    text = quality.label, color = if (index == selectedIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }, style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { value ->
                val index = value.roundToInt().coerceIn(stops.indices)
                
                if (index != selectedIndex) {
                    onSelect(stops[index])
                }
            },
            valueRange = 0f..lastIndex.toFloat(),
            // Ступеней четыре — промежуточных делений два: крайние в steps не входят.
            steps = stops.size - 2,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(THUMB_SIZE)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                )
            },
            track = {
                val fraction = selectedIndex.toFloat() / lastIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TICK_SIZE), contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TRACK_HEIGHT)
                            .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    
                    StopsRow(modifier = Modifier.fillMaxWidth()) {
                        stops.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(TICK_SIZE)
                                    .clip(CircleShape)
                                    .background(
                                        if (index <= selectedIndex) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }
                }
            })
    }
}

/**
 * Ставит детей серединами на равные доли ширины.
 *
 * Обычный Row с SpaceBetween расставил бы их краями к краям, и «360p» ушла бы
 * правее своего деления на половину своей ширины, а «1080p» — левее.
 *
 * Крайние всё же загоняются в границы: без этого половина первой подписи
 * уезжала бы за край экрана.
 */
@Composable
private fun StopsRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val lastIndex = (placeables.size - 1).coerceAtLeast(1)
        
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val center = width * index / lastIndex
                val limit = (width - placeable.width).coerceAtLeast(0)
                
                placeable.place(
                    x = (center - placeable.width / 2).coerceIn(0, limit),
                    y = (height - placeable.height) / 2
                )
            }
        }
    }
}

private val THUMB_SIZE = 15.dp
private val TRACK_HEIGHT = 4.dp
private val TICK_SIZE = 8.dp

/** На столько трек уже самого слайдера: половина бегунка с каждой стороны. */
private val TRACK_INSET = THUMB_SIZE / 2
