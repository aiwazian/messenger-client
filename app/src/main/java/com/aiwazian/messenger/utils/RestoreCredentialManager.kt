/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.aiwazian.messenger.network.api.AuthApi
import com.aiwazian.messenger.network.dto.RestoreAuthenticationRequestDto
import com.aiwazian.messenger.network.dto.RestoreRegistrationRequestDto
import com.aiwazian.messenger.network.dto.SignInResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class RestoreCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun registerAfterSignIn() {
        try {
            val options = authApi.restoreRegistrationOptions().body()?.options ?: return
            val requestJson = options.toString()

            val response = try {
                credentialManager.createCredential(
                    context,
                    CreateRestoreCredentialRequest(requestJson = requestJson)
                )
            } catch (e: E2eeUnavailableException) {
                credentialManager.createCredential(
                    context,
                    CreateRestoreCredentialRequest(
                        requestJson = requestJson,
                        isCloudBackupEnabled = false
                    )
                )
            }

            val registrationJson = (response as? CreateRestoreCredentialResponse)?.responseJson

            if (registrationJson != null) {
                authApi.registerRestoreCredential(RestoreRegistrationRequestDto(registrationJson))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Restore credential registration failed: ${e.message}")
        }
    }

    suspend fun tryRestore(): SignInResponseDto? {
        return try {
            withTimeout(RESTORE_TIMEOUT) {
                val options = authApi.restoreAuthenticationOptions().body()?.options ?: return@withTimeout null

                val result = credentialManager.getCredential(
                    context,
                    GetCredentialRequest(listOf(GetRestoreCredentialOption(requestJson = options.toString())))
                )

                val credential = result.credential as? RestoreCredential

                if (credential == null) {
                    return@withTimeout null
                }

                val response = authApi.restoreAuthentication(
                    RestoreAuthenticationRequestDto(
                        assertionJson = credential.authenticationResponseJson,
                        deviceModel = deviceInfoProvider.getDeviceModel(),
                        osVersion = deviceInfoProvider.getOsVersion(),
                        osName = deviceInfoProvider.getOsName()
                    )
                )

                if (response.isSuccessful) response.body() else null
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Restore credential sign-in timed out: ${e.message}")
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Restore credential sign-in failed: ${e.message}")
            null
        }
    }

    suspend fun clear() {
        try {
            credentialManager.clearCredentialState(
                ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Restore credential clear failed: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "RestoreCredentialManager"

        val RESTORE_TIMEOUT = 8.seconds
    }
}
