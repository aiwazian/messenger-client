/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.ui.components.CodeInputBlocks
import com.aiwazian.messenger.ui.components.NumberKeyboard
import com.aiwazian.messenger.ui.screens.settings.security.passcode.PasscodeViewModel
import com.aiwazian.messenger.utils.BiometricHelper

@Composable
fun LockScreen(lockViewModel: LockViewModel = hiltViewModel()) {
    val uiState by lockViewModel.uiState.collectAsState()
    val fingerprintEnabled by lockViewModel.fingerprintEnabled.collectAsState()
    
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricHelper = remember(activity) { activity?.let { BiometricHelper(it) } }
    
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock",
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = if (uiState.remainingSeconds > 0) "Попробуйте снова через ${uiState.remainingSeconds} сек." else "",
                    fontSize = 16.sp
                )
                
                Spacer(Modifier.height(16.dp))
                
                CodeInputBlocks(
                    value = uiState.passcode,
                    length = PasscodeViewModel.MAX_LENGTH_PASSCODE,
                    status = uiState.status,
                    onStatusShown = lockViewModel::onStatusShown,
                )
            }
            
            NumberKeyboard(
                value = uiState.passcode,
                onValueChange = lockViewModel::onPasscodeChanged,
                rightIcon = if (fingerprintEnabled && uiState.passcode.isEmpty()) Icons.Rounded.Fingerprint else Icons.AutoMirrored.Outlined.Backspace,
                onRightClick = if (fingerprintEnabled && uiState.passcode.isEmpty()) {
                    {
                        if (biometricHelper?.canAuthenticate() == true) {
                            biometricHelper.authenticate(
                                onSuccess = {
                                    lockViewModel.onFingerprintSuccess()
                                }
                            )
                        }
                    }
                } else null
            )
        }
    }
}
