package com.aiwazian.messenger.ui.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun ChannelBlackListScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(text = stringResource(R.string.removed_user)) },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = navViewModel::removeLastScreenInStack
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
        
        }
    }
}