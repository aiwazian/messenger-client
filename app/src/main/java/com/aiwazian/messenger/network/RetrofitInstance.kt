/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network

import com.aiwazian.messenger.BuildConfig
import com.aiwazian.messenger.network.api.AuthApi
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.ChatFolderApi
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.api.NotificationSettingsApi
import com.aiwazian.messenger.network.api.PrivacyApi
import com.aiwazian.messenger.network.api.SearchApi
import com.aiwazian.messenger.network.api.SessionApi
import com.aiwazian.messenger.network.api.UserApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    
    private val skipAuthPaths = listOf(
        "auth/signin",
        "auth/signup",
        "auth/check",
        "auth/password-reset/request",
        "auth/password-reset/verify",
        "auth/password-reset/confirm",
    )
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            AuthInterceptor(
                shouldSkipAuth = { path ->
                    skipAuthPaths.any { path.contains(it) }
                })
        ).build()
    
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(okHttpClient)
        .build()
    
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }
    val messageApi: MessageApi by lazy { retrofit.create(MessageApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }
    val chatFolderApi: ChatFolderApi by lazy { retrofit.create(ChatFolderApi::class.java) }
    val groupApi: GroupApi by lazy { retrofit.create(GroupApi::class.java) }
    val channelApi: ChannelApi by lazy { retrofit.create(ChannelApi::class.java) }
    val searchApi: SearchApi by lazy { retrofit.create(SearchApi::class.java) }
    val sessionApi: SessionApi by lazy { retrofit.create(SessionApi::class.java) }
    val privacyApi: PrivacyApi by lazy { retrofit.create(PrivacyApi::class.java) }
    val notificationSettingsApi: NotificationSettingsApi by lazy {
        retrofit.create(NotificationSettingsApi::class.java)
    }
}
