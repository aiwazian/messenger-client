/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloaderManager: DownloaderManager
) {
    suspend fun upload(uri: Uri, uploadUrl: String, fileId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileSize = uri.getFileSize(context) ?: 0
                val mimeType = uri.getFileType(context).toMediaTypeOrNull()

                val requestBody = ProgressRequestBody(mimeType, fileSize, { progress ->
                    // TODO Update progress
                }) {
                    context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Unable to open input stream")
                }

                val request = Request.Builder().url(uploadUrl).put(requestBody).build()
                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val targetFile = downloaderManager.getFile(fileId, "")
                    if (!targetFile.exists()) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    Result.success(targetFile.absolutePath)
                } else {
                    Result.failure(Exception("UploadManager request unsuccessful"))
                }
            } catch (e: Exception) {
                Log.e("UploadManager", "Upload error", e)
                Result.failure(e)
            }
        }
}
