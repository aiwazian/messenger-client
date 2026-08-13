/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        
        /*
         * Сессия может закончиться в любой момент: её отключили с другого
         * устройства или сервер перестал принимать токен. Пока на устройстве
         * остаётся другой аккаунт, приложение просто перезапускается под ним, и
         * экран авторизации не показывается.
         */
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
        
        /*
         * Проверка обновления привязана к жизненному циклу активити и создаётся
         * здесь, а не выше: пользователю без сессии показывается экран входа, и
         * дёргать Play в этот момент незачем.
         */
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
    
    /**
     * Перезапуск под новым аккаунтом: экраны и view-модели прошлого аккаунта
     * держат его данные, поэтому задача пересобирается с нуля.
     */
    private fun restartWithNextAccount() {
        restartTask(MainActivity::class.java)
    }
    
    private fun openAuthScreen() {
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

/**
 * Обновление скачано, но ставится только после перезапуска. Диалог закрывается
 * кнопкой «Позже» — приложение продолжает работать на старой версии, а пакет
 * дождётся следующего раза.
 */
@Composable
private fun UpdateReadyDialog(
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_downloaded_title)) },
        text = { Text(stringResource(R.string.update_downloaded_message)) },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text(stringResource(R.string.update_restart))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}
