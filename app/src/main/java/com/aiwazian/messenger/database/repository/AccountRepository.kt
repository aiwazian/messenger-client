package com.aiwazian.messenger.database.repository

import com.aiwazian.messenger.data.LocalAccount
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.mappers.toLocal
import javax.inject.Inject

class AccountRepository @Inject constructor(private val accountDao: AccountDao) {
    suspend fun getCurrent(): LocalAccount? {
        return accountDao.getMe()?.toLocal()
    }
}