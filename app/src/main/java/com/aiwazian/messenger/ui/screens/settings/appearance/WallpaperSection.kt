/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.screens.chat.components.MediaPickerBottomSheet
import com.aiwazian.messenger.ui.screens.wallpaper.WallpaperViewModel

@Composable
internal fun WallpaperSection(viewModel: WallpaperViewModel) {
    var showPicker by remember { mutableStateOf(false) }

    val wallpaper by viewModel.currentWallpaper.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    SectionContainer {
        SectionItem(
            headlineText = stringResource(R.string.change_wallpaper),
            onClick = { showPicker = true })

        if (wallpaper != null) {
            SectionItem(
                headlineText = stringResource(R.string.reset_wallpaper),
                contentColor = MaterialTheme.colorScheme.error,
                onClick = viewModel::resetWallpaper
            )
        }
    }

    val target = uiState.target

    if (showPicker && target != null) {
        MediaPickerBottomSheet(
            onDismissRequest = { showPicker = false },
            wallpaperTarget = target
        )
    }
}
