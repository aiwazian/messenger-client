/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.rememberNavBackStack
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.settings.storage.StorageScreen
import com.aiwazian.messenger.ui.theme.ApplicationTheme
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManageSpaceActivity : AppCompatActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                CompositionLocalProvider(
                    LocalNavBackStack provides rememberNavBackStack()
                ) {
                    StorageScreen(
                        onBackClick = ::finish
                    )
                }
            }
        }
    }
}
