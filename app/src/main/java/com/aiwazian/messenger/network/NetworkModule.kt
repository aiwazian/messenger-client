/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network

import com.aiwazian.messenger.network.api.AuthApi
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.api.PrivacyApi
import com.aiwazian.messenger.network.api.SearchApi
import com.aiwazian.messenger.network.api.SessionApi
import com.aiwazian.messenger.network.api.UserApi
import com.aiwazian.messenger.utils.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApi(): AuthApi = RetrofitInstance.authApi
    
    @Provides
    @Singleton
    fun provideUserApi(): UserApi = RetrofitInstance.userApi
    
    @Provides
    @Singleton
    fun provideMessageApi(): MessageApi = RetrofitInstance.messageApi
    
    @Provides
    @Singleton
    fun provideChatApi(): ChatApi = RetrofitInstance.chatApi
    
    @Provides
    @Singleton
    fun provideGroupApi(): GroupApi = RetrofitInstance.groupApi
    
    @Provides
    @Singleton
    fun provideChannelApi(): ChannelApi = RetrofitInstance.channelApi
    
    @Provides
    @Singleton
    fun provideSearchApi(): SearchApi = RetrofitInstance.searchApi
    
    @Provides
    @Singleton
    fun provideSessionApi(): SessionApi = RetrofitInstance.sessionApi
    
    @Provides
    @Singleton
    fun providePrivacyApi(): PrivacyApi = RetrofitInstance.privacyApi
    
    @Provides
    @Singleton
    fun provideSessionManager(): SessionManager = SessionManager
}