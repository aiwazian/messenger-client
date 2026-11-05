/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePackManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)
    private var sessionId = 0
    private var pendingConfirmationState: SplitInstallSessionState? = null

    fun isLanguageInstalled(languageCode: String): Boolean {
        return splitInstallManager.installedLanguages.contains(languageCode)
    }

    fun downloadLanguage(languageCode: String): Flow<LanguagePackStatus> = callbackFlow {
        val request = SplitInstallRequest.newBuilder()
            .addLanguage(Locale.forLanguageTag(languageCode))
            .build()

        val listener = SplitInstallStateUpdatedListener { state ->
            val boundSessionId = sessionId
            if (boundSessionId != 0 && state.sessionId() != boundSessionId) {
                return@SplitInstallStateUpdatedListener
            }

            when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> trySend(LanguagePackStatus.Downloading)
                SplitInstallSessionStatus.DOWNLOADED, SplitInstallSessionStatus.INSTALLING -> {
                    trySend(LanguagePackStatus.Installing)
                }

                SplitInstallSessionStatus.INSTALLED -> {
                    pendingConfirmationState = null
                    trySend(LanguagePackStatus.Installed)
                    close()
                }

                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    pendingConfirmationState = state
                    trySend(LanguagePackStatus.RequiresConfirmation)
                }

                SplitInstallSessionStatus.FAILED -> {
                    pendingConfirmationState = null
                    trySend(LanguagePackStatus.Failed(state.errorCode()))
                    close()
                }

                SplitInstallSessionStatus.CANCELED -> {
                    pendingConfirmationState = null
                    trySend(LanguagePackStatus.Canceled)
                    close()
                }

                else -> Unit
            }
        }

        splitInstallManager.registerListener(listener)
        splitInstallManager.startInstall(request)
            .addOnSuccessListener { id -> sessionId = id }
            .addOnFailureListener { exception ->
                val errorCode = (exception as? SplitInstallException)?.errorCode ?: -1
                trySend(LanguagePackStatus.Failed(errorCode))
                close()
            }

        awaitClose {
            splitInstallManager.unregisterListener(listener)
            sessionId = 0
            pendingConfirmationState = null
        }
    }

    fun startUserConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val state = pendingConfirmationState ?: return false
        return splitInstallManager.startConfirmationDialogForResult(state, launcher)
    }

    fun cancelDownload() {
        val id = sessionId
        if (id != 0) {
            splitInstallManager.cancelInstall(id)
        }
    }
}
