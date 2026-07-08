/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.di

import android.content.Context
import androidx.room3.Room
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.AttachmentDao
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.FileDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext
        context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aiwazian.messenger"
        ).fallbackToDestructiveMigration(true).build()
    }
    
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
    
    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()
    
    @Provides
    fun provideGroupDao(database: AppDatabase): GroupDao = database.groupDao()
    
    @Provides
    fun provideChat(database: AppDatabase): ChatDao = database.chatDao()
    
    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()
    
    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()
    
    @Provides
    fun provideAttachmentDao(database: AppDatabase): AttachmentDao = database.attachmentDao()
    
    @Provides
    fun provideFileDao(database: AppDatabase): FileDao = database.fileDao()
    
    @Provides
    fun provideAvatarDao(database: AppDatabase): AvatarDao = database.avatarDao()
}