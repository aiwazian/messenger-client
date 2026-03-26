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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrimaryColorOption
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.flow.collectLatest
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(storageViewModel: StorageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by storageViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        storageViewModel.loadStorageInfo(context)
    }
    
    LaunchedEffect(Unit) {
        storageViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is StorageUiEvent.CacheCleared -> { // Кеш успешно очищен
                }
                
                is StorageUiEvent.Error -> { // Показать ошибку
                }
            }
        }
    }
    
    val sizeBytes = storageViewModel.appSize
    val sizeMb = sizeBytes / (1024.0 * 1024.0)
    val sizeMbRounded = BigDecimal(sizeMb).setScale(
        2,
        RoundingMode.HALF_UP
    ).toDouble()
    
    Scaffold(
        topBar = { TopBar() }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Использование памяти",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W500
                )
                Text(
                    text = "$sizeMbRounded MB",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                SectionContainer(footer = { SectionDescription("Все медиа останутся в облаке, при необходимости Вы сможете загрузить их.") }) {
                    uiState.categories.forEachIndexed { index, category ->
                        val sizeMb = category.totalSize / (1024.0 * 1024.0)
                        val sizeMbRounded = BigDecimal(sizeMb).setScale(
                            2,
                            RoundingMode.HALF_UP
                        ).toDouble()
                        
                        SectionRadioItem(
                            text = category.category.title,
                            selected = category.isSelected,
                            primaryText = "$sizeMbRounded MB",
                            radioColor = PrimaryColorOption.entries[index].color,
                            onClick = { storageViewModel.toggleCategory(category.category) })
                        
                    }
                    
                    val selectedSize = uiState.selectedSize
                    val selectedSizeMb = selectedSize / (1024.0 * 1024.0)
                    val selectedSizeMbRounded = BigDecimal(selectedSizeMb).setScale(
                        2,
                        RoundingMode.HALF_UP
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
                                text = "Очистить кеш (",
                                fontSize = 16.sp,
                                lineHeight = 18.sp
                            )
                            AnimatedContent(
                                targetState = selectedSizeMbRounded,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInVertically { -it } + fadeIn() + scaleIn() togetherWith slideOutVertically { it } + fadeOut() + scaleOut()
                                    } else {
                                        slideInVertically { it } + fadeIn() + scaleIn() togetherWith slideOutVertically { -it } + fadeOut() + scaleOut()
                                    }
                                }) { size ->
                                Text(
                                    text = "$size",
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            Text(
                                text = "MB)",
                                fontSize = 16.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (uiState.showConfirmDialog) {
        ClearCacheConfirmationDialog(
            onConfirm = {
                storageViewModel.clearSelectedCache(context)
            },
            onDismiss = storageViewModel::hideConfirmDialog
        )
    }
}

@Composable
private fun ClearCacheConfirmationDialog(
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    CustomDialog(
        title = "Очистка кеша",
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
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
            }
        })
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}
