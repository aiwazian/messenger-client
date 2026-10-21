/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {
    private val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    
    fun copy(text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("label", text))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }
    }
}
