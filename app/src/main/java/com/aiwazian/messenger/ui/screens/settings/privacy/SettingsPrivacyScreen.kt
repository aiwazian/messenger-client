/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SettingsPrivacyScreen(privacyViewModel: SettingsPrivacyViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    
    val privacy by privacyViewModel.privacySettings.collectAsState()
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = { TopBar() },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.confidentiality))
            }) {
                SectionItem(
                    text = stringResource(R.string.bio),
                    primaryText = if (privacy.bio == PrivacyLevel.Everybody) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navHost.add(AppRoute.SettingsBio(privacy.bio))
                    })
                
                SectionItem(
                    text = stringResource(R.string.date_of_birth),
                    primaryText = if (privacy.dateOfBirth == PrivacyLevel.Everybody) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navHost.add(AppRoute.SettingsDateOfBirth(privacy.dateOfBirth))
                    })
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.confidentiality)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}
