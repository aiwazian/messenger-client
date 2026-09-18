/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.media.MediaMetadataRetriever
import com.aiwazian.messenger.domain.AudioTrackMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMetadataCache @Inject constructor() {

    private val cache = ConcurrentHashMap<String, AudioTrackMetadata>()

    suspend fun get(fileId: String, filePath: String, fallbackTitle: String): AudioTrackMetadata? {
        cache[fileId]?.let { return it }

        val extracted = withContext(Dispatchers.IO) {
            runCatching { extract(filePath, fallbackTitle) }.getOrNull()
        } ?: return null

        cache[fileId] = extracted
        return extracted
    }

    private fun extract(filePath: String, fallbackTitle: String): AudioTrackMetadata {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(filePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: fallbackTitle

            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?.takeIf { it.isNotBlank() }

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.toInt() ?: 0

            return AudioTrackMetadata(
                title = title,
                artist = artist,
                durationMs = durationMs,
                cover = retriever.embeddedPicture
            )
        }
    }
}
