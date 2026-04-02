/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.PrivacyApi
import com.aiwazian.messenger.network.dto.UpdatePrivacySettingsRequestDto
import javax.inject.Inject

class PrivacyRepository @Inject constructor(
    private val privacyApi: PrivacyApi
) {

    suspend fun getPrivacySettings(): PrivacySettings? {
        val response = privacyApi.getPrivacySettings()
        return if (response.isSuccessful) {
            response.body()?.toDomain()
        } else {
            null
        }
    }

    suspend fun updateBioPrivacy(bio: Int): Boolean {
        val request = UpdatePrivacySettingsRequestDto(bio = bio)
        val response = privacyApi.updatePrivacySettings(request)
        return response.isSuccessful
    }

    suspend fun updateDateOfBirthPrivacy(dateOfBirth: Int): Boolean {
        val request = UpdatePrivacySettingsRequestDto(dateOfBirth = dateOfBirth)
        val response = privacyApi.updatePrivacySettings(request)
        return response.isSuccessful
    }

    suspend fun updateInvitesPrivacy(invites: Int): Boolean {
        val request = UpdatePrivacySettingsRequestDto(invites = invites)
        val response = privacyApi.updatePrivacySettings(request)
        return response.isSuccessful
    }
}
