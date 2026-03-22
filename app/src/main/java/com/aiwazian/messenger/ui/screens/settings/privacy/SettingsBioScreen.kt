/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.launch

@Composable
fun SettingsBioScreen(privacyViewModel: SettingsPrivacyViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    
    val settingsBioViewModel = hiltViewModel<SettingsBioViewModel>()
    val privacy by privacyViewModel.privacySettings.collectAsState()
    
    val currentValue by settingsBioViewModel.currentLevel.collectAsState()
    val showSaveButton by settingsBioViewModel.showSaveButton.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()
    
    val actions = if (showSaveButton) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = {
                    scope.launch {
                        val isSaved = settingsBioViewModel.trySave()

                        if (isSaved) {
                            privacyViewModel.updateBioValue(currentValue.ordinal)
                            navHost.removeLastOrNull()
                        } else {
                            settingsBioViewModel.vibrate(VibrationPattern.Error)
                            settingsBioViewModel.vibrate(VibrationPattern.Error)
                        }
                    }
                })
        )
    } else {
        emptyList()
    }
    
    LaunchedEffect(privacy.bio) {
        settingsBioViewModel.init(PrivacyLevel.fromId(privacy.bio))
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.bio))
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                ),
                actions = actions
            )
        }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionHeader("Кто видит мой раздел \"О себе\"?")
            
            SectionContainer {
                SectionRadioItem(
                    text = stringResource(R.string.everybody),
                    selected = currentValue == PrivacyLevel.Everybody,
                    onClick = {
                        settingsBioViewModel.selectValue(PrivacyLevel.Everybody)
                    })
                SectionRadioItem(
                    text = stringResource(R.string.nobody),
                    selected = currentValue == PrivacyLevel.Nobody,
                    onClick = {
                        settingsBioViewModel.selectValue(PrivacyLevel.Nobody)
                    })
            }
        }
    }
}



