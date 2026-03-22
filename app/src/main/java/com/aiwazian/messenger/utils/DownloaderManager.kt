/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
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
import androidx.core.net.toUri

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
        chatId: Long,
        messageId: Int,
        fileId: String,
        size: Long
    ): Int {
        val path = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        val extension = fileName.substringAfterLast('.', "")
        val finalFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId

        val id = ketch.download(
            url = url,
            fileName = finalFileName,
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
                        
                        val finalStatus = mapKetchStatus(model.status)
                        val finalUri = if (finalStatus == DownloadStatus.COMPLETED) {
                            // Assuming model.path is the directory and model.fileName is the file name
                            "${model.path}/${model.fileName}"
                        } else {
                            existing.localUri
                        }
                        
                        current + (id to existing.copy(
                            progress = model.progress,
                            status = finalStatus,
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
    
    fun registerUpload(
        id: Int,
        name: String,
        size: Long
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
    
    fun isDownloaded(fileId: String, extension: String): Boolean {
        val file = getFile(fileId, extension)
        return file.exists() && file.length() > 0
    }

    fun getFile(fileId: String, extension: String): java.io.File {
        val path = context.getExternalFilesDir(null) ?: context.filesDir
        val finalFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
        return java.io.File(path, finalFileName)
    }
    
    fun openFile(path: String) {
        val file = java.io.File(path)
        if (!file.exists()) {
            Log.e("DownloaderManager", "File does not exist at path: $path")
            return
        }

        if (file.extension.equals("apk", ignoreCase = true)) {
            openApkFile(file)
        } else {
            openGenericFile(file)
        }
    }

    private fun openApkFile(file: java.io.File) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("DownloaderManager", "Error opening APK file", e)
        }
    }

    private fun openGenericFile(file: java.io.File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("DownloaderManager", "Error opening generic file", e)
        }
    }
}
