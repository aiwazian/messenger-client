/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun AutoDownloadMediaScreen(viewModel: AutoDownloadMediaViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val scrollState = rememberScrollState()
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                title = {
                    Text(text = "Автозагрузка медиа")
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SectionContainer {
                SectionToggleItem(
                    text = "Загружать автоматически",
                    isChecked = uiState.isAutoDownloadEnabled,
                    onCheckedChange = { viewModel.toggleAutoDownloadMedia(!uiState.isAutoDownloadEnabled) }
                )
            }
            
            AnimatedVisibility(
                visible = uiState.isAutoDownloadEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SectionContainer(footer = {
                    SectionDescription(text = "Голосовые сообщения имеют небольшой размер и всегда загружаются автоматически.")
                }) {
                    SectionToggleItem(
                        text = "Фото",
                        isChecked = uiState.isPhotoEnabled,
                        onCheckedChange = { viewModel.toggleAutoDownloadPhotos(!uiState.isPhotoEnabled) }
                    )
                    SectionToggleItem(
                        text = "Видео",
                        supportingText = "До 10 МБ",
                        isChecked = uiState.isVideoEnabled,
                        onCheckedChange = { viewModel.toggleAutoDownloadVideos(!uiState.isVideoEnabled) }
                    )
                    SectionToggleItem(
                        text = "Файлы",
                        supportingText = "До 10 МБ",
                        isChecked = uiState.isFileEnabled,
                        onCheckedChange = { viewModel.toggleAutoDownloadFiles(!uiState.isFileEnabled) }
                    )
                }
            }
        }
    }
}
