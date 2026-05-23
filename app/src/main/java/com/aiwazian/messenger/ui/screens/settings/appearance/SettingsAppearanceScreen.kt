/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
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
            val theme = when (viewModel.currentTheme.collectAsState().value) {
                ThemeOption.DARK -> stringResource(R.string.enabled)
                ThemeOption.LIGHT -> stringResource(R.string.disabled)
                else -> stringResource(R.string.system_default)
            }
            
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
                                .padding(8.dp)
                        ) {
                            AppPrimaryColor.entries.forEach { color ->
                                RadioButton(
                                    modifier = Modifier.scale(1.5f),
                                    selected = primaryColor == color,
                                    onClick = {
                                        viewModel.setPrimaryColor(color)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = color.color,
                                        unselectedColor = color.color,
                                    )
                                )
                            }
                        }
                    }
                }
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



