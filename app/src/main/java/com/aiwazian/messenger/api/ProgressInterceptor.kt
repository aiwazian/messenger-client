package com.aiwazian.messenger.api

import com.aiwazian.messenger.utils.ProgressResponseBody
import okhttp3.Interceptor
import okhttp3.Response

class ProgressInterceptor(
    private val onProgress: (url: String, progress: Int) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url.toString()
        val body = response.body
        return response.newBuilder()
            .body(
                ProgressResponseBody(
                    url,
                    body,
                    onProgress
                )
            )
            .build()
    }
}
