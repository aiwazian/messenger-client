/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Запрещает скриншоты и запись экрана, пока экран находится в композиции.
 *
 * Флаг [WindowManager.LayoutParams.FLAG_SECURE] — единственный штатный способ
 * Android: скриншот даёт чёрный кадр, запись экрана и превью в списке
 * недавних тоже пустые.
 *
 * Отличия от варианта с `context as? Activity`:
 * - Activity ищется через цепочку [ContextWrapper]: в Compose контекст часто
 *   обёрнут (тема, диалог, превью), и прямое приведение типа молча даёт null,
 *   и защита не включается;
 * - флаг считается по числу владельцев, поэтому выход из одного защищённого
 *   экрана не снимает защиту с другого, который всё ещё открыт;
 * - при `enabled = false` ничего не делается, так что флаг включается и
 *   выключается на лету вслед за настройкой чата.
 */
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

/**
 * Счётчик владельцев флага: флаг у окна один, а защищённых экранов одновременно
 * может быть несколько (например, чат поверх другого чата).
 */
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

/** В Compose контекст почти всегда обёрнут, поэтому идём по цепочке до Activity. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
