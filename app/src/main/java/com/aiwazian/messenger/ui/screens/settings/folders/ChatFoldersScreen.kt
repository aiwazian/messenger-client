/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.LottieAnimation

@Composable
fun ChatFoldersScreen(viewModel: ChatFoldersViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(topBar = {
        PageTopBar(
            title = { Text(stringResource(R.string.chat_folders)) },
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = navBackStack::removeLastOrNull
            )
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val composition by rememberLottieComposition(
                    spec = LottieCompositionSpec.Asset(LottieAnimation.FOLDER)
                )
                
                LottieAnimation(
                    composition = composition,
                    modifier = Modifier.size(100.dp),
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )
                
                Text(
                    text = stringResource(R.string.chat_folders_description),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.Add,
                    headlineText = stringResource(R.string.create_new_folder),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        navBackStack.add(AppRoute.ChatFolderEditor())
                    })
                
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    content = { Text(stringResource(R.string.all_chats)) })
                
                uiState.folders.forEach { folder ->
                    ChatFolderRow(
                        folder = folder,
                        onEditClick = {
                            navBackStack.add(AppRoute.ChatFolderEditor(folder.id))
                        },
                        onDeleteClick = { viewModel.requestFolderDeletion(folder) })
                }
            }
        }
    }
    
    uiState.folderPendingDeletion?.let {
        AppDialog(
            title = stringResource(R.string.remove_folder),
            onDismissRequest = viewModel::cancelFolderDeletion,
            content = {
                Text("Это не затронет чаты, которые в ней находятся.")
            },
            buttons = {
                TextButton(onClick = viewModel::cancelFolderDeletion) {
                    Text(stringResource(R.string.cancel))
                }
                
                TextButton(
                    onClick = viewModel::confirmFolderDeletion,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            })
    }
}

@Composable
private fun ChatFolderRow(
    folder: ChatFolder, onEditClick: () -> Unit, onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ListItem(
        modifier = Modifier.clickable(onClick = onEditClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        content = { Text(folder.name) },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
                }
                
                AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = { Text(stringResource(R.string.edit_folder)) },
                        onClick = {
                            expanded = false
                            onEditClick()
                        })
                    
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            expanded = false
                            onDeleteClick()
                        })
                }
            }
        })
}
