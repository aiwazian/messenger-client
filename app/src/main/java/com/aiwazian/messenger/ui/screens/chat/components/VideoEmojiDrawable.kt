/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

private const val FRAME_INTERVAL_MS = 66L
private const val FRAME_STEP_US = 66_000L
private const val RETRY_DELAY_MS = 2_000L
private const val EMOJI_VIDEO_CACHE_DIRECTORY = "emoji_videos"
private const val EMOJI_VIDEO_MAX_AGE_MS = 6L * 60 * 60 * 1000
private const val DOWNLOAD_BUFFER_SIZE = 8 * 1024
private const val TAG = "VideoEmojiDrawable"

class VideoEmojiDrawable(
    context: Context,
    private val url: String,
    private val size: Int,
    view: EditText
) : Drawable() {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var frame: Bitmap? = null

    private var scope: CoroutineScope? = null
    private var started = false

    init {
        setBounds(0, 0, size, size)
        callback = view

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                callback = v

                start(context)
            }

            override fun onViewDetachedFromWindow(v: View) {
                callback = null

                stop()
            }
        })

        if (view.isAttachedToWindow) {
            start(context)
        }
    }

    private fun start(context: Context) {
        if (started) {
            return
        }

        started = true

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope ->
            scope.launch {
                runLoop(context)
            }
        }
    }

    private fun stop() {
        scope?.cancel()
        scope = null
        started = false
    }

    private suspend fun runLoop(context: Context) {
        val retriever = MediaMetadataRetriever()

        try {
            val file: File = run {
                var candidate = emojiVideoFile(context, url)

                while (candidate == null) {
                    delay(RETRY_DELAY_MS)

                    candidate = emojiVideoFile(context, url)
                }

                candidate
            }

            retriever.setDataSource(file.absolutePath)

            val durationUs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.times(1000L) ?: 0L

            var positionUs = 0L

            while (true) {
                val nextFrame = try {
                    retriever.getScaledFrameAtTime(
                        positionUs,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        size,
                        size
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Frame extraction failed for $url", e)
                    null
                }

                if (nextFrame != null) {
                    frame = nextFrame

                    mainHandler.post { invalidateSelf() }
                }

                positionUs += FRAME_STEP_US

                if (durationUs > 0L && positionUs >= durationUs) {
                    positionUs = 0L
                }

                delay(FRAME_INTERVAL_MS)
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            runCatching { retriever.release() }
        }
    }

    override fun draw(canvas: Canvas) {
        val currentFrame = frame ?: return

        canvas.drawBitmap(currentFrame, null, bounds, null)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

private suspend fun emojiVideoFile(context: Context, url: String): File? =
    kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, EMOJI_VIDEO_CACHE_DIRECTORY)

            directory.mkdirs()
            dropStaleEmojiVideos(directory)

            val target = File(directory, "${url.hashCode().toUInt()}.webm")

            if (target.exists() && target.length() > 0L) {
                return@withContext target
            }

            val partial = File(directory, "${target.name}.${System.nanoTime()}.part")

            URL(url).openStream().use { input ->
                partial.outputStream().use { output ->
                    input.copyTo(output, DOWNLOAD_BUFFER_SIZE)
                }
            }

            if (!partial.renameTo(target)) {
                partial.delete()
                return@withContext null
            }

            target
        } catch (e: Exception) {
            Log.w(TAG, "Unable to cache emoji video $url", e)
            null
        }
    }

private fun dropStaleEmojiVideos(directory: File) {
    val deadline = System.currentTimeMillis() - EMOJI_VIDEO_MAX_AGE_MS

    directory.listFiles()?.forEach { file ->
        if (file.lastModified() < deadline) {
            file.delete()
        }
    }
}
