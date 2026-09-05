/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.icu.text.BreakIterator
import android.icu.text.UnicodeSet

object EmojiInput {
    
    const val DEFAULT_EMOJI = "👍"
    
    const val MAX_EMOJI_COUNT = 10
    
    fun parse(value: String, maxCount: Int = MAX_EMOJI_COUNT): List<String> {
        val result = mutableListOf<String>()
        val iterator = BreakIterator.getCharacterInstance()
        
        iterator.setText(value)
        
        var start = iterator.first()
        var end = iterator.next()
        
        while (end != BreakIterator.DONE && result.size < maxCount) {
            val cluster = withoutSkinTones(value.substring(start, end))
            
            if (isEmoji(cluster) && !result.contains(cluster)) {
                result.add(cluster)
            }
            
            start = end
            end = iterator.next()
        }
        
        return result
    }
    
    fun format(emojis: List<String>): String = emojis.joinToString(separator = "")
    
    private fun withoutSkinTones(value: String): String = buildString {
        var index = 0
        
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            
            index += Character.charCount(codePoint)
            
            if (codePoint < SKIN_TONE_FIRST || codePoint > SKIN_TONE_LAST) {
                appendCodePoint(codePoint)
            }
        }
    }
    
    private fun isEmoji(value: String): Boolean {
        if (value.isEmpty()) {
            return false
        }
        
        val hasKeycap = value.any { it.code == COMBINING_KEYCAP }
        
        var hasPictographic = false
        var index = 0
        
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            
            index += Character.charCount(codePoint)
            
            when {
                PICTOGRAPHIC.contains(codePoint) -> hasPictographic = true
                
                codePoint in REGIONAL_INDICATOR_FIRST..REGIONAL_INDICATOR_LAST -> {
                    hasPictographic = true
                }
                
                isKeycapBase(codePoint) -> {
                    if (!hasKeycap) {
                        return false
                    }
                    
                    hasPictographic = true
                }
                
                isJoiner(codePoint) -> Unit
                
                else -> return false
            }
        }
        
        return hasPictographic
    }
    
    private fun isKeycapBase(codePoint: Int): Boolean = codePoint == NUMBER_SIGN ||
            codePoint == ASTERISK ||
            codePoint in DIGIT_FIRST..DIGIT_LAST
    
    private fun isJoiner(codePoint: Int): Boolean = codePoint == ZERO_WIDTH_JOINER ||
            codePoint == VARIATION_SELECTOR_15 ||
            codePoint == VARIATION_SELECTOR_16 ||
            codePoint == COMBINING_KEYCAP ||
            codePoint == TAG_TERMINATOR ||
            codePoint in TAG_FIRST..TAG_LAST
    
    private val PICTOGRAPHIC = UnicodeSet("[:Extended_Pictographic:]").freeze()
    
    private const val SKIN_TONE_FIRST = 0x1F3FB
    private const val SKIN_TONE_LAST = 0x1F3FF
    
    private const val REGIONAL_INDICATOR_FIRST = 0x1F1E6
    private const val REGIONAL_INDICATOR_LAST = 0x1F1FF
    
    private const val TAG_FIRST = 0xE0020
    private const val TAG_LAST = 0xE007E
    private const val TAG_TERMINATOR = 0xE007F
    
    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val VARIATION_SELECTOR_15 = 0xFE0E
    private const val VARIATION_SELECTOR_16 = 0xFE0F
    private const val COMBINING_KEYCAP = 0x20E3
    
    private const val NUMBER_SIGN = 0x23
    private const val ASTERISK = 0x2A
    private const val DIGIT_FIRST = 0x30
    private const val DIGIT_LAST = 0x39
}
