/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aiwazian.messenger.analytics.AnalyticsTracker
import com.aiwazian.messenger.ui.components.navigation.AppNavDisplay
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.theme.ApplicationTheme
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        /*
         * Сессия может закончиться в любой момент: её отключили с другого
         * устройства или сервер перестал принимать токен. Пока на устройстве
         * остаётся другой аккаунт, приложение просто перезапускается под ним, и
         * экран авторизации не показывается.
         */
        SessionManager.setSessionEndCallback { resolution ->
            when (resolution) {
                is SessionEndResolution.SwitchedToAccount -> restartWithNextAccount(resolution.userId)
                is SessionEndResolution.NoAccountsLeft -> openAuthScreen()
            }
        }
        
        val currentUserId = runBlocking {
            SessionManager.loadSession()
            SessionManager.getCurrentUserId()
        }
        val hasSession = currentUserId != null
        AnalyticsTracker.setCurrentUser(currentUserId)
        
        if (!hasSession) {
            openAuthScreen()
            return
        }
        
        installSplashScreen().setKeepOnScreenCondition {
            false
        }
        
        enableEdgeToEdge()
        
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        
        AnalyticsTracker.trackMainScreenOpen()
        
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
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        AnalyticsTracker.startSession(this, isAuthorized = true)
    }
    
    override fun onStop() {
        AnalyticsTracker.endSession(this)
        super.onStop()
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
    
    /**
     * Перезапуск под новым аккаунтом: экраны и view-модели прошлого аккаунта
     * держат его данные, поэтому задача пересобирается с нуля.
     */
    private fun restartWithNextAccount(userId: Long) {
        AnalyticsTracker.setCurrentUser(userId)
        restartTask(MainActivity::class.java)
    }
    
    private fun openAuthScreen() {
        AnalyticsTracker.setCurrentUser(null)
        restartTask(AuthActivity::class.java)
    }
    
    /**
     * Решение о завершении сессии приходит из фонового потока, а работать с
     * активити можно только на главном.
     */
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
