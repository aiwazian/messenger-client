/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.PrivacyApi
import com.aiwazian.messenger.network.dto.UpdatePrivacySettingsRequestDto
import javax.inject.Inject

class PrivacyRepository @Inject constructor(
    private val privacyApi: PrivacyApi
) {
    
    suspend fun getPrivacySettings(): Result<PrivacySettings> {
        return try {
            val response = privacyApi.getPrivacySettings()
            if (response.isSuccessful) {
                val settings = response.body()?.toDomain()
                if (settings != null) {
                    Result.success(settings)
                } else {
                    Result.failure(Exception("Body is null"))
                }
            } else {
                Result.failure(Exception("Failed to get privacy settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBioPrivacy(bio: PrivacyLevel): Result<Unit> {
        return try {
            val request = UpdatePrivacySettingsRequestDto(bio = bio)
            val response = privacyApi.updatePrivacySettings(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update bio privacy: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateLastSeenPrivacy(lastSeen: PrivacyLevel): Result<Unit> {
        return try {
            val request = UpdatePrivacySettingsRequestDto(lastSeen = lastSeen)
            val response = privacyApi.updatePrivacySettings(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update last seen privacy: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDateOfBirthPrivacy(dateOfBirth: PrivacyLevel): Result<Unit> {
        return try {
            val request = UpdatePrivacySettingsRequestDto(dateOfBirth = dateOfBirth)
            val response = privacyApi.updatePrivacySettings(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update date of birth privacy: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateInvitesPrivacy(invites: PrivacyLevel): Result<Unit> {
        return try {
            val request = UpdatePrivacySettingsRequestDto(invites = invites)
            val response = privacyApi.updatePrivacySettings(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update invites privacy: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDeleteAfterDays(days: Int): Result<Unit> {
        return try {
            val request = UpdatePrivacySettingsRequestDto(deleteAfterDays = days)
            val response = privacyApi.updatePrivacySettings(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update delete after days: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
