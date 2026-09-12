/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.widget.EditText
import androidx.core.graphics.withSave
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aiwazian.messenger.domain.CustomEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CUSTOM_EMOJI_TOKEN_PATTERN = Regex("""\[ce:(\d+):(\d+)]""")

class CustomEmojiSpan(
    private val drawable: Drawable,
    val packId: Long,
    val emojiId: Long
) : ReplacementSpan() {
    
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return drawable.bounds.width()
    }
    
    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        canvas.withSave {
            val lineCenterY = top + (bottom - top) / 2f
            val emojiHeight = drawable.bounds.height()
            val transY = lineCenterY - (emojiHeight / 2f)
            translate(x, transY)
            drawable.draw(this)
        }
    }
}

sealed interface CustomEmojiTextPart {
    
    data class Text(val value: String) : CustomEmojiTextPart
    
    data class Emoji(val packId: Long, val emojiId: Long) : CustomEmojiTextPart
}

object CustomEmojiText {
    
    const val PLACEHOLDER = "\uFFFC"
    
    fun token(packId: Long, emojiId: Long): String = "[ce:$packId:$emojiId]"
    
    fun serialize(text: CharSequence): String {
        if (text !is Spanned) {
            return text.toString()
        }
        
        val spans = text.getSpans(0, text.length, CustomEmojiSpan::class.java)
            .sortedBy { text.getSpanStart(it) }
        
        if (spans.isEmpty()) {
            return text.toString()
        }
        
        val builder = StringBuilder()
        var index = 0
        
        spans.forEach { span ->
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            
            if (start >= index) {
                builder.append(text, index, start)
                builder.append(token(span.packId, span.emojiId))
                
                index = end
            }
        }
        
        builder.append(text, index, text.length)
        
        return builder.toString()
    }
    
    fun parse(text: String): List<CustomEmojiTextPart> {
        val parts = mutableListOf<CustomEmojiTextPart>()
        var index = 0
        
        CUSTOM_EMOJI_TOKEN_PATTERN.findAll(text).forEach { match ->
            val packId = match.groupValues[1].toLongOrNull()
            val emojiId = match.groupValues[2].toLongOrNull()
            
            if (packId != null && emojiId != null) {
                if (match.range.first > index) {
                    parts.add(CustomEmojiTextPart.Text(text.substring(index, match.range.first)))
                }
                
                parts.add(CustomEmojiTextPart.Emoji(packId = packId, emojiId = emojiId))
                
                index = match.range.last + 1
            }
        }
        
        if (index < text.length) {
            parts.add(CustomEmojiTextPart.Text(text.substring(index)))
        }
        
        return parts
    }
}

suspend fun insertCustomEmoji(editText: EditText, packId: Long, emoji: CustomEmoji) {
    val size = customEmojiSize(editText)
    
    val drawable = loadCustomEmojiDrawable(
        context = editText.context,
        url = emoji.url,
        cacheKey = emoji.fileId,
        size = size
    ) ?: return
    
    withContext(Dispatchers.Main) {
        val spannable = SpannableString(CustomEmojiText.PLACEHOLDER)
        
        spannable.setSpan(
            CustomEmojiSpan(drawable = drawable, packId = packId, emojiId = emoji.id),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        val editable = editText.text
        val cursor = editText.selectionEnd.takeIf { it in 0..editable.length } ?: editable.length
        
        editable.insert(cursor, spannable)
    }
}

suspend fun buildCustomEmojiText(
    editText: EditText,
    text: String,
    resolveEmoji: suspend (Long) -> CustomEmoji?
): CharSequence {
    val parts = CustomEmojiText.parse(text)
    
    if (parts.none { it is CustomEmojiTextPart.Emoji }) {
        return text
    }
    
    val size = customEmojiSize(editText)
    val builder = SpannableStringBuilder()
    
    parts.forEach { part ->
        when (part) {
            is CustomEmojiTextPart.Text -> builder.append(part.value)
            
            is CustomEmojiTextPart.Emoji -> {
                val emoji = resolveEmoji(part.emojiId)
                
                val drawable = if (emoji == null) {
                    null
                } else {
                    loadCustomEmojiDrawable(
                        context = editText.context,
                        url = emoji.url,
                        cacheKey = emoji.fileId,
                        size = size
                    )
                }
                
                if (drawable == null) {
                    builder.append(CustomEmojiText.token(part.packId, part.emojiId))
                } else {
                    val start = builder.length
                    
                    builder.append(CustomEmojiText.PLACEHOLDER)
                    
                    builder.setSpan(
                        CustomEmojiSpan(
                            drawable = drawable,
                            packId = part.packId,
                            emojiId = part.emojiId
                        ),
                        start,
                        builder.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }
    
    return builder
}

private fun customEmojiSize(editText: EditText): Int {
    val lineHeight = editText.lineHeight
    
    return if (lineHeight > 0) {
        lineHeight
    } else {
        editText.textSize.toInt()
    }
}

private suspend fun loadCustomEmojiDrawable(
    context: Context,
    url: String,
    cacheKey: String,
    size: Int
): Drawable? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .size(size, size)
        .build()
    
    val result = Coil.imageLoader(context).execute(request)
    
    if (result !is SuccessResult) {
        return null
    }
    
    val drawable = result.drawable.mutate()
    
    drawable.setBounds(0, 0, size, size)
    
    return drawable
}
