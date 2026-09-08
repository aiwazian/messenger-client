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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.extensions.formatFileSize
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun StorageScreen(viewModel: StorageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            val message = when (event) {
                is StorageUiEvent.CacheCleared -> context.getString(
                    R.string.storage_cache_cleared, event.freedBytes.formatFileSize()
                )
                
                StorageUiEvent.CacheAlreadyEmpty -> context.getString(
                    R.string.storage_cache_already_empty
                )
                
                StorageUiEvent.DatabaseCleared -> context.getString(
                    R.string.storage_database_cleared
                )
                
                is StorageUiEvent.Error -> event.message.asString(context)
            }
            
            snackbarJob?.cancel()
            snackbarJob = scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }
    
    AppScaffold(
        topBar = {
            PageTopBar(
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.MoreVert, dropdownActions = listOf(
                            DropdownMenuAction(
                                icon = Icons.Rounded.DeleteOutline,
                                text = UiText.StringResource(R.string.clear_database),
                                onClick = viewModel::showClearDatabaseDialog,
                                isDestructive = true
                            )
                        )
                    )
                )
            )
        }, snackbarHost = {
            AppSnackbar(snackbarHostState)
        }) {
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
                text = uiState.appSize.formatFileSize(),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SectionContainer(footer = {
            SectionDescription("Все медиа останутся в облаке, при необходимости Вы сможете загрузить их.")
        }) {
            uiState.categories.forEachIndexed { index, category ->
                StorageCategory(
                    text = stringResource(category.category.title),
                    selected = category.isSelected,
                    color = AppPrimaryColor.entries[index].color,
                    primaryText = category.totalSize.formatFileSize(),
                    onClick = {
                        viewModel.toggleCategory(category.category)
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
                onClick = viewModel::showConfirmDialog,
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
    
    if (uiState.showConfirmDialog) {
        ClearCacheConfirmationDialog(
            onConfirm = {
                viewModel.clearSelectedCache()
            }, onDismiss = viewModel::hideConfirmDialog
        )
    }
    
    if (uiState.showClearDatabaseDialog) {
        ClearDatabaseConfirmationDialog(
            onConfirm = {
                viewModel.clearDatabase()
                viewModel.hideClearDatabaseDialog()
            }, onDismiss = viewModel::hideClearDatabaseDialog
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
    AppDialog(
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
    AppDialog(
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
