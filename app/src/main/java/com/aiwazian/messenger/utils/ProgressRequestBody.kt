/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.InputStream

class ProgressRequestBody(
    private val inputStream: InputStream,
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val onProgress: (Int) -> Unit
) : RequestBody() {

    override fun contentType() = contentType

    override fun contentLength() = contentLength

    override fun writeTo(sink: BufferedSink) {
        val source = inputStream.source()
        val buffer = ByteArray(8192)
        var totalBytesRead = 0L
        var read: Int

        try {
            while (source.read(sink.buffer, 8192).also { read = it.toInt() } != -1L) {
                totalBytesRead += read
                val progress = ((totalBytesRead * 100) / contentLength).toInt()
                onProgress(progress)
            }
        } finally {
            source.close()
        }
    }
}
