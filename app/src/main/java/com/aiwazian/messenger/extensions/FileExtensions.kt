/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Css
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Html
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.Php
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Window
import androidx.compose.ui.graphics.vector.ImageVector

fun String.getFileIcon(): ImageVector {
    return when (this.lowercase()) {
        "js" -> Icons.Rounded.Javascript
        "css" -> Icons.Rounded.Css
        "apk" -> Icons.Rounded.Android
        "php" -> Icons.Rounded.Php
        "pdf" -> Icons.Rounded.PictureAsPdf
        "exe" -> Icons.Rounded.Window
        "json" -> Icons.Rounded.DataObject
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg" -> Icons.Rounded.Image
        "mp3", "wav", "ogg", "flac", "m4a" -> Icons.Rounded.AudioFile
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Rounded.VideoFile
        "zip", "rar", "7z", "tar", "gz" -> Icons.Rounded.FolderZip
        "txt", "md" -> Icons.AutoMirrored.Rounded.TextSnippet
        "doc", "docx", "odt" -> Icons.Rounded.Description
        "html", "htm" -> Icons.Rounded.Html
        "kt", "java", "py", "cpp", "c", "cs", "swift", "go", "rs", "ts" -> Icons.Rounded.Code
        "sh", "bat", "cmd" -> Icons.Rounded.Terminal
        else -> Icons.Rounded.FilePresent
    }
}
