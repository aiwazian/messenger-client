package com.aiwazian.messenger.utils

object DownloadManager {
    var onProgressUpdate: ((url: String, progress: Int) -> Unit)? = null
    
    fun updateProgress(url: String, progress: Int) {
        onProgressUpdate?.invoke(url, progress)
    }
}
