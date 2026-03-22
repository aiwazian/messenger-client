/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.utils.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class AuthInterceptor(
    private val shouldSkipAuth: (String) -> Boolean
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (shouldSkipAuth(path)) {
            return chain.proceed(request)
        }

        // Ждём инициализации SessionManager перед выполнением запроса
        if (!SessionManager.isInit) {
            runBlocking {
                while (!SessionManager.isInit) {
                    kotlinx.coroutines.delay(100)
                }
            }
        }

        val token = SessionManager.getToken()
        val authRequest = if (token.isNotEmpty()) {
            request.newBuilder()
                .addHeader(
                    "Authorization",
                    "Bearer $token"
                )
                .build()
        } else {
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body("".toResponseBody(null))
                .build()
        }

        val response = chain.proceed(authRequest)

        if (response.code == 401 && SessionManager.isAuthorized()) {
            SessionManager.setAuthorized(false)
            SessionManager.getUnauthorizedCallback()?.invoke()
        }

        return response
    }
}
