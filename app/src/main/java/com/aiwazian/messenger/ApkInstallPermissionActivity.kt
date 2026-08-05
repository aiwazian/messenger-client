/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.ApkInstaller
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Asks for the permission to install applications.
 *
 * The settings of the unknown app sources are opened right away. When the user
 * comes back without the permission, a bottom sheet offers to open them again,
 * and when the permission is granted the installation is repeated.
 */
@AndroidEntryPoint
class ApkInstallPermissionActivity : ComponentActivity() {
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var apkInstaller: ApkInstaller
    
    private var isSheetVisible by mutableStateOf(false)
    
    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onSettingsClosed()
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val selectedTheme by themeManager.currentTheme.collectAsState()
            val primaryColor by themeManager.appPrimaryColor.collectAsState()
            val isDynamicColorEnable by themeManager.dynamicColor.collectAsState()
            
            ApplicationTheme(
                theme = selectedTheme,
                dynamicColor = isDynamicColorEnable,
                appPrimaryColor = primaryColor.color
            ) {
                if (isSheetVisible) {
                    InstallPermissionBottomSheet(
                        onOpenSettings = ::openSettings, onDismiss = ::finish
                    )
                }
            }
        }
        
        if (savedInstanceState == null) {
            openSettings()
        }
    }
    
    private fun openSettings() {
        isSheetVisible = false
        
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri()
        )
        
        try {
            settingsLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open the unknown app sources settings", e)
            finish()
        }
    }
    
    private fun onSettingsClosed() {
        if (!apkInstaller.canInstallPackages()) {
            isSheetVisible = true
            return
        }
        
        val path = intent.getStringExtra(EXTRA_APK_PATH)
        
        if (path.isNullOrBlank()) {
            finish()
            return
        }
        
        lifecycleScope.launch {
            apkInstaller.install(File(path))
            finish()
        }
    }
    
    companion object {
        const val EXTRA_APK_PATH = "apk_path"
        
        private const val TAG = "ApkInstallPermissionActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallPermissionBottomSheet(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Rounded.Android,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
            
            Text(PERMISSION_MESSAGE)
            
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSettings,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

private const val PERMISSION_MESSAGE = "Allow installing unknown apps to continue"
