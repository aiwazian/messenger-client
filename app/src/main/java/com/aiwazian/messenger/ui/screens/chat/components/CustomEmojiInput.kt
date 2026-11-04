/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.icu.text.BreakIterator
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import androidx.core.graphics.withSave
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.ui.components.isVideoMediaUrl
import com.aiwazian.messenger.utils.MessageFormatAction
import com.aiwazian.messenger.utils.MessageFormatSpan
import com.aiwazian.messenger.utils.MessageTextFormatter
import com.aiwazian.messenger.utils.MessageTextStyle
import com.aiwazian.messenger.utils.RegexPatterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    
    fun toPlainText(
        text: String,
        defaultEmoji: (emojiId: Long) -> String?
    ): String {
        val parts = parse(text)
        
        if (parts.none { it is CustomEmojiTextPart.Emoji }) {
            return text
        }
        
        return parts.joinToString(separator = "") { part ->
            when (part) {
                is CustomEmojiTextPart.Text -> part.value
                
                is CustomEmojiTextPart.Emoji -> defaultEmoji(part.emojiId).orEmpty()
            }
        }
    }
    
    fun parse(text: String): List<CustomEmojiTextPart> {
        val parts = mutableListOf<CustomEmojiTextPart>()
        var index = 0
        
        RegexPatterns.CUSTOM_EMOJI_TOKEN.findAll(text).forEach { match ->
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

    val drawable = if (isVideoMediaUrl(emoji.url)) {
        VideoEmojiDrawable(
            context = editText.context,
            url = emoji.url,
            size = size,
            view = editText
        )
    } else {
        loadCustomEmojiDrawable(
            context = editText.context,
            url = emoji.url,
            cacheKey = emoji.fileId,
            size = size
        )
    } ?: return
    
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

fun insertSystemEmoji(editText: EditText, emoji: String) {
    val editable = editText.text
    val cursor = editText.selectionEnd.takeIf { it in 0..editable.length } ?: editable.length
    
    editable.insert(cursor, emoji)
}

fun deleteBeforeCursor(editText: EditText) {
    val editable = editText.text
    
    if (editable.isEmpty()) {
        return
    }
    
    val start = editText.selectionStart.takeIf { it in 0..editable.length } ?: editable.length
    val end = editText.selectionEnd.takeIf { it in 0..editable.length } ?: editable.length
    
    val from = minOf(start, end)
    val to = maxOf(start, end)
    
    if (from != to) {
        editable.delete(from, to)
        
        return
    }
    
    if (from == 0) {
        return
    }
    
    val iterator = BreakIterator.getCharacterInstance()
    
    iterator.setText(editable.toString())
    
    val previous = iterator.preceding(from)
    
    editable.delete(
        if (previous == BreakIterator.DONE) from - 1 else previous,
        from
    )
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

                val drawable = when {
                    emoji == null -> null

                    isVideoMediaUrl(emoji.url) -> VideoEmojiDrawable(
                        context = editText.context,
                        url = emoji.url,
                        size = size,
                        view = editText
                    )

                    else -> loadCustomEmojiDrawable(
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

internal fun toMarkedText(editable: Editable): String {
    val tokenString = CustomEmojiText.serialize(editable)
    val index = buildEmojiTokenIndex(editable)
    val spans = mutableListOf<MessageFormatSpan>()

    editable.getSpans(0, editable.length, StyleSpan::class.java)
        .filter { it.style == Typeface.BOLD }
        .forEach { span ->
            val start = index.fieldToToken(editable.getSpanStart(span))
            val end = index.fieldToToken(editable.getSpanEnd(span))

            if (end > start) {
                spans += MessageFormatSpan(MessageTextStyle.Bold, start, end)
            }
        }

    editable.getSpans(0, editable.length, StyleSpan::class.java)
        .filter { it.style == Typeface.ITALIC }
        .forEach { span ->
            val start = index.fieldToToken(editable.getSpanStart(span))
            val end = index.fieldToToken(editable.getSpanEnd(span))

            if (end > start) {
                spans += MessageFormatSpan(MessageTextStyle.Italic, start, end)
            }
        }

    editable.getSpans(0, editable.length, TypefaceSpan::class.java)
        .filter { it.family == MONOSPACE_FONT_FAMILY }
        .forEach { span ->
            val start = index.fieldToToken(editable.getSpanStart(span))
            val end = index.fieldToToken(editable.getSpanEnd(span))

            if (end > start) {
                spans += MessageFormatSpan(MessageTextStyle.Mono, start, end)
            }
        }

    editable.getSpans(0, editable.length, UnderlineSpan::class.java)
        .filter { editable.getSpanFlags(it) and Spanned.SPAN_COMPOSING == 0 }
        .forEach { span ->
            val start = index.fieldToToken(editable.getSpanStart(span))
            val end = index.fieldToToken(editable.getSpanEnd(span))

            if (end > start) {
                spans += MessageFormatSpan(MessageTextStyle.Underline, start, end)
            }
        }

    editable.getSpans(0, editable.length, StrikethroughSpan::class.java)
        .forEach { span ->
            val start = index.fieldToToken(editable.getSpanStart(span))
            val end = index.fieldToToken(editable.getSpanEnd(span))

            if (end > start) {
                spans += MessageFormatSpan(MessageTextStyle.Strikethrough, start, end)
            }
        }

    return MessageTextFormatter.serialize(tokenString, spans)
}

internal suspend fun buildFormattedFieldContent(
    editText: EditText,
    markedText: String,
    resolveEmoji: suspend (Long) -> CustomEmoji?
): CharSequence {
    val parsed = MessageTextFormatter.parse(markedText)
    val content = buildCustomEmojiText(editText, parsed.plain, resolveEmoji)
    val spannable = content as? Spannable ?: SpannableStringBuilder(content)

    if (parsed.spans.isNotEmpty()) {
        val index = buildEmojiTokenIndex(spannable)

        parsed.spans.forEach { span ->
            val start = index.tokenToField(span.start, clampToEnd = false)
            val end = index.tokenToField(span.end, clampToEnd = true)

            if (start == null || end == null || end <= start) {
                return@forEach
            }

            when (span.style) {
                MessageTextStyle.Bold -> spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                MessageTextStyle.Italic -> spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                MessageTextStyle.Mono -> spannable.setSpan(
                    TypefaceSpan(MONOSPACE_FONT_FAMILY),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                MessageTextStyle.Underline -> spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                MessageTextStyle.Strikethrough -> spannable.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    return spannable
}

internal fun applyMessageFormatAction(
    editText: EditText,
    action: MessageFormatAction,
    onTextChange: (String) -> Unit
): Boolean {
    val editable = editText.text
    val selectionStart = editText.selectionStart.takeIf { it in 0..editable.length } ?: return false
    val selectionEnd = editText.selectionEnd.takeIf { it in 0..editable.length } ?: return false
    val start = minOf(selectionStart, selectionEnd)
    val end = maxOf(selectionStart, selectionEnd)

    if (start >= end) {
        return false
    }

    editText.beginBatchEdit()

    when (action) {
        MessageFormatAction.Clear -> clearFormatSpans(editable, start, end)

        MessageFormatAction.ToggleBold -> toggleFormatSpan(editable, start, end, MessageTextStyle.Bold)

        MessageFormatAction.ToggleItalic -> toggleFormatSpan(editable, start, end, MessageTextStyle.Italic)

        MessageFormatAction.ToggleUnderline -> toggleFormatSpan(editable, start, end, MessageTextStyle.Underline)

        MessageFormatAction.ToggleStrikethrough -> toggleFormatSpan(editable, start, end, MessageTextStyle.Strikethrough)

        MessageFormatAction.ToggleMono -> toggleFormatSpan(editable, start, end, MessageTextStyle.Mono)
    }

    editText.endBatchEdit()

    editText.setSelection(end)
    onTextChange(toMarkedText(editable))

    return true
}

private data class EditableFormatSpan(
    val span: Any,
    val style: MessageTextStyle,
    val start: Int,
    val end: Int
)

private fun formatSpansIn(editable: Editable, start: Int, end: Int): List<EditableFormatSpan> {
    val spans = mutableListOf<EditableFormatSpan>()

    editable.getSpans(start, end, StyleSpan::class.java)
        .filter { it.style == Typeface.BOLD && editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        .forEach { spans += EditableFormatSpan(it, MessageTextStyle.Bold, editable.getSpanStart(it), editable.getSpanEnd(it)) }

    editable.getSpans(start, end, StyleSpan::class.java)
        .filter { it.style == Typeface.ITALIC && editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        .forEach { spans += EditableFormatSpan(it, MessageTextStyle.Italic, editable.getSpanStart(it), editable.getSpanEnd(it)) }

    editable.getSpans(start, end, TypefaceSpan::class.java)
        .filter { it.family == MONOSPACE_FONT_FAMILY && editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        .forEach { spans += EditableFormatSpan(it, MessageTextStyle.Mono, editable.getSpanStart(it), editable.getSpanEnd(it)) }

    editable.getSpans(start, end, UnderlineSpan::class.java)
        .filter {
            editable.getSpanFlags(it) and Spanned.SPAN_COMPOSING == 0 &&
                    editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start
        }
        .forEach { spans += EditableFormatSpan(it, MessageTextStyle.Underline, editable.getSpanStart(it), editable.getSpanEnd(it)) }

    editable.getSpans(start, end, StrikethroughSpan::class.java)
        .filter { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        .forEach { spans += EditableFormatSpan(it, MessageTextStyle.Strikethrough, editable.getSpanStart(it), editable.getSpanEnd(it)) }

    return spans
}

private fun newFormatSpan(style: MessageTextStyle): Any = when (style) {
    MessageTextStyle.Bold -> StyleSpan(Typeface.BOLD)
    MessageTextStyle.Italic -> StyleSpan(Typeface.ITALIC)
    MessageTextStyle.Underline -> UnderlineSpan()
    MessageTextStyle.Strikethrough -> StrikethroughSpan()
    MessageTextStyle.Mono -> TypefaceSpan(MONOSPACE_FONT_FAMILY)
}

private fun isFullyCovered(
    spans: List<EditableFormatSpan>,
    target: MessageTextStyle,
    start: Int,
    end: Int
): Boolean {
    var cursor = start

    spans.filter { it.style == target }.sortedBy { it.start }.forEach { span ->
        if (span.start > cursor) {
            return false
        }

        cursor = maxOf(cursor, span.end)
    }

    return cursor >= end
}

private fun toggleFormatSpan(
    editable: Editable,
    start: Int,
    end: Int,
    style: MessageTextStyle
) {
    val spans = formatSpansIn(editable, start, end)

    if (isFullyCovered(spans, style, start, end)) {
        spans.filter { it.style == style }.forEach { span ->
            editable.removeSpan(span.span)

            if (span.start < start) {
                editable.setSpan(newFormatSpan(style), span.start, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (span.end > end) {
                editable.setSpan(newFormatSpan(style), end, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    } else {
        var mergedStart = start
        var mergedEnd = end

        spans.filter { it.style == style }.forEach { span ->
            mergedStart = minOf(mergedStart, span.start)
            mergedEnd = maxOf(mergedEnd, span.end)

            editable.removeSpan(span.span)
        }

        editable.setSpan(newFormatSpan(style), mergedStart, mergedEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private fun clearFormatSpans(editable: Editable, start: Int, end: Int) {
    formatSpansIn(editable, start, end).forEach { span ->
        editable.removeSpan(span.span)

        if (span.start < start) {
            editable.setSpan(newFormatSpan(span.style), span.start, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (span.end > end) {
            editable.setSpan(newFormatSpan(span.style), end, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

internal data class EmojiTokenSegment(
    val fieldStart: Int,
    val fieldEnd: Int,
    val tokenStart: Int,
    val tokenEnd: Int,
    val isToken: Boolean
)

internal class EmojiTokenIndex internal constructor(
    private val segments: List<EmojiTokenSegment>
) {

    fun fieldToToken(position: Int): Int {
        val clamped = position.coerceIn(0, segments.last().fieldEnd)

        segments.forEach { segment ->
            if (clamped <= segment.fieldEnd) {
                return if (segment.isToken && clamped > segment.fieldStart) {
                    segment.tokenEnd
                } else {
                    segment.tokenStart + (clamped - segment.fieldStart)
                }
            }
        }

        return segments.last().tokenEnd
    }

    fun tokenToField(position: Int, clampToEnd: Boolean): Int? {
        val clamped = position.coerceIn(0, segments.last().tokenEnd)

        segments.forEach { segment ->
            if (clamped <= segment.tokenEnd) {
                return if (segment.isToken && clamped > segment.tokenStart) {
                    if (clampToEnd) segment.fieldEnd else segment.fieldStart
                } else {
                    segment.fieldStart + (clamped - segment.tokenStart)
                }
            }
        }

        return null
    }
}

internal fun buildEmojiTokenIndex(text: CharSequence): EmojiTokenIndex {
    if (text !is Spanned) {
        return EmojiTokenIndex(
            listOf(
                EmojiTokenSegment(
                    fieldStart = 0,
                    fieldEnd = text.length,
                    tokenStart = 0,
                    tokenEnd = text.length,
                    isToken = false
                )
            )
        )
    }

    val spans = text.getSpans(0, text.length, CustomEmojiSpan::class.java)
        .sortedBy { text.getSpanStart(it) }

    if (spans.isEmpty()) {
        return EmojiTokenIndex(
            listOf(
                EmojiTokenSegment(
                    fieldStart = 0,
                    fieldEnd = text.length,
                    tokenStart = 0,
                    tokenEnd = text.length,
                    isToken = false
                )
            )
        )
    }

    val segments = mutableListOf<EmojiTokenSegment>()
    var fieldPosition = 0
    var tokenPosition = 0

    spans.forEach { span ->
        val fieldStart = text.getSpanStart(span)
        val fieldEnd = text.getSpanEnd(span)

        if (fieldStart > fieldPosition) {
            val length = fieldStart - fieldPosition

            segments += EmojiTokenSegment(
                fieldStart = fieldPosition,
                fieldEnd = fieldStart,
                tokenStart = tokenPosition,
                tokenEnd = tokenPosition + length,
                isToken = false
            )

            tokenPosition += length
            fieldPosition = fieldStart
        }

        val tokenLength = CustomEmojiText.token(span.packId, span.emojiId).length

        segments += EmojiTokenSegment(
            fieldStart = fieldStart,
            fieldEnd = fieldEnd,
            tokenStart = tokenPosition,
            tokenEnd = tokenPosition + tokenLength,
            isToken = true
        )

        tokenPosition += tokenLength
        fieldPosition = fieldEnd
    }

    if (fieldPosition < text.length) {
        val length = text.length - fieldPosition

        segments += EmojiTokenSegment(
            fieldStart = fieldPosition,
            fieldEnd = text.length,
            tokenStart = tokenPosition,
            tokenEnd = tokenPosition + length,
            isToken = false
        )
    }

    return EmojiTokenIndex(segments)
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
    
    val result = SingletonImageLoader.get(context).execute(request)
    
    if (result !is SuccessResult) {
        return null
    }
    
    val drawable = result.image.asDrawable(context.resources).mutate()
    
    drawable.setBounds(0, 0, size, size)
    
    return drawable
}

private const val MONOSPACE_FONT_FAMILY = "monospace"
