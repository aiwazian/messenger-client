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
import androidx.compose.material.icons.rounded.DataArray
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.Html
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.Php
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile

fun String.getFileIcon() = when (this.trim().lowercase()) {
    "js" -> Icons.Rounded.Javascript
    "css" -> Icons.Rounded.Css
    "apk", "xapk", "apks", "aab" -> Icons.Rounded.Android
    "php" -> Icons.Rounded.Php
    "pdf" -> Icons.Rounded.PictureAsPdf
    "exe", "msi", "dmg", "deb", "rpm" -> Icons.Rounded.DesktopWindows
    "json", "yaml", "yml", "xml " -> Icons.Rounded.DataObject
    "toml" -> Icons.Rounded.DataArray
    "gif" -> Icons.Rounded.Gif
    "heic", "heif", "avif", "tif", "tiff", "ico", "png", "jpg", "jpeg", "webp", "bmp", "svg" -> Icons.Rounded.Image
    "mp3", "wav", "ogg", "flac", "m4a", "opus", "aac", "wma", "amr" -> Icons.Rounded.AudioFile
    "mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "wmv", "flv", "mpg", "mpeg" -> Icons.Rounded.VideoFile
    "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "iso" -> Icons.Rounded.FolderZip
    "txt", "md", "log" -> Icons.AutoMirrored.Rounded.TextSnippet
    "doc", "docx", "odt" -> Icons.Rounded.Description
    "html", "htm" -> Icons.Rounded.Html
    "kt", "java", "py", "cpp", "c", "cs", "swift", "go", "rs", "ts" -> Icons.Rounded.Code
    "sh", "bat", "cmd" -> Icons.Rounded.Terminal
    "xls", "xlsx", "csv", "ods" -> Icons.Rounded.TableChart
    "ppt", "pptx", "odp" -> Icons.Rounded.Slideshow
    "ttf", "otf", "woff", "woff2" -> Icons.Rounded.FontDownload
    "db", "sqlite", "sql" -> Icons.Rounded.Storage
    "env" -> Icons.Rounded.Tune
    else -> Icons.Rounded.FilePresent
}
