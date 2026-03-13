package com.aiwazian.messenger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.CodeBlocks
import com.aiwazian.messenger.ui.element.CustomNumberBoard
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.LockScreenViewModel
import com.aiwazian.messenger.viewModels.PasscodeLockViewModel

@Composable
fun LockScreen() {
    Content()
}

@Composable
private fun Content() {
    val context = LocalContext.current
    
    val lockScreenViewModel = hiltViewModel<LockScreenViewModel>()
    
    lockScreenViewModel.onWrongPasscode = {
        val vibrateService = VibrateService(context)
        vibrateService.vibrate(VibrationPattern.Error)
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
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock",
                    modifier = Modifier.size(40.dp),
                )
                
                CodeBlocks(
                    count = PasscodeLockViewModel.MAX_LENGTH_PASSCODE,
                    showInput = false,
                    code = lockScreenViewModel.passcode
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
                        Icons.AutoMirrored.Outlined.Backspace
                    ),
                )
                
                CustomNumberBoard(
                    value = lockScreenViewModel.passcode,
                    buttons = boardButtons,
                    onChange = lockScreenViewModel::onPasscodeChanged
                )
            }
        }
    }
}
