package com.aiwazian.messenger.data

import com.aiwazian.messenger.enums.DownloadStatus
import okhttp3.ResponseBody
import retrofit2.Call

data class DownloadItem(
    val url: String,
    val fileName: String,
    var progress: Int = 0,
    var status: DownloadStatus = DownloadStatus.PENDING,
    var call: Call<ResponseBody>? = null,
    var onComplete: (() -> Unit)? = null
)
