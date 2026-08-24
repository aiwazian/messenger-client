/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Достаёт Activity из контекста, разворачивая цепочку ContextWrapper.
 *
 * Контекст из LocalContext и у View почти всегда обёрнут: тема, диалог и окно
 * добавляют свой ContextThemeWrapper, поэтому приведение напрямую к Activity
 * падает. Возвращает null, если Activity в цепочке нет — так бывает в превью
 * и у контекста приложения.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    
    return null
}
