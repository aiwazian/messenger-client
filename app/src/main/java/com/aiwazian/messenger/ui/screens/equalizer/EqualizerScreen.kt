/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel = hiltViewModel()) {
    val info by viewModel.info.collectAsState()
    val bandLevels by viewModel.bandLevels.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.equalizer))
                }
            )
        }
    ) { _ ->
        val equalizerInfo = info
        if (equalizerInfo == null) {
            Spacer(Modifier.fillMaxSize())
            return@AppScaffold
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(equalizerInfo.bandCount) { band ->
                val levelMb = (bandLevels.getOrNull(band) ?: 0)
                    .coerceIn(equalizerInfo.minLevelMb, equalizerInfo.maxLevelMb)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatLevel(levelMb),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = levelMb.toFloat(),
                            onValueChange = {
                                viewModel.setBandLevel(band, it.roundToInt())
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onValueChangeFinished = viewModel::persistBandLevels,
                            valueRange = equalizerInfo.minLevelMb.toFloat()..equalizerInfo.maxLevelMb.toFloat(),
                            modifier = Modifier
                                .graphicsLayer { rotationZ = 270f }
                                .width(320.dp)
                        )
                    }

                    Text(
                        text = formatFrequency(equalizerInfo.centerFrequenciesMilliHz[band]),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatLevel(levelMb: Int): String {
    val db = levelMb / 100f
    return if (db == 0f) "0 dB" else String.format(Locale.US, "%+.1f dB", db)
}

private fun formatFrequency(milliHz: Int): String {
    val hz = milliHz / 1000f
    return if (hz >= 1000f) {
        String.format(Locale.US, "%.1f kHz", hz / 1000f)
    } else {
        "${hz.roundToInt()} Hz"
    }
}
