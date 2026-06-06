/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.utils.AmplitudeExtractor.AMPLITUDES_COUNT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Декодирует аудиофайл в PCM и возвращает фиксированное число амплитуд (RMS по чанкам).
 * Количество всегда [AMPLITUDES_COUNT], значения в диапазоне 0f..1f.
 * Амплитуды нормируются к максимуму для лучшей визуализации.
 */
object AmplitudeExtractor {
    
    private const val TAG = "AmplitudeExtractor"
    const val AMPLITUDES_COUNT = 30
    
    data class AudioAnalysis(
        val amplitudes: List<Float>,
        val durationMs: Int
    )
    
    suspend fun extract(context: Context, uri: Uri): AudioAnalysis = withContext(Dispatchers.IO) {
        runCatching { extractInternal(context, uri) }
            .onFailure { Log.e(TAG, "Failed to extract from $uri", it) }
            .getOrDefault(AudioAnalysis(emptyAmplitudes(), 0))
    }
    
    private fun extractInternal(context: Context, uri: Uri): AudioAnalysis {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            
            val (trackIndex, format) = selectAudioTrack(extractor)
                ?: return AudioAnalysis(emptyAmplitudes(), 0)
            
            val durationUs = runCatching { format.getLong(MediaFormat.KEY_DURATION) }
                .getOrDefault(0L)
            val durationMs = (durationUs / 1000).toInt()
            if (durationMs <= 0) {
                Log.w(TAG, "No duration in MediaFormat for $uri")
                return AudioAnalysis(emptyAmplitudes(), 0)
            }

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: return AudioAnalysis(emptyAmplitudes(), durationMs)

            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val samples = decodeToPcm(extractor, decoder)
            Log.d(TAG, "Decoded ${samples.size} PCM samples, duration=${durationMs}ms from $uri")
            return AudioAnalysis(computeAmplitudes(samples), durationMs)
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
        
        val totalSamples = samples.size
        val baseChunkSize = totalSamples / AMPLITUDES_COUNT
        val remainder = totalSamples % AMPLITUDES_COUNT
        val raw = FloatArray(AMPLITUDES_COUNT)
        
        var currentStart = 0
        for (i in 0 until AMPLITUDES_COUNT) {
            val chunkSize = baseChunkSize + if (i < remainder) 1 else 0
            val end = currentStart + chunkSize

            var sumSquares = 0.0
            for (j in currentStart until end) {
                val sample = samples[j].toDouble()
                sumSquares += sample * sample
            }
            val rms = if (chunkSize > 0) sqrt(sumSquares / chunkSize) / Short.MAX_VALUE else 0.0
            raw[i] = rms.toFloat().coerceIn(0f, 1f)
            
            currentStart = end
        }
        
        val maxAmp = raw.maxOrNull() ?: 0f
        val minAmp = raw.minOrNull() ?: 0f
        Log.d(TAG, "RMS raw range: min=$minAmp max=$maxAmp, sample of values: ${raw.take(5)}")

        return if (maxAmp > 0f) {
            raw.map { it / maxAmp }
        } else {
            emptyAmplitudes()
        }
    }
    
    private fun emptyAmplitudes(): List<Float> = List(AMPLITUDES_COUNT) { 0f }
}
