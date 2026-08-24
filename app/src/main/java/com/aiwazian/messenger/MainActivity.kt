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
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.components.navigation.AppNavDisplay
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.InAppUpdateManager
import com.aiwazian.messenger.utils.SessionEndResolution
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
    
    private var startRoute by mutableStateOf<AppRoute?>(null)
    private val externalRouteFlow = MutableSharedFlow<AppRoute>(extraBufferCapacity = 1)
    
    private var inAppUpdateManager: InAppUpdateManager? = null
    private var isUpdateReadyToInstall by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        SessionManager.setSessionEndCallback { resolution ->
            when (resolution) {
                is SessionEndResolution.SwitchedToAccount -> restartWithNextAccount()
                is SessionEndResolution.NoAccountsLeft -> openAuthScreen()
            }
        }
        
        val hasSession = runBlocking {
            SessionManager.loadSession()
            SessionManager.hasAnySession()
        }
        
        if (!hasSession) {
            openAuthScreen()
            return
        }
        
        installSplashScreen().setKeepOnScreenCondition {
            false
        }
        
        enableEdgeToEdge()
        
        inAppUpdateManager = InAppUpdateManager(this) {
            isUpdateReadyToInstall = true
        }
        
        if (savedInstanceState == null) {
            handleIntent(intent)
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
                val startRoutes = mutableListOf<AppRoute>(AppRoute.Main)
                
                startRoute?.let {
                    startRoutes.add(it)
                    startRoute = null
                }
                
                AppNavDisplay(
                    *startRoutes.toTypedArray(),
                    externalRouteFlow = externalRouteFlow
                )
                
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
        val chatId = intent.getLongExtra("chatId", -1)
        if (chatId != -1L) {
            if (isNewIntent) {
                externalRouteFlow.tryEmit(AppRoute.Chat(chatId, null))
            } else {
                startRoute = AppRoute.Chat(chatId, null)
            }
            return
        }
    }
    
    private fun restartWithNextAccount() {
        restartTask(MainActivity::class.java)
    }
    
    private fun openAuthScreen() {
        restartTask(AuthActivity::class.java)
    }
    
    private fun restartTask(target: Class<*>) {
        runOnUiThread {
            val intent = Intent(
                this,
                target
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
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
