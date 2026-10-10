/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.appearance

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.WallpaperTarget
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.wallpaper.WallpaperViewModel

@Composable
fun ChatAppearanceScreen(
    target: WallpaperTarget,
    viewModel: WallpaperViewModel = hiltViewModel()
) {
    LaunchedEffect(target) {
        viewModel.init(target)
    }

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.appearance))
                }
            )
        }
    ) {
        AppearanceChatPreview()

        WallpaperSection(viewModel)
    }
}
