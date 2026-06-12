/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.ui.components.CodeBlocks
import com.aiwazian.messenger.ui.components.CustomNumberBoard
import com.aiwazian.messenger.ui.screens.settings.security.passcode.PasscodeViewModel
import com.aiwazian.messenger.utils.BiometricHelper

@Composable
fun LockScreen(lockViewModel: LockViewModel = hiltViewModel()) {
    val uiState by lockViewModel.uiState.collectAsState()
    val fingerprintEnabled by lockViewModel.fingerprintEnabled.collectAsState()
    
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricHelper = remember(activity) { activity?.let { BiometricHelper(it) } }
    
    val showFingerprint = fingerprintEnabled && uiState.passcode.isEmpty()
    
    LaunchedEffect(fingerprintEnabled) {
        if (fingerprintEnabled && biometricHelper?.canAuthenticate() == true) {
            biometricHelper.authenticate(
                onSuccess = { lockViewModel.onFingerprintSuccess() }
            )
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(80.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Lock",
                        modifier = Modifier.size(40.dp),
                    )

                    if (uiState.remainingSeconds > 0) {
                        Text(
                            text = "Попробуйте снова через ${uiState.remainingSeconds} сек.",
                            fontSize = 16.sp
                        )
                    }
                }
                
                CodeBlocks(
                    count = PasscodeViewModel.MAX_LENGTH_PASSCODE,
                    showInput = false,
                    code = uiState.passcode
                )
                
                val boardButtons = listOf(
                    listOf(
                        "1",
                        "2",
                        "3"
                    ),
                    listOf(
                        "4",
                        "5",
                        "6"
                    ),
                    listOf(
                        "7",
                        "8",
                        "9"
                    ),
                    listOf(
                        null,
                        "0",
                        if (showFingerprint) Icons.Filled.Fingerprint
                        else Icons.AutoMirrored.Outlined.Backspace
                    ),
                )
                
                val bottomRightIcon: ImageVector = if (showFingerprint) {
                    Icons.Rounded.Fingerprint
                } else {
                    Icons.AutoMirrored.Rounded.Backspace
                }
                
                CustomNumberBoard(
                    value = uiState.passcode,
                    buttons = boardButtons,
                    onChange = lockViewModel::onPasscodeChanged,
                    bottomRightIcon = bottomRightIcon,
                    onBottomRightClick = if (showFingerprint && biometricHelper?.canAuthenticate() == true) {
                        {
                            biometricHelper.authenticate(
                                onSuccess = { lockViewModel.onFingerprintSuccess() }
                            )
                        }
                    } else null
                )
            }
        }
    }
}
