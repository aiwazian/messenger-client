/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import com.aiwazian.messenger.domain.DownloadItem
import com.aiwazian.messenger.domain.DownloadStatus
import com.ketch.DownloadConfig
import com.ketch.Ketch
import com.ketch.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloaderManager @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    okHttpClient: OkHttpClient
) {
    private val ketch = Ketch.builder()
        .setOkHttpClient(okHttpClient)
        .setDownloadConfig(
            DownloadConfig(
                connectTimeOutInMs = 15000,
                readTimeOutInMs = 15000
            )
        )
        .build(context)
    
    private val _downloads = MutableStateFlow<Map<Int, DownloadItem>>(emptyMap())
    val downloads: StateFlow<List<DownloadItem>> = _downloads
        .map { it.values.toList() }
        .stateIn(
            CoroutineScope(Dispatchers.Default),
            SharingStarted.Eagerly,
            emptyList()
        )
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun download(
        url: String,
        fileName: String,
        chatId: Long,
        messageId: Int,
        fileId: String,
        size: Long
    ): Int {
        val path = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        val id = ketch.download(
            url = url,
            fileName = fileName,
            path = path,
            tag = "chat_$chatId"
        )
        
        val item = DownloadItem(
            id = id,
            messageId = messageId,
            fileId = fileId,
            name = fileName,
            size = size,
            progress = 0,
            status = DownloadStatus.DOWNLOADING
        )
        
        _downloads.update { it + (id to item) }
        observeDownload(id)
        return id
    }
    
    private fun observeDownload(id: Int) {
        scope.launch {
            ketch.observeDownloadById(id).collect { model ->
                if (model != null) {
                    _downloads.update { current ->
                        val existing = current[id] ?: return@update current
                        current + (id to existing.copy(
                            progress = model.progress,
                            status = mapKetchStatus(model.status),
                            speed = model.speedInBytePerMs.toString()
                        ))
                    }
                    
                    if (model.status == Status.SUCCESS || model.status == Status.FAILED || model.status == Status.CANCELLED) {
                        // Optional: remove from active list after some time or keep it
                    }
                }
            }
        }
    }
    
    private fun mapKetchStatus(status: Status): DownloadStatus {
        return when (status) {
            Status.DEFAULT -> DownloadStatus.IDLE
            Status.QUEUED -> DownloadStatus.IDLE
            Status.PROGRESS -> DownloadStatus.DOWNLOADING
            Status.PAUSED -> DownloadStatus.PAUSED
            Status.SUCCESS -> DownloadStatus.COMPLETED
            Status.FAILED -> DownloadStatus.FAILED
            Status.CANCELLED -> DownloadStatus.CANCELLED
            else -> DownloadStatus.IDLE
        }
    }
    
    fun pause(id: Int) = ketch.pause(id)
    fun resume(id: Int) = ketch.resume(id)
    fun cancel(id: Int) {
        ketch.cancel(id)
        _downloads.update { it - id }
    }
    
    // For Uploads (since Ketch is primarily for downloads, we manage uploads manually but keep them in the same list)
    fun registerUpload(
        id: Int,
        name: String,
        size: Long,
        chatId: Long
    ) {
        val item = DownloadItem(
            id = id,
            name = name,
            size = size,
            progress = 0,
            status = DownloadStatus.UPLOADING,
            isUpload = true
        )
        _downloads.update { it + (id to item) }
    }
    
    fun updateUploadProgress(
        id: Int,
        progress: Int
    ) {
        _downloads.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.copy(progress = progress))
        }
    }
    
    fun completeUpload(id: Int) {
        _downloads.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100
            ))
        }
    }
    
    fun failUpload(id: Int) {
        _downloads.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.copy(status = DownloadStatus.FAILED))
        }
    }
}
