/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.socket.ServerSyncService
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.GlobalBottomSheetManager
import com.aiwazian.messenger.ui.components.navigation.AppNavDisplay
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.screens.chat.components.InviteLinkBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.MusicPlayerSheet
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.InAppUpdateManager
import com.aiwazian.messenger.utils.RestoreCredentialManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var serverSyncService: ServerSyncService

    @Inject
    lateinit var restoreCredentialManager: RestoreCredentialManager

    @Inject
    lateinit var realtimeEventSyncService: Lazy<RealtimeEventSyncService>

    @Inject
    lateinit var globalBottomSheetManager: GlobalBottomSheetManager
    
    private var startRoute by mutableStateOf<AppRoute?>(null)
    private val externalRouteFlow = MutableSharedFlow<AppRoute>(extraBufferCapacity = 1)
    
    private var inAppUpdateManager: InAppUpdateManager? = null
    private var isUpdateReadyToInstall by mutableStateOf(false)
    
    private var isAuthorized by mutableStateOf<Boolean?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        SessionManager.setSessionEndCallback {
            restartApp()
        }
        
        installSplashScreen().setKeepOnScreenCondition {
            isAuthorized == null
        }

        enableEdgeToEdge()

        inAppUpdateManager = InAppUpdateManager(this) {
            isUpdateReadyToInstall = true
        }

        lifecycleScope.launch {
            SessionManager.loadSession()
            var authorized = SessionManager.hasAnySession()

            if (!authorized) {
                val restored = restoreCredentialManager.tryRestore()

                if (restored != null) {
                    SessionManager.saveSession(
                        restored.userId,
                        restored.token,
                        restored.createdAt
                    )
                    authorized = true
                }
            }

            if (authorized) {
                withContext(Dispatchers.Default) {
                    realtimeEventSyncService.get()
                }

                serverSyncService.start()
                inAppUpdateManager?.enableUpdateChecks()

                if (savedInstanceState == null) {
                    handleIntent(intent)
                }
            }

            isAuthorized = authorized
        }
        
        setContent {
            val selectedTheme by themeManager.currentTheme.collectAsState()
            val primaryColor by themeManager.appPrimaryColor.collectAsState()
            val isDynamicColorEnable by themeManager.dynamicColor.collectAsState()
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            
            ApplicationTheme(
                theme = selectedTheme,
                dynamicColor = isDynamicColorEnable,
                appPrimaryColor = primaryColor.color
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (isAuthorized) {
                        true -> {
                            val startRoutes = mutableListOf<AppRoute>(AppRoute.Main)

                            startRoute?.let {
                                startRoutes.add(it)
                            }

                            LaunchedEffect(Unit) {
                                startRoute = null
                            }

                            AppNavDisplay(
                                *startRoutes.toTypedArray(),
                                externalRouteFlow = externalRouteFlow
                            )
                        }

                        false -> AppNavDisplay(AppRoute.Login)
                        null -> Unit
                    }

                    val musicPlayerState by globalBottomSheetManager.musicPlayerManager.state.collectAsState()
                    val showMusicPlayerSheet by globalBottomSheetManager.showMusicPlayerSheet.collectAsState()
                    val repeatMode by globalBottomSheetManager.musicPlayerManager.repeatMode.collectAsState()

                    if (showMusicPlayerSheet && musicPlayerState.fileId != null) {
                        MusicPlayerSheet(
                            title = musicPlayerState.title,
                            artist = musicPlayerState.artist,
                            cover = musicPlayerState.artworkData,
                            isPlaying = musicPlayerState.isPlaying,
                            positionMs = musicPlayerState.positionMs,
                            durationMs = musicPlayerState.durationMs,
                            repeatMode = repeatMode,
                            onSeek = globalBottomSheetManager.musicPlayerManager::seekTo,
                            onTogglePlayPause = globalBottomSheetManager.musicPlayerManager::togglePlayPause,
                            onToggleRepeat = globalBottomSheetManager.musicPlayerManager::toggleRepeatMode,
                            onPrevious = globalBottomSheetManager.musicPlayerManager::previous,
                            onNext = globalBottomSheetManager.musicPlayerManager::next,
                            onShare = {},
                            onOpenEqualizer = {
                                globalBottomSheetManager.hideMusicPlayerSheet()
                                externalRouteFlow.tryEmit(AppRoute.Equalizer)
                            },
                            onDismiss = globalBottomSheetManager::hideMusicPlayerSheet
                        )
                    }

                    val showInviteBottomSheet by globalBottomSheetManager.showInviteBottomSheet.collectAsState()
                    val inviteLinkInfo by globalBottomSheetManager.inviteLinkInfo.collectAsState()
                    val isProcessingInvite by globalBottomSheetManager.isProcessingInvite.collectAsState()

                    if (showInviteBottomSheet && inviteLinkInfo != null) {
                        val info = inviteLinkInfo!!
                        InviteLinkBottomSheet(
                            chatId = info.chatId,
                            name = info.name.orEmpty(),
                            description = info.description,
                            count = info.membersCount ?: 0,
                            requireApproval = info.requireApproval,
                            isLoading = isProcessingInvite,
                            onDismiss = globalBottomSheetManager::dismissInviteBottomSheet,
                            onJoin = {
                                globalBottomSheetManager.subscribeViaInviteLink(
                                    scope = scope,
                                    onNavigateToChat = { chatId ->
                                        externalRouteFlow.tryEmit(AppRoute.Chat(chatId, null))
                                    },
                                    onShowSnackbar = { uiText ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(uiText.asString(this@MainActivity))
                                        }
                                    }
                                )
                            }
                        )
                    }

                    val showBannedDialog by globalBottomSheetManager.showBannedDialog.collectAsState()
                    if (showBannedDialog) {
                        AppDialog(
                            title = stringResource(R.string.no_access),
                            onDismissRequest = globalBottomSheetManager::hideBannedDialog,
                            content = { Text(stringResource(R.string.banned_message)) },
                            buttons = {
                                TextButton(onClick = globalBottomSheetManager::hideBannedDialog) {
                                    Text(stringResource(R.string.ok))
                                }
                            }
                        )
                    }
                    
                    if (isUpdateReadyToInstall) {
                        UpdateReadyDialog(
                            onRestart = {
                                isUpdateReadyToInstall = false
                                inAppUpdateManager?.completeUpdate()
                            },
                            onDismiss = { isUpdateReadyToInstall = false }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (isAuthorized != true) {
            return
        }

        handleIntent(intent, isNewIntent = true)
    }

    private fun handleIntent(intent: Intent, isNewIntent: Boolean = false) {
        if (intent.getBooleanExtra("open_music_player", false)) {
            intent.removeExtra("open_music_player")
            if (globalBottomSheetManager.musicPlayerManager.state.value.fileId != null) {
                globalBottomSheetManager.showMusicPlayerSheet()
            }
        }

        val chatId = resolveChatId(intent) ?: return
        
        if (isNewIntent) {
            externalRouteFlow.tryEmit(AppRoute.Chat(chatId, null))
        } else {
            startRoute = AppRoute.Chat(chatId, null)
        }
    }
    
    private fun resolveChatId(intent: Intent): Long? {
        val chatId = intent.getLongExtra(EXTRA_CHAT_ID, UNKNOWN_CHAT_ID)
            .takeIf { it != UNKNOWN_CHAT_ID }
            ?: intent.getStringExtra(EXTRA_CHAT_ID)?.toLongOrNull()
        
        return chatId?.takeIf { it != UNKNOWN_CHAT_ID }
    }
    
    private fun restartApp() {
        runOnUiThread {
            val intent = Intent(
                this,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
    
    companion object {
        const val EXTRA_CHAT_ID = "chatId"
        
        private const val UNKNOWN_CHAT_ID = -1L
    }
}

@Composable
private fun UpdateReadyDialog(
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.update_downloaded_title),
        content = {
            Text(stringResource(R.string.update_downloaded_message))
        },
        buttons = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
                TextButton(onClick = onRestart) {
                    Text(stringResource(R.string.update_restart))
                }
            }
        }
    )
}
