package com.aiwazian.messenger.database

import android.content.Context
import androidx.room.Room
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.FolderChatDao
import com.aiwazian.messenger.database.dao.FolderDao
import com.aiwazian.messenger.database.dao.GroupDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).fallbackToDestructiveMigration(true).build()
    }
    
    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao {
        return database.folderDao()
    }
    
    @Provides
    fun provideFolderChatDao(database: AppDatabase): FolderChatDao {
        return database.folderChatDao()
    }
    
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao {
        return database.channelDao()
    }
    
    @Provides
    fun provideAccount(database: AppDatabase): AccountDao {
        return database.accountDao()
    }
    
    @Provides
    fun provideGroup(database: AppDatabase): GroupDao {
        return database.groupDao()
    }
    
    @Provides
    fun provideChat(database: AppDatabase): ChatDao {
        return database.chatDao()
    }
}