/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aiwazian.messenger.utils.media.StickerVideoCache

private val VIDEO_MEDIA_EXTENSIONS = setOf("webm", "mp4")

private const val TAG = "VideoSticker"

private const val EMULATOR_DECODER_PREFIX = "c2.goldfish"

private val stickerMediaCodecSelector =
    MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
        val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder
        )
        val stable = decoders.filterNot { it.name.startsWith(EMULATOR_DECODER_PREFIX) }

        if (stable.isEmpty()) decoders else stable
    }

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
    modifier: Modifier = Modifier,
    cacheKey: String? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var bypassCache by remember(uri) { mutableStateOf(false) }

    val player = remember(uri, cacheKey, bypassCache) {
        val useCacheKey = if (!bypassCache) cacheKey else null

        val builder = ExoPlayer.Builder(
            context,
            DefaultRenderersFactory(context).setMediaCodecSelector(stickerMediaCodecSelector)
        )

        if (useCacheKey != null) {
            builder.setMediaSourceFactory(
                DefaultMediaSourceFactory(StickerVideoCache.dataSourceFactory(context))
            )
        }

        builder.build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Sticker video playback failed uri=$uri", error)

                    if (useCacheKey != null) {
                        bypassCache = true
                    }
                }
            })
            setMediaItem(
                if (useCacheKey != null) {
                    MediaItem.Builder()
                        .setUri(uri)
                        .setCustomCacheKey(useCacheKey)
                        .build()
                } else {
                    MediaItem.fromUri(uri)
                }
            )
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(uri, cacheKey, bypassCache) {
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
            LoopingVideoPlayer(
                uri = uri,
                modifier = modifier,
                cacheKey = cacheKey
            )
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
