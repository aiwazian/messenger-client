package com.aiwazian.messenger.ui.settings

import android.app.Activity
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.enums.Language
import com.aiwazian.messenger.services.LanguageService
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionRadioItem
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsLanguageScreen() {
    Content()
}

private data class LanguageItem(
    val text: String,
    val language: Language
)

@Composable
private fun Content() {
    val context = LocalContext.current
    
    val scrollState = rememberScrollState()
    
    val languages = listOf(
        LanguageItem(
            text = stringResource(R.string.russian_untranslatable),
            language = Language.RU
        ),
        LanguageItem(
            text = stringResource(R.string.english_untranslatable),
            language = Language.EN
        ),
    )
    
    Scaffold(
        topBar = {
            TopBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val languageService = LanguageService(context)
            val selectedOption by languageService.languageCode.collectAsState()
            
            val scope = rememberCoroutineScope()
            
            SectionContainer {
                languages.forEach { item ->
                    SectionRadioItem(
                        text = item.text,
                        selected = selectedOption == item.language,
                        onClick = {
                            scope.launch {
                                if (selectedOption != item.language) {
                                    languageService.selLanguage(item.language)
                                    languageService.saveLanguage(item.language)
                                    
                                    (context as? Activity)?.recreate()
                                }
                            }
                        })
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = { Text(stringResource(R.string.language)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}
