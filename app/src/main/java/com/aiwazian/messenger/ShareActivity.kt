/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aiwazian.messenger.ui.screens.share.ShareScreen
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ShareActivity : AppCompatActivity() {
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedText = extractSharedText(intent)
        val sharedFiles = extractSharedFiles(intent)
        
        // Система может прислать текст, файлы или то и другое сразу — закрываемся
        // только если отправлять вообще нечего.
        if (sharedText.isNullOrBlank() && sharedFiles.isEmpty()) {
            finish()
            return
        }
        
        val hasSession = runBlocking {
            SessionManager.loadSession()
            SessionManager.hasAnySession()
        }
        
        if (!hasSession) {
            startActivity(Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }
        
        enableEdgeToEdge()
        
        setContent {
            val selectedTheme by themeManager.currentTheme.collectAsState()
            val primaryColor by themeManager.appPrimaryColor.collectAsState()
            val isDynamicColorEnable by themeManager.dynamicColor.collectAsState()
            
            ApplicationTheme(
                theme = selectedTheme,
                appPrimaryColor = primaryColor.color,
                dynamicColor = isDynamicColorEnable
            ) {
                ShareScreen(
                    sharedText = sharedText,
                    sharedFiles = sharedFiles,
                    onClose = { finish() }
                )
            }
        }
    }
    
    private fun extractSharedText(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND) {
            return null
        }
        
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }
    
    /**
     * ACTION_SEND приносит одну ссылку, ACTION_SEND_MULTIPLE — список.
     *
     * Типизированные версии getParcelableExtra появились только в API 33, а
     * IntentCompat — в свежих версиях androidx.core, поэтому здесь осознанно
     * используются deprecated-перегрузки: они работают на всех версиях.
     */
    @Suppress("DEPRECATION")
    private fun extractSharedFiles(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        
        Intent.ACTION_SEND_MULTIPLE ->
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        
        else -> emptyList()
    }
}
