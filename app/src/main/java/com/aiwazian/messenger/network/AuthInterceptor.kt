/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network

import com.aiwazian.messenger.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.time.Duration.Companion.milliseconds

class AuthInterceptor(
    private val shouldSkipAuth: (String) -> Boolean
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        
        if (shouldSkipAuth(path)) {
            return chain.proceed(request)
        }
        
        if (!SessionManager.isInit) {
            runBlocking {
                while (!SessionManager.isInit) {
                    delay(100.milliseconds)
                }
            }
        }
        
        val token = SessionManager.getToken()
        val authRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        val response = chain.proceed(authRequest)
        
        if (response.code == 401) {
            SessionManager.setAuthorized(false)
            SessionManager.getUnauthorizedCallback()?.invoke()
        }
        
        return response
    }
}
