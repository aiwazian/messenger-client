package com.aiwazian.messenger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aiwazian.messenger.enums.PrimaryColorOption
import com.aiwazian.messenger.enums.ThemeOption

private fun darkColorSchemeMaterial(customPrimaryColor: Color) = darkColorScheme(
    primary = customPrimaryColor,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    error = Color(0xFFFF6464),
)

private fun lightColorSchemeMaterial(customPrimaryColor: Color) = lightColorScheme(
    primary = customPrimaryColor,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    error = Color(0xFFFF6464),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApplicationTheme(
    theme: ThemeOption = ThemeOption.SYSTEM,
    primaryColor: Color = PrimaryColorOption.Blue.color,
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
        
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        isDark -> darkColorSchemeMaterial(primaryColor)
        else -> lightColorSchemeMaterial(primaryColor)
    }
    
    MaterialExpressiveTheme(
        motionScheme = MotionScheme.expressive(),
        colorScheme = colorScheme,
        content = content
    )
}