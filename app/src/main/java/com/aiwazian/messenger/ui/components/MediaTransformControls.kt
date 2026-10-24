/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.utils.media.MediaTransform

@Composable
fun MediaTransformControls(
    state: MediaTransformState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    resetVisible: Boolean = state.isChanged,
    onReset: () -> Unit = {},
    onFlip: suspend (MediaTransform) -> Unit = {},
    onRotate: suspend (MediaTransform) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MediaOverlayControl {
                MediaFlipButton(state = state, onApplied = onFlip)
            }
            
            MediaOverlayControl {
                MediaRotateButton(state = state, onApplied = onRotate)
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MediaOverlayControl {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.cancel).uppercase())
                }
            }
            
            AnimatedVisibility(
                visible = resetVisible,
                enter = expressiveScaleIn,
                exit = expressiveScaleOut
            ) {
                MediaOverlayControl {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(text = stringResource(R.string.reset).uppercase())
                    }
                }
            }
            
            MediaOverlayControl {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(text = stringResource(R.string.done).uppercase())
                }
            }
        }
    }
}
