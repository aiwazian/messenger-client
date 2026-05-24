/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
                val mimeType = fileUri.getFileType(context).toMediaTypeOrNull()

                val requestBody = ProgressRequestBody(mimeType, fileSize, { progress ->
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
                    val filePath = saveFileLocally(fileUri, fileId)
                    
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

    suspend fun saveImageToGallery(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)
                ?: MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
                ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val displayName = "IMG_${System.currentTimeMillis()}.$extension"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                resolver.openInputStream(uri)?.use { input ->
                    resolver.openOutputStream(target)?.use { output ->
                        input.copyTo(output)
                    } ?: return@withContext false
                } ?: return@withContext false
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val target = File(picturesDir, displayName)
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext false
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, target.absolutePath)
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
            true
        } catch (e: Exception) {
            Log.e("UploadManager", "saveImageToGallery error: ${e.message}", e)
            false
        }
    }

    private fun saveFileLocally(uri: Uri, fileId: String): String {
        val path = File(context.getExternalFilesDir(null) ?: context.filesDir, "Uploads")
        path.mkdirs()
        val targetFile = File(path, fileId)
        
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
