/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiwazian.messenger.database.entity.GroupBlackListEntity
import com.aiwazian.messenger.database.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMemberDao {

    // ===== Group Members =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: GroupMemberEntity)

    @Transaction
    suspend fun insertAll(members: List<GroupMemberEntity>) {
        members.forEach { insert(it) }
    }

    @Query("SELECT * FROM group_member WHERE groupId = :groupId")
    fun getMembersFlow(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("SELECT userId FROM group_member WHERE groupId = :groupId")
    fun getMemberUserIdsFlow(groupId: Long): Flow<List<Long>>

    @Query("SELECT userId FROM group_member WHERE groupId = :groupId")
    suspend fun getMemberUserIds(groupId: Long): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM group_member WHERE groupId = :groupId AND userId = :userId)")
    suspend fun isMember(groupId: Long, userId: Long): Boolean

    @Query("DELETE FROM group_member WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: Long, userId: Long)

    @Query("DELETE FROM group_member WHERE groupId = :groupId")
    suspend fun clearMembers(groupId: Long)

    // ===== Group BlackList =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlackListEntry(entry: GroupBlackListEntity)

    @Transaction
    suspend fun insertAllBlackList(entries: List<GroupBlackListEntity>) {
        entries.forEach { insertBlackListEntry(it) }
    }

    @Query("SELECT * FROM group_blacklist WHERE groupId = :groupId")
    fun getBlackListFlow(groupId: Long): Flow<List<GroupBlackListEntity>>

    @Query("SELECT userId FROM group_blacklist WHERE groupId = :groupId")
    fun getBlackListUserIdsFlow(groupId: Long): Flow<List<Long>>

    @Query("SELECT userId FROM group_blacklist WHERE groupId = :groupId")
    suspend fun getBlackListUserIds(groupId: Long): List<Long>

    @Query("DELETE FROM group_blacklist WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeFromBlackList(groupId: Long, userId: Long)

    @Query("DELETE FROM group_blacklist WHERE groupId = :groupId")
    suspend fun clearBlackList(groupId: Long)
}
