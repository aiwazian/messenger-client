/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.database.entity.AttachmentEntity
import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.database.entity.UserEntity

@Database(
    entities = [UserEntity::class, MessageEntity::class, ChannelEntity::class, AccountEntity::class, GroupEntity::class, AttachmentEntity::class],
    exportSchema = false,
    version = 14
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun channelDao(): ChannelDao

    abstract fun groupDao(): GroupDao

    abstract fun chatDao(): ChatDao

    abstract fun accountDao(): AccountDao

    abstract fun messageDao(): MessageDao
}
