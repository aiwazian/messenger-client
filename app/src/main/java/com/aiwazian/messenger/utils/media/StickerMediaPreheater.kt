/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.ui.components.isVideoMediaUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Singleton
class StickerMediaPreheater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val appScope: CoroutineScope
) {

    private val requestedImages = ConcurrentHashMap.newKeySet<String>()

    private val requestedVideos = ConcurrentHashMap.newKeySet<String>()

    private val videoSemaphore = Semaphore(VIDEO_PARALLELISM)

    fun preheatStickerPack(pack: StickerPack) {
        preheat(pack.coverImageUrl, pack.coverCacheKey)
        pack.stickers.forEach { sticker -> preheat(sticker.url, sticker.fileId) }
    }

    fun preheatEmojiPack(pack: EmojiPack) {
        preheat(pack.coverImageUrl, pack.coverCacheKey)
        pack.emojis.forEach { emoji -> preheat(emoji.url, emoji.fileId) }
    }

    fun preheatEmojis(emojis: List<CustomEmoji>) {
        emojis.forEach { emoji -> preheat(emoji.url, emoji.fileId) }
    }

    private fun preheat(url: String?, cacheKey: String?) {
        if (url.isNullOrEmpty() || cacheKey.isNullOrEmpty()) {
            return
        }

        if (isVideoMediaUrl(url)) {
            preheatVideo(url, cacheKey)
        } else {
            preheatImage(url, cacheKey)
        }
    }

    private fun preheatImage(url: String, cacheKey: String) {
        if (!requestedImages.add(cacheKey)) {
            return
        }

        SingletonImageLoader.get(context).enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .diskCacheKey(cacheKey)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .listener(onError = { _, _ -> requestedImages.remove(cacheKey) })
                .build()
        )
    }

    private fun preheatVideo(url: String, cacheKey: String) {
        if (!requestedVideos.add(cacheKey)) {
            return
        }

        appScope.launch {
            videoSemaphore.withPermit {
                val cached = runCatching {
                    StickerVideoCache.get(context).isCached(cacheKey, 0, 1)
                }.getOrDefault(false)

                if (cached) {
                    return@withPermit
                }

                runCatching {
                    val dataSource = StickerVideoCache.dataSourceFactory(context)
                        .createDataSource()

                    CacheWriter(
                        dataSource,
                        DataSpec.Builder()
                            .setUri(url)
                            .setKey(cacheKey)
                            .build(),
                        null,
                        null
                    ).cache()
                }.onFailure {
                    requestedVideos.remove(cacheKey)
                }
            }
        }
    }

    private companion object {
        const val VIDEO_PARALLELISM = 2
    }
}
