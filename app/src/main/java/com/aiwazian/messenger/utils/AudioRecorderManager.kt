/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    
    fun startRecording(): File? {
        val outputDir = context.cacheDir
        val outputFile = File.createTempFile("audio_record_", ".ogg", outputDir)
        currentFile = outputFile
        
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.OGG)
            setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
        return outputFile
    }
    
    fun stopRecording(): File? {
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Handle cases where stop() is called immediately after start()
            e.printStackTrace()
            currentFile?.delete()
            currentFile = null
        } finally {
            recorder?.release()
            recorder = null
        }
        return currentFile
    }
    
    fun cancelRecording() {
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            Log.e("AudioRecorderManager", "${e.message} ", e)
        } finally {
            recorder?.release()
            recorder = null
            currentFile?.delete()
            currentFile = null
        }
    }
    
    fun getMaxAmplitude(): Int {
        return recorder?.maxAmplitude ?: 0
    }
}
