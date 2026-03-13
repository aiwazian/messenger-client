package com.aiwazian.messenger.database.repository

import android.util.Log
import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.mappers.toChannel
import com.aiwazian.messenger.database.mappers.toEntity
import com.aiwazian.messenger.services.ChannelService
import com.aiwazian.messenger.types.EntityId
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val channelService: ChannelService,
    private val channelDao: ChannelDao
) {
    
    suspend fun get(id: Long): ChannelInfo? {
        try {
            val channel = channelService.get(id)
            
            if (channel != null) {
                channelDao.insert(channel.toEntity())
                return channel
            }
            
            return channelDao.get(id)?.toChannel()
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при получении канала",
                e
            )
        }
        
        val localChannel = channelDao.get(id)
        
        return localChannel?.toChannel()
    }
    
    suspend fun create(channelInfo: ChannelInfo): Long? {
        try {
            val createdId = channelService.create(channelInfo)
            
            if (createdId == null) {
                return null
            }
            
            channelDao.insert(channelInfo.toEntity())
            
            return createdId
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при создании канала",
                e
            )
            
            return null
        }
    }
    
    suspend fun save(channelInfo: ChannelInfo): Long? {
        try {
            val savedId = channelService.save(channelInfo)
            
            if (savedId == null) {
                return null
            }
            
            channelDao.insert(channelInfo.toEntity())
            
            return savedId
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при сохранении канала",
                e
            )
            
            return null
        }
    }
    
    suspend fun delete(id: Long): Boolean {
        try {
            val isDeleted = channelService.delete(id)
            
            if (isDeleted) {
                channelDao.delete(id)
            }
            
            return isDeleted
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при удалении канала",
                e
            )
            
            return false
        }
    }
    
    suspend fun getSubscribers(id: Long): List<UserInfo> {
        try {
            val subscribers = channelService.getSubscribers(id)
            
            return subscribers ?: emptyList()
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при получении подписчиков канала",
                e
            )
            
            return emptyList()
        }
    }
    
    suspend fun join(id: Long): Boolean {
        try {
            channelService.join(id)
            
            val channel = channelDao.get(id)
            
            if (channel != null) {
                channelDao.update(channel.copy(isSubscribed = true))
            }
            
            return true
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при подписке на канал",
                e
            )
            
            return false
        }
    }
    
    suspend fun checkIsBusyPublicLink(link: String): Boolean? {
        return try {
            channelService.isBusyPublicLick(link)
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при проверке публичной ссылки канала",
                e
            )
            
            null
        }
    }
    
    suspend fun leave(id: Long): Boolean {
        try {
            val channel = channelDao.get(id)
            
            if (channel != null) {
                channelDao.update(channel.copy(isSubscribed = false))
            }
            
            channelService.leave(id)
            
            return true
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при отписке от канала",
                e
            )
            
            return false
        }
    }
    
}