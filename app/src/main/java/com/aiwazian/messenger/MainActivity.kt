/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aiwazian.messenger.ui.components.navigation.AppNavHost
import com.aiwazian.messenger.ui.screens.lock.LockScreen
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.utils.NotificationService
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import com.aiwazian.messenger.socket.WebSocketClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var appLockManager: AppLockManager
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var webSocketClient: WebSocketClient
    
    private var startRoute by mutableStateOf<AppRoute>(AppRoute.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent =
            Intent(
                this,
                AuthActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        
        val hasSession = runBlocking {
            SessionManager.loadSession()
            SessionManager.hasAnySession()
        }
        
        if (!hasSession) {
            startActivity(intent)
            finish()
            return
        }
        
        installSplashScreen().setKeepOnScreenCondition {
            false
        }
        
        enableEdgeToEdge()
        
        SessionManager.setUnauthorizedCallback {
            val intent = Intent(
                this,
                AuthActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            this@MainActivity.startActivity(intent)
            this@MainActivity.finish()
        }
        
        val chatId =
            intent.getLongExtra(
                "chatId",
                -1L
            )
        if (chatId != -1L) {
            startRoute = AppRoute.Chat(chatId)
        }
        
        setContent {
            val isLockApp by appLockManager.isLockApp.collectAsState()
            val selectedTheme by themeManager.currentTheme.collectAsState()
            val selectedColor by themeManager.primaryColor.collectAsState()
            val isDynamicColorEnable by themeManager.dynamicColor.collectAsState()
            
            LaunchedEffect(Unit) {
                try {
                    webSocketClient.connectWithLifecycle(
                        BuildConfig.WS_URL,
                        this@MainActivity
                    )
                    
                    webSocketClient.setOnDisconnectedCallback { code, _ ->
                        if (code == 1008) {
                            SessionManager.getUnauthorizedCallback()?.invoke()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "MainActivity",
                        "Ошибка подключения вебсокета",
                        e
                    )
                }
                
                try {
                    val notificationService = NotificationService()
                    val token = notificationService.getFirebaseToken()
                    notificationService.sendTokenToServer(token)
                } catch (e: Exception) {
                    Log.e(
                        "MainActivity",
                        "Ошибка при отправке токена для уведомлений на сервер",
                        e
                    )
                }
            }
            
            ApplicationTheme(
                theme = selectedTheme,
                dynamicColor = isDynamicColorEnable,
                primaryColor = selectedColor.color
            ) {
                AppNavHost(startRoute = startRoute)
                
                AnimatedVisibility(
                    visible = isLockApp,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut(tween(100))
                ) {
                    LockScreen()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}
