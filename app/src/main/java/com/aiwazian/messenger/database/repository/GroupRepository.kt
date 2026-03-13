package com.aiwazian.messenger.database.repository

import android.util.Log
import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.database.mappers.toEntity
import com.aiwazian.messenger.database.mappers.toGroup
import com.aiwazian.messenger.services.GroupService
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val groupService: GroupService,
    private val groupDao: GroupDao
) {
    
    suspend fun create(groupInfo: GroupInfo): Long? {
        try {
            val createdId = groupService.create(groupInfo)
            
            if (createdId == null) {
                return null
            }
            
            groupDao.insert(groupInfo.toEntity())
            
            return createdId
        } catch (e: Exception) {
            Log.e(
                "GroupRepository",
                "Ошибка при создании канала",
                e
            )
            
            return null
        }
    }
    
    suspend fun get(id: Long): GroupInfo? {
        try {
            val group = groupService.get(id)
            
            if (group != null) {
                groupDao.insert(group.toEntity())
                return group
            }
            
            return groupDao.get(id)?.toGroup()
        } catch (e: Exception) {
            Log.e(
                "GroupRepository",
                "Ошибка при получении группы",
                e
            )
            
            return null
        }
    }
    
    suspend fun delete(id: Long): Boolean {
        try {
            val isDeleted = groupService.delete(id)
            
            if (isDeleted) {
                groupDao.delete(id)
            }
            
            return isDeleted
        } catch (e: Exception) {
            Log.e(
                "GroupRepository",
                "Ошибка при получении участников группы",
                e
            )
            
            return false
        }
    }
}