/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
import com.aiwazian.messenger.domain.DownloadStatus
import com.aiwazian.messenger.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHandler @Inject constructor(
    private val chatRepository: ChatRepository,
    private val downloaderManager: DownloaderManager
) {
    suspend fun openFile(
        chatId: Long,
        messageId: Int,
        fileId: String,
        fileName: String,
        fileSize: Long,
        localUri: String?
    ) {
        if (localUri != null && downloaderManager.isDownloaded(fileId, fileName.substringAfterLast('.', ""))) {
            downloaderManager.openFile(localUri)
        } else {
            val isDownloading = downloaderManager.downloads.value.any { it.fileId == fileId && it.status == DownloadStatus.DOWNLOADING }
            if (isDownloading) return

            try {
                val response = chatRepository.getDownloadUrl(chatId, messageId, fileId)
                if (response != null) {
                    downloaderManager.download(
                        url = response.downloadUrl,
                        fileName = fileName,
                        chatId = chatId,
                        messageId = messageId,
                        fileId = fileId,
                        size = fileSize
                    )
                }
            } catch (e: Exception) {
                Log.e("FileHandler", "Error starting download", e)
            }
        }
    }
}
