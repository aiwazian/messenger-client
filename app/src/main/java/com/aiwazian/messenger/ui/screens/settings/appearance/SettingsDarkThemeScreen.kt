/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import kotlinx.coroutines.launch

private data class ThemeItem(
    val name: String,
    val theme: ThemeOption
)

@Composable
fun SettingsDarkThemeScreen() {
    Content()
}

@Composable
private fun Content() {
    val viewModel = hiltViewModel<AppearanceViewModel>()
    
    val coroutine = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopBar()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            val selectedOption by viewModel.currentTheme.collectAsState()
            
            val themes = listOf(
                ThemeItem(
                    "Как в системе",
                    ThemeOption.SYSTEM
                ),
                ThemeItem(
                    "Включена",
                    ThemeOption.DARK
                ),
                ThemeItem(
                    "Отключена",
                    ThemeOption.LIGHT
                )
            )
            
            SectionContainer {
                themes.forEach { (name, theme) ->
                    SectionRadioItem(
                        text = name,
                        selectedOption == theme
                    ) {
                        coroutine.launch {
                            viewModel.setTheme(theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.dark_theme)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}



