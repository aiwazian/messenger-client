/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.utils.AmplitudeExtractor.AMPLITUDES_COUNT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Decodes an audio file into a fixed number of amplitudes (RMS per bucket),
 * normalized to the maximum. Values are in the 0f..1f range and the result
 * always contains [AMPLITUDES_COUNT] entries.
 *
 * The PCM stream is never accumulated in memory: decoding only keeps a
 * per-chunk sum of squares, so memory usage does not grow with recording length.
 */
object AmplitudeExtractor {

    private const val TAG = "AmplitudeExtractor"
    const val AMPLITUDES_COUNT = 40

    private const val CHUNK_SAMPLES = 1024

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

            val accumulator = AmplitudeAccumulator()
            decode(extractor, decoder, accumulator)
            return AudioAnalysis(accumulator.computeAmplitudes(), durationMs)
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

    private fun decode(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        accumulator: AmplitudeAccumulator
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
        var isEncodingResolved = false
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
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    pcmEncoding = runCatching {
                        decoder.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }.getOrDefault(pcmEncoding)
                    isEncodingResolved = true
                }

                outputIndex >= 0 -> {
                    if (!isEncodingResolved) {
                        isEncodingResolved = true
                        pcmEncoding = runCatching {
                            decoder.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }.getOrDefault(pcmEncoding)
                    }
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            accumulator.add(outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer())
                        } else {
                            accumulator.add(outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer())
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
        }
    }

    private class AmplitudeAccumulator {
        private val chunkSumSquares = ArrayList<Double>()
        private val scratch = ShortArray(CHUNK_SAMPLES)
        private val floatScratch = FloatArray(CHUNK_SAMPLES)
        private var currentSumSquares = 0.0
        private var currentCount = 0
        private var totalSamples = 0L

        fun add(pcm: ShortBuffer) {
            while (pcm.hasRemaining()) {
                val count = min(pcm.remaining(), CHUNK_SAMPLES)
                pcm.get(scratch, 0, count)
                accumulate(count)
            }
        }

        fun add(pcm: FloatBuffer) {
            while (pcm.hasRemaining()) {
                val count = min(pcm.remaining(), CHUNK_SAMPLES)
                pcm.get(floatScratch, 0, count)
                for (i in 0 until count) {
                    scratch[i] = (floatScratch[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                accumulate(count)
            }
        }

        private fun accumulate(count: Int) {
            var index = 0
            while (index < count) {
                val take = min(count - index, CHUNK_SAMPLES - currentCount)
                for (i in index until index + take) {
                    val sample = scratch[i].toDouble()
                    currentSumSquares += sample * sample
                }
                currentCount += take
                totalSamples += take
                index += take
                if (currentCount == CHUNK_SAMPLES) {
                    flushChunk()
                }
            }
        }

        private fun flushChunk() {
            if (currentCount > 0) {
                chunkSumSquares.add(currentSumSquares)
                currentSumSquares = 0.0
                currentCount = 0
            }
        }

        fun computeAmplitudes(): List<Float> {
            flushChunk()
            if (totalSamples == 0L || chunkSumSquares.isEmpty()) return emptyAmplitudes()

            val sums = DoubleArray(AMPLITUDES_COUNT)
            val counts = LongArray(AMPLITUDES_COUNT)

            var position = 0L
            chunkSumSquares.forEachIndexed { chunkIndex, chunkSum ->
                val chunkSize = if (chunkIndex == chunkSumSquares.lastIndex) {
                    (totalSamples - chunkIndex * CHUNK_SAMPLES).toInt()
                } else {
                    CHUNK_SAMPLES
                }

                var offset = 0
                while (offset < chunkSize) {
                    val bucket = ((position * AMPLITUDES_COUNT) / totalSamples)
                        .toInt()
                        .coerceIn(0, AMPLITUDES_COUNT - 1)
                    val bucketEnd =
                        ((bucket + 1) * totalSamples + AMPLITUDES_COUNT - 1) / AMPLITUDES_COUNT
                    val take = minOf(
                        chunkSize - offset,
                        (bucketEnd - position).toInt().coerceAtLeast(1)
                    )

                    sums[bucket] += chunkSum * take / chunkSize
                    counts[bucket] += take
                    position += take
                    offset += take
                }
            }

            val raw = FloatArray(AMPLITUDES_COUNT) { i ->
                if (counts[i] > 0) {
                    (sqrt(sums[i] / counts[i]) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
            }

            val maxAmp = raw.maxOrNull() ?: 0f
            return if (maxAmp > 0f) raw.map { it / maxAmp } else emptyAmplitudes()
        }
    }

    private fun emptyAmplitudes(): List<Float> = List(AMPLITUDES_COUNT) { 0f }
}
