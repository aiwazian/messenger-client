/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.di

import com.aiwazian.messenger.network.RetrofitInstance
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
    @ApiClient
    fun provideApiClient() = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    
    @Provides
    @Singleton
    @FileClient
    fun provideFileClient() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
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
    fun provideChatFolderApi(): ChatFolderApi = RetrofitInstance.chatFolderApi
    
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
    fun provideNotificationSettingsApi(): NotificationSettingsApi =
        RetrofitInstance.notificationSettingsApi
    
    @Provides
    @Singleton
    fun provideSessionManager(): SessionManager = SessionManager
}