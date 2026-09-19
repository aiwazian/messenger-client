package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.PrivacyExceptions
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toRequestLists
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

    suspend fun updateBioPrivacy(
        bio: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                bio = bio,
                exceptions = exceptions(PrivacyField.BIO, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateLastSeenPrivacy(
        lastSeen: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                lastSeen = lastSeen,
                exceptions = exceptions(PrivacyField.LAST_SEEN, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateDateOfBirthPrivacy(
        dateOfBirth: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                dateOfBirth = dateOfBirth,
                exceptions = exceptions(PrivacyField.DATE_OF_BIRTH, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateInvitesPrivacy(
        invites: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                invites = invites,
                exceptions = exceptions(PrivacyField.INVITES, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateProfilePhotoPrivacy(
        profilePhoto: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                profilePhoto = profilePhoto,
                exceptions = exceptions(PrivacyField.PROFILE_PHOTO, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateForwardedProfilePrivacy(
        forwardedProfile: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                forwardedProfile = forwardedProfile,
                exceptions = exceptions(PrivacyField.FORWARDED_PROFILE, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateForwardAndCopyPrivacy(
        forwardAndCopy: PrivacyLevel,
        exceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            request.copy(
                forwardAndCopy = forwardAndCopy,
                exceptions = exceptions(PrivacyField.FORWARD_AND_COPY, exceptions) ?: request.exceptions
            )
        }
    }

    suspend fun updateForwardingPrivacy(
        forwardedProfile: PrivacyLevel,
        forwardAndCopy: PrivacyLevel,
        forwardedProfileExceptions: PrivacyExceptions? = null,
        forwardAndCopyExceptions: PrivacyExceptions? = null
    ): Result<Unit> {
        return update { request ->
            val exceptionLists = buildMap {
                forwardedProfileExceptions?.let {
                    put(PrivacyField.FORWARDED_PROFILE, it.toRequestLists())
                }
                forwardAndCopyExceptions?.let {
                    put(PrivacyField.FORWARD_AND_COPY, it.toRequestLists())
                }
            }.takeIf { it.isNotEmpty() }

            request.copy(
                forwardedProfile = forwardedProfile,
                forwardAndCopy = forwardAndCopy,
                exceptions = exceptionLists ?: request.exceptions
            )
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

    private fun exceptions(field: PrivacyField, value: PrivacyExceptions?) =
        value?.let { mapOf(field to it.toRequestLists()) }

    private suspend fun update(
        buildRequest: (UpdatePrivacySettingsRequestDto) -> UpdatePrivacySettingsRequestDto
    ): Result<Unit> {
        return try {
            val request = buildRequest(UpdatePrivacySettingsRequestDto())
            val response = privacyApi.updatePrivacySettings(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update privacy settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
