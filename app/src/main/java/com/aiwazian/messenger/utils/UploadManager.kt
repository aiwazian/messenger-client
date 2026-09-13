/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.di.FileClient
import com.aiwazian.messenger.domain.AttachmentUploadException
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
import java.io.FileNotFoundException
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

    suspend fun upload(
        fileUri: Uri,
        upload: FileInitResponseDto,
        fileId: String,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        keepLocalCopy: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val fileSize = fileUri.getFileSize(context) ?: 0
        
        if (upload.maxSizeBytes in 1..<fileSize) {
            return@withContext Result.failure(
                AttachmentUploadException.TooLarge(fileSize, upload.maxSizeBytes)
            )
        }
        
        var attempt = 1
        var nextDelay = RetryPolicy.INITIAL_DELAY
        var lastError: IOException? = null
        
        while (attempt <= maxAttempts) {
            val missingSource = findMissingSource(fileUri)
            
            if (missingSource != null) {
                activeUploads.remove(fileId)
                return@withContext Result.failure(missingSource)
            }
            
            try {
                val fileType = fileUri.getFileType(context)
                val contentType = fileType.toMediaTypeOrNull()
                val fileName = fileUri.getFileName(context) ?: DEFAULT_FILE_NAME
                
                val requestBody = ProgressRequestBody(contentType, fileSize, {}) {
                    context.contentResolver.openInputStream(fileUri)
                        ?: throw IOException("Unable to open input stream")
                }
                
                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                
                upload.fields.forEach { (key, value) ->
                    multipartBuilder.addFormDataPart(key, value)
                }
                
                multipartBuilder.addFormDataPart("file", fileName, requestBody)
                
                val request = Request.Builder().url(upload.url).post(multipartBuilder.build())
                    .build()
                val call = okHttpClient.newCall(request)
                activeUploads[fileId] = call
                
                val response = call.execute()
                
                if (response.isSuccessful) {
                    var localPath: String? = null
                    
                    if (keepLocalCopy) {
                        localPath = keepLocally(
                            fileUri = fileUri,
                            fileId = fileId,
                            fileName = fileName,
                            mimeType = fileType,
                            allowMove = false
                        )
                        
                        if (localPath == null) {
                            fileRepository.updateFileStatus(fileId, DownloadStatus.COMPLETED)
                        } else {
                            fileRepository.updateFilePathAndStatus(
                                fileId,
                                localPath,
                                DownloadStatus.COMPLETED
                            )
                        }
                    }
                    
                    activeUploads.remove(fileId)
                    return@withContext Result.success(localPath ?: fileUri.toString())
                }
                
                activeUploads.remove(fileId)
                
                if (response.code in 400..499) {
                    return@withContext Result.failure(
                        IOException("Upload rejected by storage with code ${response.code}")
                    )
                }
                
                lastError = IOException("Upload failed with code ${response.code}")
                Log.w(TAG, "Upload attempt $attempt failed: ${response.code}")
            } catch (e: FileNotFoundException) {
                activeUploads.remove(fileId)
                return@withContext Result.failure(
                    AttachmentUploadException.SourceMissing(fileUri.toString(), e)
                )
            } catch (e: SecurityException) {
                activeUploads.remove(fileId)
                return@withContext Result.failure(
                    AttachmentUploadException.SourceMissing(fileUri.toString(), e)
                )
            } catch (e: IOException) {
                activeUploads.remove(fileId)
                lastError = e
                Log.e(TAG, "Upload attempt $attempt error: ${e.message}", e)
            }
            
            if (attempt == maxAttempts) break
            
            delay(nextDelay)
            nextDelay = increase(nextDelay)
            attempt++
        }
        
        Result.failure(lastError ?: IOException("Upload failed"))
    }
    
    suspend fun adoptLocalCopy(
        fileUri: Uri,
        fileId: String,
        status: DownloadStatus
    ): String? = withContext(Dispatchers.IO) {
        val fileName = fileUri.getFileName(context) ?: DEFAULT_FILE_NAME
        val mimeType = fileUri.getFileType(context)
        
        val localPath = keepLocally(
            fileUri = fileUri,
            fileId = fileId,
            fileName = fileName,
            mimeType = mimeType,
            allowMove = true
        )
        
        if (localPath == null) {
            fileRepository.updateFileStatus(fileId, status)
        } else {
            fileRepository.updateFilePathAndStatus(fileId, localPath, status)
        }
        
        localPath
    }
    
    fun cancel(fileId: String) {
        activeUploads[fileId]?.cancel()
        activeUploads.remove(fileId)
    }
    
    private fun findMissingSource(fileUri: Uri): AttachmentUploadException.SourceMissing? {
        return try {
            val stream = context.contentResolver.openInputStream(fileUri)
            
            if (stream == null) {
                AttachmentUploadException.SourceMissing(fileUri.toString())
            } else {
                stream.close()
                null
            }
        } catch (e: FileNotFoundException) {
            AttachmentUploadException.SourceMissing(fileUri.toString(), e)
        } catch (e: SecurityException) {
            AttachmentUploadException.SourceMissing(fileUri.toString(), e)
        } catch (e: IOException) {
            Log.w(TAG, "Unable to probe $fileUri: ${e.message}", e)
            null
        }
    }
    
    private fun increase(current: Duration): Duration {
        val increased = current * 2.0
        return if (increased > RetryPolicy.MAX_DELAY) RetryPolicy.MAX_DELAY else increased
    }
    
    private fun keepLocally(
        fileUri: Uri,
        fileId: String,
        fileName: String,
        mimeType: String,
        allowMove: Boolean
    ): String? {
        val directory = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            mimeType.getFolderNameFromMimeType()
        )
        directory.mkdirs()
        
        val extension = fileName.substringAfterLast('.', "")
        val target = File(directory, if (extension.isEmpty()) fileId else "$fileId.$extension")
        
        if (target.exists() && target.length() > 0) {
            return target.absolutePath
        }
        
        val source = if (fileUri.scheme == SCHEME_FILE) fileUri.path?.let { File(it) } else null
        
        if (allowMove && source != null && source.renameTo(target)) {
            return target.absolutePath
        }
        
        return try {
            val stream = context.contentResolver.openInputStream(fileUri) ?: return null
            
            stream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            if (allowMove) {
                source?.delete()
            }
            
            target.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Unable to keep $fileUri locally: ${e.message}", e)
            target.delete()
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to keep $fileUri locally: ${e.message}", e)
            target.delete()
            null
        }
    }
    
    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        
        const val UNLIMITED_ATTEMPTS = Int.MAX_VALUE
        
        private const val TAG = "UploadManager"
        private const val SCHEME_FILE = "file"
        private const val DEFAULT_FILE_NAME = "file"
    }
}
