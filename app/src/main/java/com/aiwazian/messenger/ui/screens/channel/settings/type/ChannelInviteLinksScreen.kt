/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ChannelInviteLinksScreen() {
    val navBackStack = LocalNavBackStack.current
    
    Scaffold(topBar = {
        PageTopBar(
            title = {
                Text(stringResource(R.string.invite_links))
            },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navBackStack::removeLastOrNull
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionItem(
                    headlineText = "Создать ссылку-приглашение",
                    leadingIcon = Icons.Rounded.AddLink,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



