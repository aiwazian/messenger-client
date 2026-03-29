/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import androidx.annotation.StringRes
import com.aiwazian.messenger.R

enum class FileCategory(
    @param:StringRes val title: Int,
    val extensions: Set<String>
) {
    PHOTOS(
        title = R.string.photos,
        extensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
    ),
    VIDEOS(
        title = R.string.videos,
        extensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "3gp", "m4v")
    ),
    DOCUMENTS(
        title = R.string.documents,
        extensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp")
    ),
    AUDIO(
        title = R.string.audio,
        extensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "opus")
    ),
    OTHER(
        title = R.string.other,
        extensions = setOf("apk", "zip", "rar", "7z", "tar", "gz", "exe", "dmg", "iso")
    );
    
    companion object {
        fun fromExtension(extension: String): FileCategory {
            val lowerExt = extension.lowercase()
            return entries.find { it.extensions.contains(lowerExt) } ?: OTHER
        }
    }
}

data class StorageFile(
    val id: String,
    val name: String,
    val size: Long,
    val extension: String,
    val category: FileCategory,
    val localUri: String,
    val messageId: Int,
    val chatId: Long
)

data class CategoryStats(
    val category: FileCategory,
    val fileCount: Int,
    val totalSize: Long,
    val isSelected: Boolean = false
)

data class StorageUiState(
    val categories: List<CategoryStats> = emptyList(),
    val totalCacheSize: Long = 0,
    val selectedSize: Long = 0,
    val isLoading: Boolean = false,
    val showConfirmDialog: Boolean = false
) {
    val selectedCategories: List<CategoryStats>
        get() = categories.filter { it.isSelected }
}
