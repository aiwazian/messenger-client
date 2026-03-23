/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
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

    // Обработка событий
    LaunchedEffect(Unit) {
        storageViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is StorageUiEvent.CacheCleared -> {
                    // Кеш успешно очищен
                }
                is StorageUiEvent.Error -> {
                    // Показать ошибку
                }
            }
        }
    }

    val sizeBytes = storageViewModel.appSize
    val sizeMb = sizeBytes / (1024.0 * 1024.0)
    val sizeMbRounded = BigDecimal(sizeMb).setScale(2, RoundingMode.HALF_UP).toDouble()

    Scaffold(
        topBar = { TopBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Заголовок с общим размером
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

            // Список категорий
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.categories) { category ->
                        CategoryItem(
                            category = category,
                            onToggle = { storageViewModel.toggleCategory(category.category) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { storageViewModel.selectAllCategories() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Выбрать все")
                            }
                            TextButton(
                                onClick = { storageViewModel.deselectAllCategories() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Снять все")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Кнопка очистки кеша
                        val selectedSize = uiState.selectedSize
                        val selectedSizeMb = selectedSize / (1024.0 * 1024.0)
                        val selectedSizeMbRounded = BigDecimal(selectedSizeMb)
                            .setScale(2, RoundingMode.HALF_UP)
                            .toDouble()

                        val hasSelection = uiState.selectedCategories.isNotEmpty()

                        Button(
                            onClick = { storageViewModel.showConfirmDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = if (hasSelection) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                }
                            ),
                            enabled = hasSelection
                        ) {
                            Text(
                                text = "Очистить кеш ($selectedSizeMbRounded MB)",
                                modifier = Modifier.padding(8.dp),
                                fontSize = 16.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Диалог подтверждения
        if (uiState.showConfirmDialog) {
            ClearCacheConfirmationDialog(
                selectedCategories = uiState.selectedCategories,
                totalSelectedSize = uiState.selectedSize,
                onConfirm = {
                    storageViewModel.clearSelectedCache(context)
                },
                onDismiss = { storageViewModel.hideConfirmDialog() }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryStats,
    onToggle: () -> Unit
) {
    val icon = when (category.category) {
        FileCategory.PHOTOS -> Icons.Outlined.Photo
        FileCategory.VIDEOS -> Icons.Outlined.VideoFile
        FileCategory.DOCUMENTS -> Icons.Outlined.Description
        FileCategory.AUDIO -> Icons.Outlined.AudioFile
        FileCategory.OTHER -> Icons.Outlined.Folder
    }

    val sizeMb = category.totalSize / (1024.0 * 1024.0)
    val sizeMbRounded = BigDecimal(sizeMb).setScale(2, RoundingMode.HALF_UP).toDouble()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = category.isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.category.title,
                    tint = if (category.isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Информация
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.category.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (category.isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "${category.fileCount} файлов • $sizeMbRounded MB",
                    fontSize = 12.sp,
                    color = if (category.isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Индикатор выбора
            if (category.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Выбрано",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ClearCacheConfirmationDialog(
    selectedCategories: List<CategoryStats>,
    totalSelectedSize: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sizeMb = totalSelectedSize / (1024.0 * 1024.0)
    val sizeMbRounded = BigDecimal(sizeMb).setScale(2, RoundingMode.HALF_UP).toDouble()

    val categoryNames = selectedCategories.joinToString(", ") { it.category.title }

    CustomDialog(
        title = "Очистка кеша",
        onDismissRequest = onDismiss,
        content = {
            Column {
                Text(
                    text = "Вы собираетесь удалить следующие категории:",
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = categoryNames,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Общий размер: $sizeMbRounded MB",
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
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
        }
    )
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
