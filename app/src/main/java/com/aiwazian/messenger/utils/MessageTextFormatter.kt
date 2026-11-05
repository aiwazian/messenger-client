/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

enum class MessageTextStyle {
    Bold, Italic, Underline, Strikethrough, Mono
}

data class MessageFormatSpan(
    val style: MessageTextStyle,
    val start: Int,
    val end: Int
)

enum class MessageFormatAction {
    ToggleBold, ToggleItalic, ToggleUnderline, ToggleStrikethrough, ToggleMono, Clear
}

data class MessageFormatParseResult(
    val plain: String,
    val spans: List<MessageFormatSpan>
)

private data class OpenMarker(
    val style: MessageTextStyle,
    val plainStart: Int,
    val seq: Int
)

object MessageTextFormatter {

    fun parse(text: String): MessageFormatParseResult {
        val plain = StringBuilder()
        val spans = mutableListOf<MessageFormatSpan>()
        val openMarkers = ArrayDeque<OpenMarker>()
        var index = 0
        var seq = 0

        while (index < text.length) {
            val marker = markerAt(text, index)

            if (marker == null) {
                plain.append(text[index])
                index += 1

                continue
            }

            val top = openMarkers.lastOrNull()

            if (top != null && top.style == marker.style) {
                openMarkers.removeLast()

                if (plain.length > top.plainStart) {
                    spans += MessageFormatSpan(marker.style, top.plainStart, plain.length)
                }
            } else {
                openMarkers.addLast(
                    OpenMarker(
                        style = marker.style,
                        plainStart = plain.length,
                        seq = seq
                    )
                )

                seq += 1
            }

            index += marker.length
        }

        openMarkers.sortedByDescending { it.seq }.forEach { open ->
            plain.insert(open.plainStart, open.style.markerText())

            spans.forEachIndexed { spanIndex, span ->
                if (span.start >= open.plainStart) {
                    spans[spanIndex] = span.copy(
                        start = span.start + open.style.markerText().length,
                        end = span.end + open.style.markerText().length
                    )
                }
            }
        }

        return MessageFormatParseResult(
            plain = plain.toString(),
            spans = spans.filter { it.end > it.start }
        )
    }

    fun serialize(plain: String, spans: List<MessageFormatSpan>): String {
        val normalized = normalize(spans)
        val out = StringBuilder()
        val boundaries = normalized
            .flatMap { listOf(it.start, it.end) }
            .distinct()
            .sorted()
        val open = ArrayDeque<MessageFormatSpan>()
        var previous = 0

        boundaries.forEach { boundary ->
            out.append(plain, previous, boundary)
            previous = boundary

            val ending = open.any { it.end == boundary }

            if (ending) {
                open.reversed().forEach { out.append(it.style.markerText()) }
                open.clear()
            }

            normalized
                .filter { it.start <= boundary && it.end > boundary }
                .sortedBy { it.style.styleOrder() }
                .forEach { span ->
                    if (span !in open) {
                        out.append(span.style.markerText())
                        open.addLast(span)
                    }
                }
        }

        out.append(plain, previous, plain.length)

        return out.toString()
    }

    private fun normalize(spans: List<MessageFormatSpan>): List<MessageFormatSpan> {
        val boundaries = spans
            .flatMap { listOf(it.start, it.end) }
            .distinct()
            .sorted()

        val pieces = mutableListOf<MessageFormatSpan>()

        spans.forEach { span ->
            val cuts = boundaries.filter { it > span.start && it < span.end }
            var cursor = span.start

            cuts.forEach { cut ->
                pieces += MessageFormatSpan(span.style, cursor, cut)
                cursor = cut
            }

            pieces += MessageFormatSpan(span.style, cursor, span.end)
        }

        val merged = mutableListOf<MessageFormatSpan>()

        MessageTextStyle.entries.forEach { style ->
            var current: MessageFormatSpan? = null

            pieces.filter { it.style == style }.sortedBy { it.start }.forEach { piece ->
                val last = current

                if (last != null && piece.start <= last.end) {
                    val index = merged.indexOf(last)

                    merged[index] = last.copy(end = maxOf(last.end, piece.end))
                    current = merged[index]
                } else {
                    current = piece
                    merged += piece
                }
            }
        }

        return merged
    }

    private fun markerAt(text: String, index: Int): Marker? {
        val char = text[index]

        return when {
            char == '_' && index + 1 < text.length && text[index + 1] == '_' ->
                Marker(MessageTextStyle.Underline, 2)

            char == '*' -> Marker(MessageTextStyle.Bold, 1)

            char == '_' -> Marker(MessageTextStyle.Italic, 1)

            char == '~' -> Marker(MessageTextStyle.Strikethrough, 1)

            char == '`' -> Marker(MessageTextStyle.Mono, 1)

            else -> null
        }
    }

    private data class Marker(
        val style: MessageTextStyle,
        val length: Int
    )
}

private fun MessageTextStyle.markerText(): String = when (this) {
    MessageTextStyle.Bold -> "*"
    MessageTextStyle.Italic -> "_"
    MessageTextStyle.Underline -> "__"
    MessageTextStyle.Strikethrough -> "~"
    MessageTextStyle.Mono -> "`"
}

private fun MessageTextStyle.styleOrder(): Int = when (this) {
    MessageTextStyle.Bold -> 0
    MessageTextStyle.Italic -> 1
    MessageTextStyle.Underline -> 2
    MessageTextStyle.Strikethrough -> 3
    MessageTextStyle.Mono -> 4
}
