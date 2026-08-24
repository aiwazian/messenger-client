/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.aiwazian.messenger.extensions.findActivity

@Composable
fun SecureScreenEffect(enabled: Boolean) {
    val view = LocalView.current
    
    DisposableEffect(view, enabled) {
        if (!enabled || view.isInEditMode) {
            return@DisposableEffect onDispose { }
        }
        
        val window = view.context.findActivity()?.window
            ?: return@DisposableEffect onDispose { }
        
        SecureScreenFlag.acquire(window)
        
        onDispose {
            SecureScreenFlag.release(window)
        }
    }
}

private object SecureScreenFlag {
    private var holders = 0
    
    fun acquire(window: Window) {
        holders++
        if (holders == 1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    
    fun release(window: Window) {
        holders = (holders - 1).coerceAtLeast(0)
        if (holders == 0) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
