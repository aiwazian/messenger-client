/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.group

import android.util.Log
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.dto.SetNoCopyRequestDto
import javax.inject.Inject

class GroupContentProtectionRepository @Inject constructor(
    private val groupApi: GroupApi,
    private val groupDao: GroupDao
) {
    
    suspend fun setNoCopy(group: Group, noCopy: Boolean): Result<Unit> {
        return try {
            val response = groupApi.setNoCopy(group.id, SetNoCopyRequestDto(noCopy))
            if (response.isSuccessful) {
                groupDao.insert(group.copy(noCopy = noCopy).toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Set no copy failed: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при изменении запрета копирования группы", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "GroupContentProtectionRepository"
    }
}
