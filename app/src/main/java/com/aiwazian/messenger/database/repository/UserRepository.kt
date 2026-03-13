package com.aiwazian.messenger.database.repository

import android.util.Log
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.database.mappers.toEntity
import com.aiwazian.messenger.database.mappers.toUser
import com.aiwazian.messenger.services.UserService
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userService: UserService,
    private val userDao: UserDao,
    private val accountDao: AccountDao
) {
    
    suspend fun getMe(): UserInfo? {
        try {
            val response = RetrofitInstance.api.getMe()
            
            val user = response.body()
            
            if (user != null) {
                val userEntity = user.toEntity()
                userDao.insert(userEntity)
                
                val accountEntity = AccountEntity(id = userEntity.id, isCurrent = true)
                accountDao.add(accountEntity)
                
                return user
            }
        } catch (e: Exception) {
            Log.e(
                "UserRepository",
                "Ошибка при запросе Get Me",
                e
            )
        }
        
        val accountEntity = accountDao.getMe()
        
        if (accountEntity == null) {
            return null
        }
        
        val user = userDao.get(accountEntity.id)
        
        return user?.toUser()
    }
    
    suspend fun getById(id: Long): UserInfo? {
        try {
            val user = userService.getById(id)
            
            if (user != null) {
                userDao.insert(user.toEntity())
                return user
            }
        } catch (e: Exception) {
            Log.e(
                "UserRepository",
                "Ошибка при получении профиля",
                e
            )
        }
        
        val localUser = userDao.get(id)
        
        return localUser?.toUser()
    }
    
    suspend fun updateProfile(user: UserInfo): Boolean {
        try {
            userDao.insert(user.toEntity())
            return userService.updateProfile(user)
        } catch (e: Exception) {
            Log.e(
                "UserRepository",
                "Ошибка при обновлении профиля",
                e
            )
            return false
        }
    }
}