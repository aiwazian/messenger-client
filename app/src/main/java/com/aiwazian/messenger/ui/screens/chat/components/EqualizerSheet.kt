/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.utils.EqualizerInfo
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    info: EqualizerInfo,
    bandLevels: List<Int>,
    onBandLevelChange: (band: Int, levelMb: Int) -> Unit,
    onEditingFinished: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.equalizer),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(info.bandCount) { band ->
                val levelMb = (bandLevels.getOrNull(band) ?: 0)
                    .coerceIn(info.minLevelMb, info.maxLevelMb)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatLevel(levelMb),
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = levelMb.toFloat(),
                            onValueChange = { onBandLevelChange(band, it.roundToInt()) },
                            onValueChangeFinished = onEditingFinished,
                            valueRange = info.minLevelMb.toFloat()..info.maxLevelMb.toFloat(),
                            modifier = Modifier
                                .graphicsLayer { rotationZ = 270f }
                                .width(200.dp)
                        )
                    }

                    Text(
                        text = formatFrequency(info.centerFrequenciesMilliHz[band]),
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
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
