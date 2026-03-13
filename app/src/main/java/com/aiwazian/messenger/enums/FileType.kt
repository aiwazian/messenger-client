package com.aiwazian.messenger.enums

enum class FileType {
    IMAGE,
    VIDEO,
    MUSIC,
    ZIP,
    TEXT,
    HTML,
    CSS,
    JAVASCRIPT,
    PHP,
    APK,
    GIF,
    JSON,
    OTHER;
    
    companion object {
        private val extensionMap: Map<String, FileType> = mapOf(
            "jpg" to IMAGE,
            "jpeg" to IMAGE,
            "png" to IMAGE,
            "gif" to IMAGE,
            "bmp" to IMAGE,
            "mp4" to VIDEO,
            "avi" to VIDEO,
            "mkv" to VIDEO,
            "mov" to VIDEO,
            "mp3" to MUSIC,
            "wav" to MUSIC,
            "aac" to MUSIC,
            "flac" to MUSIC,
            "zip" to ZIP,
            "txt" to TEXT,
            "html" to HTML,
            "css" to CSS,
            "js" to JAVASCRIPT,
            "php" to PHP,
            "gif" to GIF,
            "apk" to APK,
            "json" to JSON
        )
        
        fun fromExtension(extension: String): FileType {
            return extensionMap[extension.lowercase()] ?: OTHER
        }
    }
}
