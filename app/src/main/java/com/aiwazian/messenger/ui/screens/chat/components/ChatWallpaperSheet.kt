/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Wallpaper
import com.aiwazian.messenger.domain.WallpaperSource
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.components.WallpaperImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallpaperSheet(
    wallpaper: Wallpaper?,
    onDismiss: () -> Unit,
    onSelectFromGallery: () -> Unit,
    onReset: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        if (wallpaper != null && wallpaper.source != WallpaperSource.USER) {
            WallpaperImage(
                model = wallpaper.url,
                cacheKey = wallpaper.fileId,
                darkenPercent = wallpaper.darken,
                blurPercent = wallpaper.blur,
                offsetX = wallpaper.offsetX,
                offsetY = wallpaper.offsetY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        }
        
        TextButton(
            onClick = onSelectFromGallery,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.choose_wallpaper_from_gallery))
        }
        
        if (wallpaper != null && wallpaper.source == WallpaperSource.CHAT) {
            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.reset_wallpaper))
            }
        }
    }
}
