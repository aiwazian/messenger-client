/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    fun copy(text: String) {
        val clipData = ClipData.newPlainText(
            "label",
            text
        )
        
        clipboardManager.setPrimaryClip(clipData)
    }
}
