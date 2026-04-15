/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aiwazian.messenger.ui.components.navigation.AppNavDisplay
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        installSplashScreen()
        
        enableEdgeToEdge()
        
        setContent {
            ApplicationTheme {
                AppNavDisplay(startRoute = AppRoute.Login)
            }
        }
    }
}
