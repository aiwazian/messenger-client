/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.SessionManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrivacyScreen(privacyViewModel: SettingsPrivacyViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    
    val privacy by privacyViewModel.privacySettings.collectAsState()
    
    val scrollState = rememberScrollState()
    
    var showDeleteBottomSheet by remember { mutableStateOf(false) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        privacyViewModel.loadValues()
        privacyViewModel.deleteSuccess.collect {
            SessionManager.getUnauthorizedCallback()?.invoke()
        }
    }
    
    Scaffold(
        topBar = { TopBar() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionContainer(header = {
                SectionHeader(stringResource(R.string.confidentiality))
            }) {
                SectionItem(
                    text = stringResource(R.string.bio),
                    primaryText = if (privacy.bio == PrivacyLevel.Everybody) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navHost.add(AppRoute.SettingsBio(privacy.bio))
                    })
                
                SectionItem(
                    text = stringResource(R.string.date_of_birth),
                    primaryText = if (privacy.dateOfBirth == PrivacyLevel.Everybody) {
                        stringResource(R.string.everybody)
                    } else {
                        stringResource(R.string.nobody)
                    },
                    onClick = {
                        navHost.add(AppRoute.SettingsDateOfBirth(privacy.dateOfBirth))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.delete_account),
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showDeleteBottomSheet = true
                    })
            }
        }
    }
    
    if (showDeleteBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showDeleteBottomSheet = false
            },
            dragHandle = null
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = "Всё, что связанно с вашим аккаунтом будет безвозвратно удалено.",
                        lineHeight = 18.sp
                    )
                }
                
                var waitSeconds by remember { mutableIntStateOf(10) }
                
                TextButton(
                    onClick = {
                        if (waitSeconds <= 0) {
                            showDeleteDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Все равно удалить",
                            fontSize = 16.sp
                        )
                        
                        LaunchedEffect(Unit) {
                            while (waitSeconds > 0) {
                                delay(1000)
                                waitSeconds--
                            }
                        }
                        
                        AnimatedContent(
                            targetState = waitSeconds,
                            transitionSpec = {
                                slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                            }) { second ->
                            if (second > 0) {
                                Text(
                                    text = " $second",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        CustomDialog(
            title = stringResource(R.string.delete_account),
            onDismissRequest = {
                showDeleteDialog = false
            },
            content = {
                Text(
                    text = stringResource(R.string.delete_account_confirm),
                    lineHeight = 18.sp
                )
            },
            buttons = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.no))
                }
                
                var waitSeconds by remember { mutableIntStateOf(10) }
                LaunchedEffect(Unit) {
                    while (waitSeconds > 0) {
                        delay(1000)
                        waitSeconds--
                    }
                }
                TextButton(
                    onClick = {
                        if (waitSeconds <= 0) {
                            showDeleteDialog = false
                            privacyViewModel.deleteAccount()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.yes))
                    AnimatedContent(
                        targetState = waitSeconds,
                        transitionSpec = {
                            slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                        }) { second ->
                        if (second > 0) {
                            Text(
                                text = " $second",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            })
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    
    PageTopBar(
        title = { Text(stringResource(R.string.confidentiality)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}
