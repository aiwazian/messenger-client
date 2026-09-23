/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

private val VIDEO_MEDIA_EXTENSIONS = setOf("webm", "mp4")

fun isVideoMediaUrl(url: String?): Boolean {
    if (url == null) {
        return false
    }
    
    val extension = url.substringBefore('?').substringAfterLast('.', "").lowercase()
    
    return extension in VIDEO_MEDIA_EXTENSIONS
}

@Composable
fun LoopingVideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    
    DisposableEffect(uri) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> Unit
            }
        }
        
        lifecycle.addObserver(observer)
        
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    
    ContentFrame(
        player = player,
        modifier = modifier,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        contentScale = ContentScale.Fit,
        shutter = {}
    )
}

@Composable
fun AnimatedStickerImage(
    data: Any?,
    isVideo: Boolean,
    modifier: Modifier = Modifier,
    cacheKey: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    if (isVideo) {
        val uri = when (data) {
            is String -> data.toUri()
            is Uri -> data
            else -> null
        }
        
        if (uri != null) {
            LoopingVideoPlayer(uri = uri, modifier = modifier)
        }
        
        return
    }
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(data)
            .apply {
                if (cacheKey != null) {
                    memoryCacheKey(cacheKey)
                    diskCacheKey(cacheKey)
                }
            }
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
