/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.common

import android.net.Uri
import com.aiwazian.messenger.playback.MusicAutoPauseController
import com.aiwazian.messenger.utils.AudioRecorderManager
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class VoiceRecordingController(
    private val scope: CoroutineScope,
    private val recorder: AudioRecorderManager,
    private val vibration: VibrationManager,
    private val musicAutoPauseController: MusicAutoPauseController,
    private val isRecording: () -> Boolean,
    private val updateRecordingState: (isRecording: Boolean, isLocked: Boolean, durationMs: Long, amplitude: Float) -> Unit,
    private val onRecordStartFailed: () -> Unit,
    private val onRecordedFile: (Uri) -> Unit
) {
    private var timerJob: Job? = null
    private var locked = false
    private var durationMs = 0L
    private var amplitude = 0f

    fun start() {
        if (isRecording()) return
        val file = recorder.startRecording()
        if (file == null) {
            scope.launch { onRecordStartFailed() }
            return
        }

        musicAutoPauseController.onRecordingStarted()
        locked = false
        durationMs = 0L
        amplitude = 0f
        emitState()

        timerJob?.cancel()
        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                delay(100.milliseconds)
                amplitude = (recorder.getMaxAmplitude() / 32767f).coerceIn(0f, 1f)
                durationMs = System.currentTimeMillis() - startTime
                emitState()
            }
        }
        vibration.vibrate(VibrationPattern.TactileResponse)
    }

    fun lock() {
        if (!isRecording()) return
        locked = true
        emitState()
        vibration.vibrate(VibrationPattern.TactileResponse)
    }

    fun stopAndDeliver() {
        if (!isRecording()) return
        val file = recorder.stopRecording()
        musicAutoPauseController.onRecordingStopped()
        timerJob?.cancel()
        reset()
        if (file != null) onRecordedFile(Uri.fromFile(file))
    }

    fun cancel() {
        if (!isRecording()) return
        recorder.cancelRecording()
        musicAutoPauseController.onRecordingStopped()
        timerJob?.cancel()
        reset()
        vibration.vibrate(VibrationPattern.TactileResponse)
    }

    private fun reset() {
        locked = false
        durationMs = 0L
        amplitude = 0f
        emitState()
    }

    private fun emitState() {
        updateRecordingState(isRecording(), locked, durationMs, amplitude)
    }
}
