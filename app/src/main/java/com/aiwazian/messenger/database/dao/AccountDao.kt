/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aiwazian.messenger.database.entity.AccountEntity

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(account: AccountEntity)
    
    @Update
    suspend fun update(account: AccountEntity)
    
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
    
    @Query("UPDATE account SET isCurrent = TRUE WHERE userId = :id")
    suspend fun setCurrentInternal(id: Long)
    
    @Transaction
    suspend fun setCurrent(id: Long) {
        resetCurrent()
        setCurrentInternal(id)
    }
}
