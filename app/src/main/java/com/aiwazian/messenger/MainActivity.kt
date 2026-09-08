/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aiwazian.messenger.socket.ServerSyncService
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.components.navigation.AppNavDisplay
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.InAppUpdateManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var serverSyncService: ServerSyncService
    
    private var startRoute by mutableStateOf<AppRoute?>(null)
    private val externalRouteFlow = MutableSharedFlow<AppRoute>(extraBufferCapacity = 1)
    
    private var inAppUpdateManager: InAppUpdateManager? = null
    private var isUpdateReadyToInstall by mutableStateOf(false)
    
    private var isAuthorized = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        SessionManager.setSessionEndCallback {
            restartApp()
        }
        
        isAuthorized = runBlocking {
            SessionManager.loadSession()
            SessionManager.hasAnySession()
        }
        
        installSplashScreen().setKeepOnScreenCondition {
            false
        }
        
        enableEdgeToEdge()
        
        if (isAuthorized) {
            serverSyncService.start()
            
            inAppUpdateManager = InAppUpdateManager(this) {
                isUpdateReadyToInstall = true
            }
            
            if (savedInstanceState == null) {
                handleIntent(intent)
            }
        }
        
        setContent {
            val selectedTheme by themeManager.currentTheme.collectAsState()
            val primaryColor by themeManager.appPrimaryColor.collectAsState()
            val isDynamicColorEnable by themeManager.dynamicColor.collectAsState()
            
            ApplicationTheme(
                theme = selectedTheme,
                dynamicColor = isDynamicColorEnable,
                appPrimaryColor = primaryColor.color
            ) {
                if (isAuthorized) {
                    val startRoutes = mutableListOf<AppRoute>(AppRoute.Main)
                    
                    startRoute?.let {
                        startRoutes.add(it)
                        startRoute = null
                    }
                    
                    AppNavDisplay(
                        *startRoutes.toTypedArray(),
                        externalRouteFlow = externalRouteFlow
                    )
                } else {
                    AppNavDisplay(AppRoute.Login)
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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, isNewIntent = true)
    }
    
    private fun handleIntent(intent: Intent, isNewIntent: Boolean = false) {
        if (!isAuthorized) {
            return
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
