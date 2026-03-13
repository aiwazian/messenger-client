package com.aiwazian.messenger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.DropdownMenuAction
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.enums.Language
import com.aiwazian.messenger.services.LanguageService
import com.aiwazian.messenger.ui.LogoutScreen
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionDescription
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.ui.settings.chat.SettingsChatScreen
import com.aiwazian.messenger.ui.settings.chatFolder.SettingsChatFoldersScreen
import com.aiwazian.messenger.ui.settings.privacy.SettingsPrivacyScreen
import com.aiwazian.messenger.ui.settings.profile.SettingsProfileScreen
import com.aiwazian.messenger.ui.settings.security.SettingsSecurityScreen
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun SettingsScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val languageService = LanguageService(LocalContext.current)
    
    val selectedLanguage = when (languageService.languageCode.collectAsState().value) {
        Language.RU -> stringResource(R.string.russian_untranslatable)
        Language.EN -> stringResource(R.string.english_untranslatable)
    }
    
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
            SectionHeader(stringResource(R.string.account))
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.profile),
                    description = stringResource(R.string.write_about_me),
                    onClick = {
                        navViewModel.addScreenInStack { SettingsProfileScreen() }
                    })
                
                SectionItem(
                    text = stringResource(R.string.security),
                    description = stringResource(R.string.protect_your_account),
                    onClick = {
                        navViewModel.addScreenInStack { SettingsSecurityScreen() }
                    })
            }
            
            SectionHeader(stringResource(R.string.settings))
            
            SectionContainer {
                SectionItem(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = stringResource(R.string.appearance),
                    onClick = {
                        navViewModel.addScreenInStack { SettingsChatScreen() }
                    })
                
                SectionItem(
                    icon = Icons.Outlined.Lock,
                    text = stringResource(R.string.confidentiality),
                    onClick = {
                        navViewModel.addScreenInStack { SettingsPrivacyScreen() }
                    })
                
                SectionItem(
                    icon = Icons.Outlined.DataUsage,
                    text = stringResource(R.string.data_and_storage),
                    onClick = {
                        navViewModel.addScreenInStack { SettingsDataAndStorageScreen() }
                    })
                
                SectionItem(
                    icon = Icons.Outlined.Folder,
                    text = stringResource(R.string.chat_folders),
                    onClick = {
                        navViewModel.addScreenInStack { SettingsChatFoldersScreen() }
                    })
                
                SectionItem(
                    icon = Icons.Outlined.Language,
                    text = stringResource(R.string.language),
                    primaryText = selectedLanguage,
                    onClick = {
                        navViewModel.addScreenInStack { SettingsLanguageScreen() }
                    })
            }
            
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
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val actions = listOf(
        TopBarAction(
            icon = Icons.Outlined.MoreVert,
            dropdownActions = listOf(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    text = stringResource(R.string.log_out),
                    onClick = {
                        navViewModel.addScreenInStack {
                            LogoutScreen()
                        }
                    })
            )
        )
    )
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        ),
        actions = actions
    )
}
