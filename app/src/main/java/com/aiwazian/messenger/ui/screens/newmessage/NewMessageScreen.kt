/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.newmessage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Group
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
fun NewMessageScreen() {
    val navBackStack = LocalNavBackStack.current
    
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
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.new_message)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        )
    )
}



