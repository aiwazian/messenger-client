/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun ApplicationTheme(
    theme: ThemeOption = ThemeOption.SYSTEM,
    appPrimaryColor: Color = AppPrimaryColor.Blue.color,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val isDark = when (theme) {
        ThemeOption.DARK -> true
        ThemeOption.LIGHT -> false
        ThemeOption.SYSTEM -> isSystemInDarkTheme()
    }
    
    DisposableEffect(isDark, view) {
        val window = (view.context as Activity).window
        
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
        
        onDispose {}
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        else -> rememberDynamicColorScheme(
            seedColor = appPrimaryColor,
            isDark = isDark,
            style = PaletteStyle.TonalSpot,
        )
    }
    
    MaterialExpressiveTheme(
        motionScheme = MotionScheme.expressive(), colorScheme = colorScheme, content = content
    )
}
