/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.newmessage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun NewMessageScreen() {
    val navBackStack = LocalNavBackStack.current
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.new_message)) },
            )
        },
    ) {
        SectionContainer {
            SectionItem(
                leadingIcon = Icons.Outlined.Group,
                headlineText = stringResource(R.string.create_group),
                onClick = {
                    navBackStack.add(AppRoute.CreateGroup)
                })
            SectionItem(
                leadingIcon = Icons.Outlined.Campaign,
                headlineText = stringResource(R.string.create_channel),
                onClick = {
                    navBackStack.add(AppRoute.CreateChannel)
                })
        }
    }
}
