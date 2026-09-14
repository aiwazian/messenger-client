/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

object UnicodeEmojiParser {
    
    fun parse(lines: Sequence<String>): List<String> = lines.filter { line ->
        line.isNotBlank() && !line.startsWith(COMMENT_PREFIX)
    }.filter { line ->
        line.contains(FULLY_QUALIFIED_MARKER)
    }.filterNot { line ->
        SKIN_TONE_CODES.any { code -> line.contains(code) }
    }.mapNotNull { line ->
        line.substringAfter(COMMENT_PREFIX, "")
            .trim()
            .substringBefore(' ')
            .takeIf { it.isNotEmpty() }
    }.distinct().toList()
    
    private const val COMMENT_PREFIX = "#"
    
    private const val FULLY_QUALIFIED_MARKER = "fully-qualified"
    
    private val SKIN_TONE_CODES = listOf("1F3FB", "1F3FC", "1F3FD", "1F3FE", "1F3FF")
}
