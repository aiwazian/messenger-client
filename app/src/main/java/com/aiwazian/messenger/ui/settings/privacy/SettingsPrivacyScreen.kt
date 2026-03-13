package com.aiwazian.messenger.ui.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.ui.element.SectionRadioItem
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.SettingsPrivacyViewModel

@Composable
fun SettingsPrivacyScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    val settingsPrivacyViewModel = hiltViewModel<SettingsPrivacyViewModel>()
    
    val privacy by settingsPrivacyViewModel.privacySettings.collectAsState()
    
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
            SectionHeader(stringResource(R.string.confidentiality))
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.bio),
                    primaryText = if (privacy.bio == PrivacyLevel.Everybody.ordinal) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navViewModel.addScreenInStack {
                            SettingsBioScreen(
                                PrivacyLevel.fromId(privacy.bio),
                                settingsPrivacyViewModel::updateBioValue
                            )
                        }
                    })
                
                SectionItem(
                    text = stringResource(R.string.date_of_birth),
                    primaryText = if (privacy.dateOfBirth == PrivacyLevel.Everybody.ordinal) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navViewModel.addScreenInStack {
                            SettingsDateOfBirthScreen(
                                PrivacyLevel.fromId(privacy.dateOfBirth),
                                settingsPrivacyViewModel::updateDateOfBirthValue
                            )
                        }
                    })
            }
            //
            //            SectionHeader("Удалить мой аккаунт")
            //
            //            SectionContainer {
            //                SectionItem(
            //                    text = "Если я не захожу",
            //                    primaryText = "12 месяцев",
            //                    onClick = settingsPrivacyViewModel.deleteAccountDialog::show
            //                )
            //            }
            //
            //            SectionDescription("Если Вы ни разу не загляните в ${stringResource(R.string.app_name)} за это время, аккаунт будет удален.")
            //
            if (settingsPrivacyViewModel.deleteAccountDialog.isVisible) {
                DeleteAccountIfINotLoginDialog(
                    onDismissRequest = settingsPrivacyViewModel.deleteAccountDialog::hide,
                    onConfirm = {})
            }
        }
    }
}

@Composable
private fun DeleteAccountIfINotLoginDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    var selectedOption by remember { mutableStateOf("Выберите пункт") }
    
    val options = listOf(
        "1 месяц",
        "3 месяца",
        "6 месяцев",
        "12 месяцев"
    )
    
    CustomDialog(
        title = "Удаление аккаунта при неактивности",
        onDismissRequest = onDismissRequest,
        content = {
            options.forEach { option ->
                Card(shape = RoundedCornerShape(10.dp)) {
                    SectionRadioItem(
                        text = option,
                        selected = (selectedOption == option),
                        onClick = {
                            selectedOption = option
                        })
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = { Text(stringResource(R.string.confidentiality)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}
