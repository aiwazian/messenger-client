/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DropdownMenuAction
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun SettingsScreen() {
    val navHost = LocalNavHost.current
    
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
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.account))
            }) {
                SectionItem(
                    text = stringResource(R.string.profile),
                    description = stringResource(R.string.write_about_me),
                    onClick = {
                        navHost.add(AppRoute.SettingsProfile)
                    })
                
                SectionItem(
                    text = stringResource(R.string.security),
                    description = stringResource(R.string.protect_your_account),
                    onClick = {
                        navHost.add(AppRoute.SettingsSecurity)
                    })
            }
            
            
            SectionContainer(
                header = {
                    SectionHeader(stringResource(R.string.settings))
                },
                footer = {
                    val context = LocalContext.current
                    
                    val packageInfo = remember {
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        )
                    }
                    
                    val versionName = packageInfo.versionName
                    val versionCode = packageInfo.longVersionCode
                    
                    SectionDescription(text = "${stringResource(R.string.app_name)} v${versionName} (${versionCode})")
                }) {
                SectionItem(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    text = stringResource(R.string.appearance),
                    onClick = {
                        navHost.add(AppRoute.SettingsChat)
                    })
                
                SectionItem(
                    icon = Icons.Outlined.Lock,
                    text = stringResource(R.string.confidentiality),
                    onClick = {
                        navHost.add(AppRoute.SettingsPrivacy)
                    })
                
                SectionItem(
                    icon = Icons.Rounded.DataUsage,
                    text = stringResource(R.string.data_and_storage),
                    onClick = {
                        navHost.add(AppRoute.SettingsDataAndStorage)
                    })
                
                SectionItem(
                    icon = Icons.Rounded.Language,
                    text = stringResource(R.string.language),
                    onClick = {
                        navHost.add(AppRoute.SettingsLanguage)
                    })
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    
    val actions = listOf(
        TopBarAction(
            icon = Icons.Rounded.MoreVert,
            dropdownActions = listOf(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    textResId = R.string.log_out,
                    onClick = {
                        navHost.add(AppRoute.Logout)
                    })
            )
        )
    )
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        ),
        actions = actions
    )
}
