/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionToggle
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun AutoDownloadMediaScreen(viewModel: AutoDownloadMediaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = stringResource(R.string.automatic_media_download))
                }
            )
        },
    ) {
        SectionContainer {
            SectionToggle(
                headlineText = stringResource(R.string.auto_download_media),
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
                SectionDescription(text = stringResource(R.string.auto_download_voice_hint))
            }) {
                SectionToggle(
                    headlineText = stringResource(R.string.photos),
                    isChecked = uiState.isPhotoEnabled,
                    onCheckedChange = { viewModel.toggleAutoDownloadPhotos(!uiState.isPhotoEnabled) }
                )
                SectionToggle(
                    headlineText = stringResource(R.string.videos),
                    supportingText = stringResource(R.string.up_to_10_mb),
                    isChecked = uiState.isVideoEnabled,
                    onCheckedChange = { viewModel.toggleAutoDownloadVideos(!uiState.isVideoEnabled) }
                )
                SectionToggle(
                    headlineText = stringResource(R.string.files),
                    supportingText = stringResource(R.string.up_to_10_mb),
                    isChecked = uiState.isFileEnabled,
                    onCheckedChange = { viewModel.toggleAutoDownloadFiles(!uiState.isFileEnabled) }
                )
            }
        }
    }
}
