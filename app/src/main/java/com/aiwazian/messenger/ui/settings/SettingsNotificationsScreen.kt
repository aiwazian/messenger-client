package com.aiwazian.messenger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionToggleItem
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun SettingsNotificationsScreen() {
    Content()
}

@Composable
private fun Content() {
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
            SectionHeader("Уведомления из чатов")
            
            SectionContainer {
                SectionToggleItem(
                    text = "Личные чаты",
                    isChecked = false,
                    onCheckedChange = {})
                
                SectionToggleItem(
                    text = "Группы",
                    isChecked = false,
                    onCheckedChange = {})
                
                SectionToggleItem(
                    text = "Каналы",
                    isChecked = false,
                    onCheckedChange = {})
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = {
            Text(stringResource(R.string.notifications))
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}
