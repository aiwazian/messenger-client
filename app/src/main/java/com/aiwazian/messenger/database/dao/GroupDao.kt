package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aiwazian.messenger.database.entity.GroupEntity

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(groupEntity: GroupEntity)
    
    @Query("SELECT * FROM 'group' WHERE id = :id")
    suspend fun get(id: Long): GroupEntity?
    
    @Update
    suspend fun update(groupEntity: GroupEntity)
    
    @Delete
    suspend fun remove(groupEntity: GroupEntity)
    
    @Query("DELETE FROM 'group' WHERE id = :id")
    suspend fun delete(id: Long)
}