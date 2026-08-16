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
import kotlin.time.Duration

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
     *
     * @param maxAttempts сколько раз пробовать. Загрузки, чей прогресс ждёт
     * пользователь на экране (аватарки), обязаны когда-то завершиться, поэтому
     * по умолчанию их три. Вложения сообщений передают [UNLIMITED_ATTEMPTS]:
     * обрыв сети или 5xx только откладывает следующую попытку, а задержка
     * растёт до [RetryPolicy.MAX_DELAY].
     *
     * Неудачей сразу заканчиваются только отказы, которые повтор не изменит:
     * превышение лимита размера и 4xx от хранилища.
     */
    suspend fun upload(
        fileUri: Uri,
        upload: FileInitResponseDto,
        fileId: String,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS
    ): Result<String> = withContext(Dispatchers.IO) {
        val fileSize = fileUri.getFileSize(context) ?: 0
        
        // Проверка ради понятной ошибки: иначе после долгой загрузки придёт
        // голый 403 от S3 без объяснений.
        if (upload.maxSizeBytes > 0 && fileSize > upload.maxSizeBytes) {
            return@withContext Result.failure(
                IOException("File size $fileSize exceeds limit of ${upload.maxSizeBytes} bytes")
            )
        }
        
        var attempt = 1
        var nextDelay = RetryPolicy.INITIAL_DELAY
        var lastError: IOException? = null
        
        while (attempt <= maxAttempts) {
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
                }
                
                activeUploads.remove(fileId)
                
                // 4xx — файл не прошёл политику формы: повтор ничего не изменит.
                if (response.code in 400..499) {
                    return@withContext Result.failure(
                        IOException("Upload rejected by storage with code ${response.code}")
                    )
                }
                
                lastError = IOException("Upload failed with code ${response.code}")
                Log.w("UploadManager", "Upload attempt $attempt failed: ${response.code}")
            } catch (e: IOException) {
                activeUploads.remove(fileId)
                lastError = e
                Log.e("UploadManager", "Upload attempt $attempt error: ${e.message}", e)
            }
            
            if (attempt == maxAttempts) break
            
            delay(nextDelay)
            nextDelay = increase(nextDelay)
            attempt++
        }
        
        Result.failure(lastError ?: IOException("Upload failed"))
    }
    
    fun cancel(fileId: String) {
        activeUploads[fileId]?.cancel()
        activeUploads.remove(fileId)
    }
    
    private fun increase(current: Duration): Duration {
        val increased = current * 2.0
        return if (increased > RetryPolicy.MAX_DELAY) RetryPolicy.MAX_DELAY else increased
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
    
    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        
        /** Повторять, пока файл не уйдёт либо его не отклонят окончательно. */
        const val UNLIMITED_ATTEMPTS = Int.MAX_VALUE
    }
}
