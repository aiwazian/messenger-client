/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
    var current: Context? = this
    
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    
    return null
}
