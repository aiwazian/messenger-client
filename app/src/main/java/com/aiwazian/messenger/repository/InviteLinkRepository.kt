/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.ChatApi
import javax.inject.Inject

class InviteLinkRepository @Inject constructor(
    private val chatApi: ChatApi
) {

    suspend fun getInviteLinkInfo(code: String): Result<InviteLinkInfo> {
        return try {
            val response = chatApi.getInviteLinkInfo(code)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Result.success(dto.toDomain())
                } else {
                    Result.failure(Exception("No invite link info returned"))
                }
            } else {
                Result.failure(Exception("Failed to get invite link info"))
            }
        } catch (e: Exception) {
            Log.e("InviteLinkRepository", "Error getting invite link info", e)
            Result.failure(e)
        }
    }

    suspend fun joinViaInviteCode(code: String): Result<Unit> {
        return try {
            val response = chatApi.joinViaInviteCode(code)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join via invite link failed"))
            }
        } catch (e: Exception) {
            Log.e("InviteLinkRepository", "Error joining via invite link", e)
            Result.failure(e)
        }
    }

    suspend fun deleteInviteLink(inviteLinkId: Long): Result<Unit> {
        return try {
            val response = chatApi.deleteInviteLink(inviteLinkId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete invite link failed"))
            }
        } catch (e: Exception) {
            Log.e("InviteLinkRepository", "Error deleting invite link", e)
            Result.failure(e)
        }
    }
}
