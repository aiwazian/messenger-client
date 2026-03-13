package com.aiwazian.messenger.ui.settings.privacy

import androidx.compose.foundation.layout.Column
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
import com.aiwazian.messenger.ui.element.SectionDescription
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionRadioItem
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun SettingsLastSeenScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.last_seen))
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navViewModel::removeLastScreenInStack
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionHeader("Кто видит время моего последнего захода?")
            
            SectionContainer {
                SectionRadioItem(
                    stringResource(R.string.everybody),
                    selected = true
                )
                SectionRadioItem(
                    stringResource(R.string.nobody),
                    selected = false
                )
            }
            
            SectionDescription("Вместо точного времени будет видно примерное значение (недавно, на этой неделе, в этом месяце)")
        }
    }
}
