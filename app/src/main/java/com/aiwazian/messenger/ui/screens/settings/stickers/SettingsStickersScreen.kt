/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun SettingsStickersScreen() {
    val navBackStack = LocalNavBackStack.current
    
    AppScaffold(topBar = {
        PageTopBar(title = { Text(stringResource(R.string.stickers)) })
    }) {
        SectionContainer(header = {
            SectionHeader(stringResource(R.string.sticker_packs))
        }) {
            SectionItem(
                headlineText = stringResource(R.string.sticker_packs_created),
                onClick = {
                    navBackStack.add(AppRoute.CreatedStickerPacks)
                })
            
            SectionItem(
                headlineText = stringResource(R.string.sticker_packs_added),
                onClick = {
                    navBackStack.add(AppRoute.AddedStickerPacks)
                })
        }
    }
}
