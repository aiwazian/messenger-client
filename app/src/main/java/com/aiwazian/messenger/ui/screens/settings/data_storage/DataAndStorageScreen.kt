/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun DataAndStorageScreen() {
    val navBackStack = LocalNavBackStack.current
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                title = { Text(text = stringResource(R.string.data_and_storage)) }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SectionContainer {
                SectionItem(
                    headlineText = "Очистка памяти",
                    onClick = {
                        navBackStack.add(AppRoute.SettingsStorage)
                    }
                )
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = "Автозагрузка медиа",
                    onClick = {
                        navBackStack.add(AppRoute.SettingsAutoDownloadMedia)
                    }
                )
            }
        }
    }
}
