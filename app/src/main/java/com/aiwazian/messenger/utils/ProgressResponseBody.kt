package com.aiwazian.messenger.utils

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

class ProgressResponseBody(
    private val url: String,
    private val responseBody: ResponseBody,
    private val onProgress: (url: String, progress: Int) -> Unit
) : ResponseBody() {
    
    private var bufferedSource: BufferedSource? = null
    
    override fun contentType(): MediaType? = responseBody.contentType()
    
    override fun contentLength(): Long = responseBody.contentLength()
    
    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }
    
    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            var lastProgress = 0
            
            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                if (bytesRead != -1L) {
                    totalBytesRead += bytesRead
                    val fullLength = responseBody.contentLength()
                    if (fullLength > 0) {
                        val progress = ((100 * totalBytesRead) / fullLength).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(url, progress)
                        }
                    }
                }
                return bytesRead
            }
        }
    }
}
