/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SettingsAppearanceScreen(viewModel: AppearanceViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    
    val primaryColor by viewModel.primaryColor.collectAsState()
    val isDynamicColorEnable by viewModel.dynamicColor.collectAsState()
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopBar()
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            AppearanceChatPreview()
            
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.color_theme))
            }) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SectionToggleItem(
                        text = stringResource(R.string.dynamic_color),
                        isChecked = isDynamicColorEnable,
                        onCheckedChange = {
                            viewModel.setDynamicColor(!isDynamicColorEnable)
                        })
                }
                
                AnimatedContent(
                    targetState = isDynamicColorEnable,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }) { enableDynamicColor ->
                    if (!enableDynamicColor) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AppPrimaryColor.entries.forEach { color ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                    targetValue = if (isPressed) 0.9f else 1f,
                                    label = "radio_button_scale_animation"
                                )
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer(scaleX = scale, scaleY = scale)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 3.dp,
                                            shape = CircleShape,
                                            color = color.color
                                        )
                                        .clickable(
                                            onClick = { viewModel.setPrimaryColor(color) },
                                            indication = null,
                                            interactionSource = interactionSource
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = primaryColor == color,
                                        enter = expressiveScaleIn,
                                        exit = expressiveScaleOut,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(color.color)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            val theme = when (viewModel.currentTheme.collectAsState().value) {
                ThemeOption.DARK -> stringResource(R.string.enabled)
                ThemeOption.LIGHT -> stringResource(R.string.disabled)
                else -> stringResource(R.string.system_default)
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.dark_theme),
                    trailingText = theme,
                    onClick = {
                        navBackStack.add(AppRoute.SettingsDarkTheme)
                    })
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = {
            Text(stringResource(R.string.appearance))
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        )
    )
}



