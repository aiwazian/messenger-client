/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrivacyScreen(privacyViewModel: SettingsPrivacyViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    
    val privacy by privacyViewModel.privacySettings.collectAsState()
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        privacyViewModel.loadValues()
    }
    
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
            
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()
            var showBottomSheet by remember { mutableStateOf(false) }
            
            SectionContainer(
                header = {
                    SectionHeader(title = "Delete my account")
                },
                footer = {
                    SectionDescription(text = "Если вы не зайдете...")
                }) {
                SectionItem(text = "If away for")
            }
            
            SectionContainer {
                SectionItem(
                    text = "Удалить аккаунт прямо сейчас",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showBottomSheet = true
                    })
            }
            
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState,
                    dragHandle = null
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Все чаты, каналы, группы, сообщения и все,что связанно с вашим аккаунтом будут безвозвратно удалено, это действие нельзя будет отменить")
                        TextButton(
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion { showBottomSheet = false }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Все равно удалить")
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
        title = { Text(stringResource(R.string.confidentiality)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}
