/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import com.aiwazian.messenger.database.dao.EmojiDao
import com.aiwazian.messenger.database.dao.StickerDao
import com.aiwazian.messenger.ui.screens.chat.components.EMOJI_VIDEO_CACHE_DIRECTORY
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StickerCacheStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stickerDao: StickerDao,
    private val emojiDao: EmojiDao
) {

    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val keys = collectCacheKeys()

        imagesCacheSize(keys) + videosCacheSize() + emojiVideosCacheSize()
    }

    suspend fun clear(): Long = withContext(Dispatchers.IO) {
        val keys = collectCacheKeys()

        clearImageEntries(keys) + clearVideoEntries(keys) + clearEmojiVideoFiles()
    }

    private suspend fun collectCacheKeys(): Set<String> {
        return buildSet {
            addAll(stickerDao.getAllFileIds())
            addAll(emojiDao.getAllFileIds())
        }
    }

    private fun imagesCacheSize(keys: Set<String>): Long {
        val diskCache = stickerDiskCache() ?: return 0L

        return keys.sumOf { key -> imageEntrySize(diskCache, key) }
    }

    private fun clearImageEntries(keys: Set<String>): Long {
        val diskCache = stickerDiskCache() ?: return 0L

        var freed = 0L

        keys.forEach { key ->
            val size = imageEntrySize(diskCache, key)

            if (size > 0L && diskCache.remove(key)) {
                freed += size
            }
        }

        return freed
    }

    private fun imageEntrySize(diskCache: DiskCache, key: String): Long {
        val snapshot = diskCache.openSnapshot(key) ?: return 0L

        return snapshot.use {
            diskCache.fileSystem.metadataOrNull(it.data)?.size ?: 0L
        }
    }

    private fun stickerDiskCache(): DiskCache? =
        SingletonImageLoader.get(context).diskCache

    private fun videosCacheSize(): Long =
        StickerVideoCache.get(context).cacheSpace

    private fun clearVideoEntries(keys: Set<String>): Long {
        val cache = StickerVideoCache.get(context)
        val freed = cache.cacheSpace

        keys.forEach { key -> cache.removeResource(key) }

        return freed
    }

    private fun emojiVideoDir(): File = File(context.cacheDir, EMOJI_VIDEO_CACHE_DIRECTORY)

    private fun emojiVideosCacheSize(): Long =
        emojiVideoDir().listFiles()?.sumOf { it.length() } ?: 0L

    private fun clearEmojiVideoFiles(): Long {
        var freed = 0L

        emojiVideoDir().listFiles()?.forEach { file ->
            val size = file.length()

            if (file.delete()) {
                freed += size
            }
        }

        return freed
    }
}
