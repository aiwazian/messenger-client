package com.aiwazian.messenger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.database.repository.UserRepository
import com.aiwazian.messenger.services.AppLockService
import com.aiwazian.messenger.services.DataStoreManager
import com.aiwazian.messenger.services.LanguageService
import com.aiwazian.messenger.services.NotificationService
import com.aiwazian.messenger.services.ThemeService
import com.aiwazian.messenger.services.TokenManager
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.ChatScreen
import com.aiwazian.messenger.ui.LockScreen
import com.aiwazian.messenger.ui.MainScreen
import com.aiwazian.messenger.ui.element.NavigationController
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.ChatState
import com.aiwazian.messenger.utils.WebSocketManager
import com.aiwazian.messenger.viewModels.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var appLockService: AppLockService
    
    @Inject
    lateinit var themeService: ThemeService
    
    @Inject
    lateinit var userRepository: UserRepository
    
    lateinit var navViewModel: NavigationViewModel
    
    override fun attachBaseContext(newBase: Context) {
        DataStoreManager.initialize(newBase)
        val dataStoreManager = DataStoreManager.getInstance()
        
        val savedLanguageCode = runBlocking {
            TokenManager.init()
            dataStoreManager.getLanguage().first().lowercase()
        }
        
        val languageService = LanguageService(newBase)
        val context = languageService.selLanguage(savedLanguageCode)
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        TokenManager.setUnauthorizedCallback {
            val intent = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            this@MainActivity.startActivity(intent)
            this@MainActivity.finish()
        }
        
        setContent {
            val isLockApp by appLockService.isLockApp.collectAsState()
            val selectedTheme by themeService.currentTheme.collectAsState()
            val selectedColor by themeService.primaryColor.collectAsState()
            val isDynamicColorEnable by themeService.dynamicColor.collectAsState()
            
            LaunchedEffect(Unit) {
                try {
                    WebSocketManager.onConnect = {
                        lifecycleScope.launch {
                            UserManager.loadUserData(userRepository)
                        }
                    }
                    
                    WebSocketManager.onClose = { code ->
                        if (code == 1008) {
                            TokenManager.getUnauthorizedCallback()?.invoke()
                        } else {
                            lifecycleScope.launch {
                                delay(1000)
                                WebSocketManager.connect()
                            }
                        }
                    }
                    
                    WebSocketManager.onFailure = {
                        lifecycleScope.launch {
                            delay(1000)
                            WebSocketManager.connect()
                        }
                    }
                    
                    WebSocketManager.connect()
                    
                    UserManager.loadUserData(userRepository)
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
                navViewModel = viewModel<NavigationViewModel>()
                
                NavigationController {
                    MainScreen()
                }
                
                AnimatedVisibility(
                    visible = isLockApp,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut(tween(100))
                ) {
                    LockScreen()
                }
                
                LaunchedEffect(Unit) {
                    val chatId = intent.getStringExtra("chatId")?.toLongOrNull()

                    if (chatId != null && !ChatState.isChatOpen(chatId)) {
                        navViewModel.addScreenInStack {
                            ChatScreen(chatId)
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        val chatId = intent.getStringExtra("chatId")?.toLongOrNull()
        
        if (chatId != null) {
            if (!ChatState.isChatOpen(chatId)) {
                navViewModel.addScreenInStack {
                    ChatScreen(chatId)
                }
            }
        }
    }
}
