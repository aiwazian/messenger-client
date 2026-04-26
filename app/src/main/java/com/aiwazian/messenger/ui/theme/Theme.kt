/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption

private fun darkColorSchemeMaterial(customPrimaryColor: Color) =
    darkColorScheme(
        primary = customPrimaryColor,
        primaryContainer = customPrimaryColor.copy(
            red = customPrimaryColor.red - 0.2f,
            green = customPrimaryColor.green - 0.2f,
            blue = customPrimaryColor.blue - 0.2f
        )
    )

private fun lightColorSchemeMaterial(customPrimaryColor: Color) =
    lightColorScheme(
        primary = customPrimaryColor,
        primaryContainer = customPrimaryColor.copy(
            red = customPrimaryColor.red - 0.2f,
            green = customPrimaryColor.green - 0.2f,
            blue = customPrimaryColor.blue - 0.2f
        )
    )

@Composable
fun ApplicationTheme(
    theme: ThemeOption = ThemeOption.SYSTEM,
    appPrimaryColor: Color = AppPrimaryColor.Blue.color,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = when (theme) {
        ThemeOption.DARK -> true
        ThemeOption.LIGHT -> false
        ThemeOption.SYSTEM -> isSystemInDarkTheme()
    }
    
    val view = LocalView.current
    val activity = view.context as Activity
    
    SideEffect {
        val window = activity.window
        
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        
        val insetsController = WindowCompat.getInsetsController(
            window,
            view
        )
        
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        isDark -> darkColorSchemeMaterial(appPrimaryColor)
        else -> lightColorSchemeMaterial(appPrimaryColor)
    }
    
    MaterialExpressiveTheme(
        motionScheme = MotionScheme.expressive(),
        colorScheme = colorScheme,
        content = content
    )
}
