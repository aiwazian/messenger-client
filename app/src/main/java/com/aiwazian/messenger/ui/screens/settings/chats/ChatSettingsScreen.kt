/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.chats

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionToggle
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ChatSettingsScreen(viewModel: ChatSettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = stringResource(R.string.chat_settings))
                }
            )
        },
    ) {
        SectionContainer {
            SectionToggle(
                headlineText = stringResource(R.string.pause_music_on_record),
                supportingText = stringResource(R.string.pause_music_on_record_description),
                isChecked = uiState.pauseMusicOnRecord,
                onCheckedChange = {
                    viewModel.togglePauseMusicOnRecord(!uiState.pauseMusicOnRecord)
                }
            )

            SectionToggle(
                headlineText = stringResource(R.string.pause_music_on_media),
                supportingText = stringResource(R.string.pause_music_on_media_description),
                isChecked = uiState.pauseMusicOnMedia,
                onCheckedChange = {
                    viewModel.togglePauseMusicOnMedia(!uiState.pauseMusicOnMedia)
                }
            )
        }
    }
}
