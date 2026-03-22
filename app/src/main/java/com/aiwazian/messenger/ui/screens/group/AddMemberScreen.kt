/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.group.settings.GroupViewModel

@Composable
fun AddMemberScreen(groupViewModel: GroupViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current

    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navHost::removeLastOrNull
            ),
            actions = listOf(
                TopBarAction(
                    icon = Icons.Rounded.Check,
                    onClick = {
                    
                    }
                )
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

        }
    }
}



