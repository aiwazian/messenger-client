/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.aiwazian.messenger.database.entity.AccountEntity

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(account: AccountEntity)
    
    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE account SET token = :token, createdAt = :createdAt WHERE userId = :userId")
    suspend fun updateCredentials(userId: Long, token: String, createdAt: Long)

    @Query("UPDATE account SET installationId = :installationId WHERE userId = :userId")
    suspend fun updateInstallationId(userId: Long, installationId: String?)

    @Query(
        "UPDATE account SET isCurrent = FALSE " +
            "WHERE id NOT IN (SELECT MAX(id) FROM account WHERE isCurrent = TRUE)"
    )
    suspend fun clearStaleCurrentFlags()

    @Query("SELECT * FROM account WHERE userId = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?
    
    @Query("SELECT * FROM account WHERE isCurrent = TRUE LIMIT 1")
    suspend fun getCurrentAccount(): AccountEntity?

    @Query("SELECT token FROM account WHERE isCurrent = TRUE ORDER BY id DESC LIMIT 1")
    suspend fun getCurrentToken(): String?
    
    @Query("DELETE FROM account WHERE isCurrent = TRUE")
    suspend fun deleteCurrent()
    
    @Query("SELECT * FROM account")
    suspend fun getAllAccounts(): List<AccountEntity>
    
    @Query("UPDATE account SET isCurrent = FALSE")
    suspend fun resetCurrent()
    
    @Query("UPDATE account SET installationId = NULL")
    suspend fun clearInstallationIds()
    
    @Query("SELECT * FROM account WHERE token != '' AND token IS NOT NULL ORDER BY id ASC LIMIT 1")
    suspend fun getFirstAccountWithToken(): AccountEntity?

    @Query("UPDATE account SET isCurrent = TRUE WHERE userId = :id")
    suspend fun setCurrentInternal(id: Long)
    
    @Transaction
    suspend fun setCurrent(id: Long) {
        resetCurrent()
        setCurrentInternal(id)
    }
}
