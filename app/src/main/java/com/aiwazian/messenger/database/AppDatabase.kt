/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.AttachmentDao
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.DraftDao
import com.aiwazian.messenger.database.dao.FileDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.database.entity.AttachmentEntity
import com.aiwazian.messenger.database.entity.AvatarEntity
import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.database.entity.ChatEntity
import com.aiwazian.messenger.database.entity.DraftEntity
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        ChannelEntity::class,
        AccountEntity::class,
        GroupEntity::class,
        AttachmentEntity::class,
        FileEntity::class,
        ChatEntity::class,
        AvatarEntity::class,
        DraftEntity::class
    ],
    version = 46,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 38, to = 39),
        AutoMigration(from = 39, to = 40),
        AutoMigration(from = 40, to = 41),
        AutoMigration(from = 41, to = 42),
        AutoMigration(from = 42, to = 43),
        AutoMigration(from = 44, to = 45),
        AutoMigration(from = 45, to = 46),
    ]
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    
    abstract fun channelDao(): ChannelDao
    
    abstract fun groupDao(): GroupDao
    
    abstract fun chatDao(): ChatDao
    
    abstract fun accountDao(): AccountDao
    
    abstract fun messageDao(): MessageDao
    
    abstract fun attachmentDao(): AttachmentDao
    
    abstract fun fileDao(): FileDao
    
    abstract fun avatarDao(): AvatarDao
    
    abstract fun draftDao(): DraftDao
}
