/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import com.aiwazian.messenger.domain.DownloadItem
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.repository.FileRepository
import com.ketch.DownloadConfig
import com.ketch.Ketch
import com.ketch.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloaderManager @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val fileRepository: FileRepository,
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
    
    private val _downloads = mutableListOf<DownloadItem>()
    
    suspend fun download(
        url: String,
        fileName: String,
        fileId: String
    ) {
        val folderName = getFolderNameForExtension(fileName.substringAfterLast('.', ""))
        val path = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        path.mkdirs()
        val extension = fileName.substringAfterLast('.', "")
        val finalFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
        
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
        
        _downloads.add(item)
        
        fileRepository.updateFileStatus(fileId, DownloadStatus.DOWNLOADING)
        
        observeDownload(id, fileId)
    }
    
    private fun observeDownload(downloadId: Int, fileId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            ketch.observeDownloadById(downloadId).collect { model ->
                if (model == null) {
                    return@collect
                }
                
                val existing = _downloads.firstOrNull { it.fileId == fileId } ?: return@collect
                val fileId = existing.fileId
                val finalStatus = model.status.toDomain()
                val finalUri = if (finalStatus == DownloadStatus.COMPLETED) {
                    "${model.path}/${model.fileName}"
                } else {
                    existing.localUri
                }
                
                if (
                    finalStatus == DownloadStatus.COMPLETED &&
                    !finalUri.isNullOrBlank() &&
                    existing.status != DownloadStatus.COMPLETED
                ) {
                    fileRepository.updateFileStatus(fileId, DownloadStatus.COMPLETED)
                    fileRepository.updateFilePath(fileId, finalUri)
                    fileRepository.updateFileSize(fileId, model.total)
                    _downloads.remove(existing)
                } else {
                    _downloads[_downloads.indexOfFirst { it.id == existing.id }] = DownloadItem(
                        id = downloadId,
                        fileId = fileId,
                        name = model.fileName,
                        progress = model.progress,
                        status = finalStatus,
                        size = model.total,
                        speed = model.speedInBytePerMs.toString(),
                        localUri = finalUri
                    )
                }
            }
        }
    }
    
    suspend fun pause(fileId: String) {
        val index = _downloads.indexOfFirst { it.fileId == fileId }
        if (index != -1) {
            ketch.pause(_downloads[index].id)
            _downloads[index] = _downloads[index].copy(status = DownloadStatus.PAUSED)
        }
        fileRepository.updateFileStatus(fileId, DownloadStatus.PAUSED)
    }
    
    suspend fun resume(fileId: String) {
        val index = _downloads.indexOfFirst { it.fileId == fileId }
        if (index != -1) {
            ketch.resume(_downloads[index].id)
            _downloads[index] =
                _downloads[index].copy(status = DownloadStatus.DOWNLOADING)
        }
        fileRepository.updateFileStatus(fileId, DownloadStatus.DOWNLOADING)
    }
    
    suspend fun cancel(fileId: String) {
        val index = _downloads.indexOfFirst { it.fileId == fileId }
        if (index != -1) {
            ketch.cancel(_downloads[index].id)
            _downloads.remove(_downloads[index])
        }
        fileRepository.updateFileStatus(fileId, DownloadStatus.CANCELLED)
    }
    
    fun getFile(fileId: String, extension: String): File {
        val folderName = getFolderNameForExtension(extension)
        val path = File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
        if (!path.exists()) path.mkdirs()
        val finalFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
        return File(path, finalFileName)
    }
    
    private fun getFolderNameForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "Images"
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "Video"
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> "Documents"
            else -> "Other"
        }
    }
    
    private fun Status.toDomain() = when (this) {
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
