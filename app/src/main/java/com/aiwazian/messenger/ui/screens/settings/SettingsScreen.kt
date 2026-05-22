/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
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
import androidx.core.net.toUri
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.UiText
import java.util.Locale

@Composable
fun SettingsScreen() {
    val navBackStack = LocalNavBackStack.current
    
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
                    headlineText = stringResource(R.string.profile),
                    supportingText = stringResource(R.string.write_about_me),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsProfile)
                    })
                
                SectionItem(
                    headlineText = stringResource(R.string.security),
                    supportingText = stringResource(R.string.protect_your_account),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsSecurity)
                    })
            }
            
            
            SectionContainer(
                header = {
                    SectionHeader(stringResource(R.string.settings))
                }) {
                //                SectionItem(
                //                    leadingIcon = Icons.Rounded.NotificationsNone,
                //                    headlineText = stringResource(R.string.notifications),
                //                    onClick = {
                //                        navBackStack.add(AppRoute.SettingsNotifications)
                //                    }
                //                )
                
                SectionItem(
                    leadingIcon = Icons.Rounded.ChatBubbleOutline,
                    headlineText = stringResource(R.string.appearance),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsChat)
                    })
                
                SectionItem(
                    leadingIcon = Icons.Outlined.Lock,
                    headlineText = stringResource(R.string.confidentiality),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsPrivacy)
                    })
                
                SectionItem(
                    leadingIcon = Icons.Rounded.DataUsage,
                    headlineText = stringResource(R.string.data_and_storage),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsDataAndStorage)
                    })
                
                SectionItem(
                    leadingIcon = Icons.Rounded.Language,
                    headlineText = stringResource(R.string.language),
                    onClick = {
                        navBackStack.add(AppRoute.SettingsLanguage)
                    })
            }
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.help))
            }, footer = {
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
                val context = LocalContext.current
                
                SectionItem(
                    leadingIcon = Icons.Outlined.PrivacyTip,
                    headlineText = stringResource(R.string.privacy_policy),
                    onClick = {
                        val intent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setTranslateLocale(Locale.getDefault())
                            .build()
                        intent.launchUrl(context, "https://aiwazian.ru/privacy".toUri())
                    })
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    val actions = listOf(
        TopBarAction(
            icon = Icons.Rounded.MoreVert,
            dropdownActions = listOf(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    text = UiText.StringResource(R.string.logout),
                    onClick = {
                        navBackStack.add(AppRoute.Logout)
                    })
            )
        )
    )
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        ),
        actions = actions
    )
}
