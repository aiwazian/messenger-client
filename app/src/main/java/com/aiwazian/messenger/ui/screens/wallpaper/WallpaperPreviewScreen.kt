/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.wallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.WallpaperTarget
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.WallpaperImage
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun WallpaperPreviewScreen(
    imagePath: String,
    target: WallpaperTarget,
    viewModel: WallpaperViewModel = hiltViewModel()
) {
    LaunchedEffect(target, imagePath) {
        viewModel.init(target, imagePath)
    }
    
    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                WallpaperUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is WallpaperUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message.asString(context),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(stringResource(R.string.wallpaper)) }
        )
    }, snackbarHost = {
        AppSnackbar(snackbarHostState)
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            TopBarScrim(height = innerPadding.calculateTopPadding())
            BottomBarScrim(height = innerPadding.calculateBottomPadding())
            
            uiState.draftPath?.let { draftPath ->
                WallpaperImage(
                    model = draftPath,
                    darkenPercent = (uiState.darken * PERCENT).toInt(),
                    blurPercent = (uiState.blur * PERCENT).toInt(),
                    offsetX = uiState.offsetX,
                    offsetY = uiState.offsetY,
                    onOffsetChange = viewModel::setOffset,
                    modifier = Modifier.matchParentSize()
                )
            }
            
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(10.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                WallpaperSliderRow(
                    label = stringResource(R.string.wallpaper_darkness),
                    value = uiState.darken,
                    onValueChange = viewModel::setDarken
                )
                
                WallpaperSliderRow(
                    label = stringResource(R.string.wallpaper_blur),
                    value = uiState.blur,
                    onValueChange = viewModel::setBlur
                )
                
                if (target is WallpaperTarget.Chat) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = viewModel::applyForMe,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.set_wallpaper_for_me))
                        }
                        
                        Button(
                            onClick = viewModel::applyForBoth,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.set_wallpaper_for_everyone))
                        }
                    }
                } else {
                    Button(
                        onClick = viewModel::apply,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label ${(value * PERCENT).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        val sliderState = rememberSliderState(
            value = value,
            trackRange = 0f..0.9f
        )
        
        LaunchedEffect(value) {
            sliderState.value = value
        }
        
        Slider(
            state = sliderState,
            onValueChange = onValueChange
        )
    }
}

private const val PERCENT = 100
