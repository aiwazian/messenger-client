/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.aiwazian.messenger.utils.AmplitudeExtractor.AMPLITUDES_COUNT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Декодирует аудиофайл в PCM и возвращает фиксированное число амплитуд (RMS по чанкам).
 * Количество всегда [AMPLITUDES_COUNT], значения в диапазоне 0f..1f.
 * Амплитуды нормируются к максимуму для лучшей визуализации.
 */
object AmplitudeExtractor {
    
    const val AMPLITUDES_COUNT = 30
    
    suspend fun extract(context: Context, uri: Uri): List<Float> = withContext(Dispatchers.IO) {
        runCatching { extractInternal(context, uri) }.getOrDefault(emptyList())
    }
    
    private fun extractInternal(context: Context, uri: Uri): List<Float> {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return emptyAmplitudes()
            
            val (trackIndex, format) = selectAudioTrack(extractor)
                ?: return emptyAmplitudes()
            
            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyAmplitudes()
            
            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            
            val samples = decodeToPcm(extractor, decoder)
            return computeAmplitudes(samples)
        } finally {
            try {
                decoder?.stop()
            } catch (_: Exception) {
            }
            try {
                decoder?.release()
            } catch (_: Exception) {
            }
            extractor.release()
        }
    }
    
    private fun selectAudioTrack(extractor: MediaExtractor): Pair<Int, MediaFormat>? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i to format
            }
        }
        return null
    }
    
    private fun decodeToPcm(extractor: MediaExtractor, decoder: MediaCodec): ShortArray {
        val samples = ArrayList<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        val timeoutUs = 10_000L
        
        while (!sawOutputEos) {
            if (!sawInputEos) {
                val inputIndex = decoder.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        sawInputEos = true
                    } else {
                        decoder.queueInputBuffer(
                            inputIndex, 0, sampleSize, extractor.sampleTime, 0
                        )
                        extractor.advance()
                    }
                }
            }
            
            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val shortBuffer =
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    while (shortBuffer.hasRemaining()) {
                        samples.add(shortBuffer.get())
                    }
                }
                decoder.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEos = true
                }
            }
        }
        
        return samples.toShortArray()
    }
    
    private fun computeAmplitudes(samples: ShortArray): List<Float> {
        if (samples.isEmpty()) return emptyAmplitudes()
        
        val chunkSize = max(1, samples.size / AMPLITUDES_COUNT)
        val raw = FloatArray(AMPLITUDES_COUNT)
        
        for (i in 0 until AMPLITUDES_COUNT) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, samples.size)
            if (start >= samples.size) {
                raw[i] = 0f
                continue
            }
            var sumSquares = 0.0
            var count = 0
            for (j in start until end) {
                val sample = samples[j].toDouble()
                sumSquares += sample * sample
                count++
            }
            val rms = if (count > 0) sqrt(sumSquares / count) / Short.MAX_VALUE else 0.0
            raw[i] = rms.toFloat().coerceIn(0f, 1f)
        }
        
        val maxAmp = raw.maxOrNull() ?: 0f
        return if (maxAmp > 0f) {
            raw.map { it / maxAmp }
        } else {
            raw.toList()
        }
    }
    
    private fun emptyAmplitudes(): List<Float> = List(AMPLITUDES_COUNT) { 0f }
}
