/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.di.FileClient
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.getFolderNameFromMimeType
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class UploadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:FileClient private val okHttpClient: OkHttpClient,
    private val fileRepository: FileRepository
) {
    private val activeUploads = mutableMapOf<String, okhttp3.Call>()
    
    /**
     * Загружает файл по форме, подписанной сервером (S3 presigned POST).
     *
     * Раньше здесь был PUT по одной ссылке, и S3 принимал что угодно: и файл
     * произвольного размера, и любой Content-Type. Теперь ограничения зашиты в
     * политику формы, поэтому лишние поля [FileInitResponseDto.fields] обязаны
     * уйти в запрос без изменений и строго до части с файлом.
     */
    suspend fun upload(
        fileUri: Uri,
        upload: FileInitResponseDto,
        fileId: String,
        maxAttempts: Int = 3
    ): Result<String> = withContext(Dispatchers.IO) {
        val fileSize = fileUri.getFileSize(context) ?: 0
        
        // Проверка ради понятной ошибки: иначе пользователь после долгой загрузки
        // получит от S3 голый 403 без объяснений.
        if (upload.maxSizeBytes > 0 && fileSize > upload.maxSizeBytes) {
            return@withContext Result.failure(
                IOException("File size $fileSize exceeds limit of ${upload.maxSizeBytes} bytes")
            )
        }
        
        repeat(maxAttempts) { attempt ->
            try {
                val fileType = fileUri.getFileType(context)
                val contentType = fileType.toMediaTypeOrNull()
                val fileName = fileUri.getFileName(context) ?: "file"
                
                val requestBody = ProgressRequestBody(contentType, fileSize, { progress ->
                    // TODO Update progress
                }) {
                    context.contentResolver.openInputStream(fileUri)
                        ?: throw IOException("Unable to open input stream")
                }
                
                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                
                upload.fields.forEach { (key, value) ->
                    multipartBuilder.addFormDataPart(key, value)
                }
                
                // Файл строго последней частью — так требует политика S3.
                multipartBuilder.addFormDataPart("file", fileName, requestBody)
                
                val request = Request.Builder().url(upload.url).post(multipartBuilder.build())
                    .build()
                val call = okHttpClient.newCall(request)
                activeUploads[fileId] = call
                
                val response = call.execute()
                
                if (response.isSuccessful) {
                    val extension = fileName.substringAfterLast('.', "")
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
                    
                    // 4xx означает, что файл не прошёл политику: повтор ничего не изменит.
                    if (response.code in 400..499) {
                        return@withContext Result.failure(
                            IOException("Upload rejected by storage with code ${response.code}")
                        )
                    }
                    
                    if (attempt < maxAttempts - 1) {
                        delay((1_000 * (attempt + 1)).milliseconds)
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
                    delay((1_000L * (attempt + 1)).milliseconds)
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
