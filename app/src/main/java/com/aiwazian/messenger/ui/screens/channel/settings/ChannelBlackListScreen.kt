/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost

@Composable
fun ChannelBlackListScreen() {
    val navHost = LocalNavHost.current
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(text = stringResource(R.string.removed_user)) },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navHost::removeLastOrNull
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
        
        }
    }
}



