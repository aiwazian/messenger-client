package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.AccountEntity

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(account: AccountEntity)
    
    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun get(id: Int): AccountEntity?
    
    @Query("SELECT * FROM account WHERE isCurrent = 1")
    suspend fun getMe(): AccountEntity?
    
    @Delete
    suspend fun delete(account: AccountEntity)
}