/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.getFolderNameFromMimeType
import com.aiwazian.messenger.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val fileRepository: FileRepository
) {
    private val activeUploads = mutableMapOf<String, okhttp3.Call>()
    
    suspend fun upload(
        fileUri: Uri,
        uploadUrl: String,
        fileId: String,
        maxAttempts: Int = 100
    ): Result<String> = withContext(Dispatchers.IO) {
        repeat(maxAttempts) { attempt ->
            try {
                val fileSize = fileUri.getFileSize(context) ?: 0
                val fileType = fileUri.getFileType(context)
                val contentType = fileType.toMediaTypeOrNull()
                
                val requestBody = ProgressRequestBody(contentType, fileSize, { progress ->
                    // TODO Update progress
                }) {
                    context.contentResolver.openInputStream(fileUri)
                        ?: throw IOException("Unable to open input stream")
                }
                
                val request = Request.Builder().url(uploadUrl).put(requestBody).build()
                val call = okHttpClient.newCall(request)
                activeUploads[fileId] = call
                
                val response = call.execute()
                
                if (response.isSuccessful) {
                    val extension = fileUri.getFileName(context)?.substringAfterLast('.', "")
                    val folderName = fileType.getFolderNameFromMimeType()
                    val path =
                        File(context.getExternalFilesDir(null) ?: context.filesDir, folderName)
                    path.mkdirs()
                    val filePath = saveFileLocally(fileUri, "${path}/${fileId}.${extension}")
                    
                    fileRepository.updateFileStatus(fileId, DownloadStatus.COMPLETED)
                    fileRepository.updateFilePath(fileId, filePath)
                    
                    activeUploads.remove(fileId)
                    return@withContext Result.success(filePath)
                } else {
                    activeUploads.remove(fileId)
                    if (attempt < maxAttempts - 1) {
                        delay(1_000L * (attempt + 1))
                    } else {
                        return@withContext Result.failure(
                            Exception("UploadManager request unsuccessful after $maxAttempts attempts")
                        )
                    }
                }
            } catch (e: Exception) {
                activeUploads.remove(fileId)
                Log.e("UploadManager", "Upload error: ${e.message}", e)
                if (attempt < maxAttempts - 1) {
                    delay(1_000L * (attempt + 1))
                } else {
                    return@withContext Result.failure(e)
                }
            }
        }
        return@withContext Result.failure(Exception("Failed to upload after $maxAttempts attempts"))
    }
    
    fun cancel(fileId: String) {
        activeUploads[fileId]?.cancel()
        activeUploads.remove(fileId)
    }
    
    private fun saveFileLocally(uri: Uri, pathName: String): String {
        val targetFile = File(pathName)
        
        if (!targetFile.exists()) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        return targetFile.absolutePath
    }
}
