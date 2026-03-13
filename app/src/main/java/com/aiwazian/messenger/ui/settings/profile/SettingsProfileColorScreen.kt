package com.aiwazian.messenger.ui.settings.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun SettingsProfileColorScreen() {
    Content()
}

@Composable
private fun Content() {
    Scaffold(topBar = { TopBar() }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
        
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}