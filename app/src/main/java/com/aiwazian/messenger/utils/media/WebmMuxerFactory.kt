/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.Muxer
import androidx.media3.muxer.SeekableMuxerOutput
import androidx.media3.muxer.WebmMuxer
import com.google.common.collect.ImmutableList
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import androidx.media3.muxer.MuxerException

@OptIn(UnstableApi::class)
class WebmMuxerFactory : Muxer.Factory {

    override fun create(path: String): Muxer {
        val stream = try {
            FileOutputStream(File(path))
        } catch (e: FileNotFoundException) {
            throw MuxerException("Error creating file output stream", e)
        }

        val muxer = WebmMuxer.Builder(SeekableMuxerOutput.of(stream)).build()

        return WebmExportMuxer(muxer)
    }

    override fun getSupportedSampleMimeTypes(trackType: Int): ImmutableList<String> {
        return when (trackType) {
            C.TRACK_TYPE_VIDEO -> ImmutableList.of(
                MimeTypes.VIDEO_VP8,
                MimeTypes.VIDEO_VP9
            )

            C.TRACK_TYPE_AUDIO -> ImmutableList.of(
                MimeTypes.AUDIO_OPUS,
                MimeTypes.AUDIO_VORBIS
            )

            else -> ImmutableList.of()
        }
    }

    private class WebmExportMuxer(
        private val delegate: WebmMuxer
    ) : Muxer by delegate {

        override fun addTrack(format: Format): Int = delegate.addTrack(
            format.buildUpon().setLanguage(format.language ?: UNDETERMINED_LANGUAGE).build()
        )

        override fun addMetadataEntry(metadataEntry: Metadata.Entry) = Unit
    }

    private companion object {
        const val UNDETERMINED_LANGUAGE = "und"
    }
}
