package com.aiwazian.messenger.api

import com.aiwazian.messenger.interfaces.ApiService
import com.aiwazian.messenger.services.TokenManager
import com.aiwazian.messenger.utils.Constants
import com.aiwazian.messenger.utils.DownloadManager
import com.aiwazian.messenger.utils.Route
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    
    private const val BASE_URL = Constants.SERVER_URL
    
    private val skipAuthPaths = listOf(
        Route.LOGIN,
        Route.REGISTER,
        Route.FIND_USER_BY_LOGIN
    )
    
    private val okHttpClient = OkHttpClient.Builder().connectTimeout(
        1,
        TimeUnit.MINUTES
    ).readTimeout(
        1,
        TimeUnit.MINUTES
    ).writeTimeout(
        1,
        TimeUnit.MINUTES
    ).addInterceptor(
        AuthInterceptor(
            getToken = {
                TokenManager.getToken()
            },
            shouldSkipAuth = { path ->
                skipAuthPaths.contains(path)
            },
            onUnauthorized = {
                TokenManager.getUnauthorizedCallback()?.invoke()
            })
    ).addNetworkInterceptor(
        ProgressInterceptor { url, progress ->
            DownloadManager.updateProgress(
                url,
                progress
            )
        }).build()
    
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}