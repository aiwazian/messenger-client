/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import com.aiwazian.messenger.domain.DownloadItem
import com.aiwazian.messenger.enums.DownloadStatus
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
import java.io.File
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
                connectTimeOutInMs = 30000,
                readTimeOutInMs = 30000
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
        fileId: String
    ): Int {
        val folderName = getFolderNameForExtension(fileName.substringAfterLast('.', ""))
        val path = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        path.mkdirs()
        val extension = fileName.substringAfterLast('.', "")
        val finalFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
        
        _downloads.value.values.find { it.fileId == fileId }?.let { existing ->
            ketch.cancel(existing.id)
            _downloads.update { it - existing.id }
        }
        
        val id = ketch.download(
            url = url,
            fileName = finalFileName,
            path = path.absolutePath,
        )
        
        val item = DownloadItem(
            id = id,
            fileId = fileId,
            name = fileName,
            size = 0,
            progress = 0,
            status = DownloadStatus.DOWNLOADING
        )
        
        _downloads.update { it + (id to item) }
        observeDownload(id)
        return id
    }
    
    private fun getFolderNameForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "Images"
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "Video"
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> "Documents"
            else -> "Other"
        }
    }
    
    private fun observeDownload(id: Int) {
        scope.launch {
            ketch.observeDownloadById(id).collect { model ->
                if (model != null) {
                    _downloads.update { current ->
                        val existing = current[id] ?: return@update current
                        
                        val finalStatus = mapKetchStatus(model.status)
                        val finalUri = if (finalStatus == DownloadStatus.COMPLETED) {
                            "${model.path}/${model.fileName}"
                        } else {
                            existing.localUri
                        }
                        
                        current + (id to existing.copy(
                            progress = model.progress,
                            status = finalStatus,
                            size = model.total,
                            speed = model.speedInBytePerMs.toString(),
                            localUri = finalUri
                        ))
                    }
                }
            }
        }
    }
    
    private fun mapKetchStatus(status: Status): DownloadStatus {
        return when (status) {
            Status.DEFAULT -> DownloadStatus.IDLE
            Status.QUEUED -> DownloadStatus.DOWNLOADING
            Status.PROGRESS -> DownloadStatus.DOWNLOADING
            Status.PAUSED -> DownloadStatus.PAUSED
            Status.SUCCESS -> DownloadStatus.COMPLETED
            Status.FAILED -> DownloadStatus.FAILED
            Status.CANCELLED -> DownloadStatus.CANCELLED
            Status.STARTED -> DownloadStatus.DOWNLOADING
        }
    }
    
    fun pause(id: Int) = ketch.pause(id)
    fun resume(id: Int) = ketch.resume(id)
    fun cancel(id: Int) {
        ketch.cancel(id)
        _downloads.update { it - id }
    }
    
    fun getFile(fileId: String, extension: String): File {
        val folderName = getFolderNameForExtension(extension)
        val path = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!path.exists()) path.mkdirs()
        val finalFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
        return File(path, finalFileName)
    }
}
