/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.flow.collectLatest
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun StorageScreen(storageViewModel: StorageViewModel = hiltViewModel()) {
    val uiState by storageViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        storageViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is StorageUiEvent.CacheCleared -> {}
                is StorageUiEvent.DatabaseCleared -> {}
                is StorageUiEvent.Error -> {}
            }
        }
    }
    
    val sizeBytes = storageViewModel.appSize
    val sizeMb = sizeBytes / (1024.0 * 1024.0)
    val sizeMbRounded = BigDecimal(sizeMb).setScale(
        2, RoundingMode.HALF_UP
    ).toDouble()
    
    Scaffold(
        topBar = { TopBar(storageViewModel) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.storage_usage),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W500
                )
                Text(
                    text = "$sizeMbRounded MB",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SectionContainer(footer = {
                SectionDescription("Все медиа останутся в облаке, при необходимости Вы сможете загрузить их.")
            }) {
                uiState.categories.forEachIndexed { index, category ->
                    val sizeMb = category.totalSize / (1024.0 * 1024.0)
                    val sizeMbRounded = BigDecimal(sizeMb).setScale(
                        2, RoundingMode.HALF_UP
                    ).toDouble()
                    
                    StorageCategory(
                        text = stringResource(category.category.title),
                        selected = category.isSelected,
                        color = AppPrimaryColor.entries[index].color,
                        primaryText = "$sizeMbRounded MB",
                        onClick = {
                            storageViewModel.toggleCategory(category.category)
                        },
                    )
                }
                
                val selectedSize = uiState.selectedSize
                val selectedSizeMb = selectedSize / (1024.0 * 1024.0)
                val selectedSizeMbRounded = BigDecimal(selectedSizeMb).setScale(
                    2, RoundingMode.HALF_UP
                ).toDouble()
                
                val hasSelection = uiState.selectedCategories.isNotEmpty()
                
                Button(
                    onClick = storageViewModel::showConfirmDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = hasSelection
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.clear_cache)} (",
                            fontSize = 16.sp,
                            lineHeight = 18.sp
                        )
                        AnimatedContent(
                            targetState = selectedSizeMbRounded, transitionSpec = {
                                if (targetState > initialState) {
                                    slideInVertically { -it } + fadeIn() + scaleIn() togetherWith slideOutVertically { it } + fadeOut() + scaleOut()
                                } else {
                                    slideInVertically { it } + fadeIn() + scaleIn() togetherWith slideOutVertically { -it } + fadeOut() + scaleOut()
                                }
                            }) { size ->
                            Text(
                                text = "$size", fontSize = 16.sp, lineHeight = 18.sp
                            )
                        }
                        Text(
                            text = "MB)", fontSize = 16.sp, lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
    
    if (uiState.showConfirmDialog) {
        ClearCacheConfirmationDialog(
            onConfirm = {
                storageViewModel.clearSelectedCache()
            }, onDismiss = storageViewModel::hideConfirmDialog
        )
    }
    
    if (uiState.showClearDatabaseDialog) {
        ClearDatabaseConfirmationDialog(
            onConfirm = {
                storageViewModel.clearDatabase()
                storageViewModel.hideClearDatabaseDialog()
            }, onDismiss = storageViewModel::hideClearDatabaseDialog
        )
    }
}

@Composable
private fun StorageCategory(
    onClick: () -> Unit, selected: Boolean, color: Color, text: String, primaryText: String
) {
    TextButton(
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.padding(end = 16.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = color
                )
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                    contentAlignment = Alignment.Center
                ) { selected ->
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = color
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            Text(primaryText, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ClearCacheConfirmationDialog(
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.clear_cache),
        onDismissRequest = onDismiss,
        content = {
            Column {
                Text(
                    text = "Все медиа останутся в облаке. При необходимости вы сможете загрузить их снова.",
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm, colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.clear_cache))
            }
        })
}

@Composable
private fun ClearDatabaseConfirmationDialog(
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.clear_database),
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = "Удалить все сообщения из локальной базы данных?",
                lineHeight = 16.sp
            )
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm, colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        })
}

@Composable
private fun TopBar(viewModel: StorageViewModel) {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack, onClick = navBackStack::removeLastOrNull
        ), actions = listOf(
            TopBarAction(
                icon = Icons.Rounded.MoreVert, onClick = {}, dropdownActions = listOf(
                    DropdownMenuAction(
                        icon = Icons.Rounded.DeleteOutline,
                        textResId = R.string.clear_database,
                        onClick = viewModel::showClearDatabaseDialog,
                        isDestructive = true
                    )
                )
            )
        )
    )
}
