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
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val onProgress: (Int) -> Unit,
    private val streamProvider: () -> InputStream
) : RequestBody() {

    override fun contentType() = contentType
    
    override fun contentLength(): Long = if (contentLength > 0) contentLength else -1L

    override fun isOneShot(): Boolean = true

    override fun writeTo(sink: BufferedSink) {
        var totalBytesRead = 0L
        var read: Int
        
        val knownLength = contentLength > 0
        streamProvider().source().use { source ->
            while (source.read(sink.buffer, 8192).also { read = it.toInt() } != -1L) {
                totalBytesRead += read
                if (knownLength) {
                    val progress =
                        ((totalBytesRead * 100).coerceAtLeast(0) / contentLength).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }
            }
        }
        if (!knownLength) onProgress(100)
    }
}
