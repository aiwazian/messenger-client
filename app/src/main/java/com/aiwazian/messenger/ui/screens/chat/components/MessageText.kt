/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.appendFormattedMessageRange
import com.aiwazian.messenger.ui.components.rememberCustomEmojiInlineContent
import com.aiwazian.messenger.utils.LinkNormalizer
import com.aiwazian.messenger.utils.MessageFormatSpan
import com.aiwazian.messenger.utils.MessageTextFormatter
import com.aiwazian.messenger.utils.PhoneLinks
import com.aiwazian.messenger.utils.RegexPatterns
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MessageText(
    text: String,
    modifier: Modifier = Modifier,
    isTextSelectionEnabled: Boolean = false,
    onCopySelected: ((String) -> Unit)? = null,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    onEmailClicked: ((String) -> Unit)? = null,
    onPhoneClicked: ((String) -> Unit)? = null
) {
    val format = remember(text) { MessageTextFormatter.parse(text) }
    val parts = remember(format.plain) { CustomEmojiText.parse(format.plain) }
    
    val inlineContent = rememberCustomEmojiInlineContent(
        text = format.plain,
        emojiSize = CUSTOM_EMOJI_SIZE
    )
    
    val linkColor = MaterialTheme.colorScheme.primary
    val pressedColor = MaterialTheme.colorScheme.primary.copy(alpha = LINK_PRESSED_ALPHA)
    val deviceRegion = remember { PhoneLinks.currentDeviceRegion() }
    
    var selectionRange by remember(text) { mutableStateOf<TextRange?>(null) }
    var selectionTextWidthPx by remember { mutableStateOf<Int?>(null) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textOrigin by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    
    fun selectTextRange(range: TextRange) {
        selectionTextWidthPx = textLayout?.size?.width
        
        selectionRange = range
    }
    
    val annotatedString = buildAnnotatedString {
        var partStart = 0
        
        parts.forEach { part ->
            when (part) {
                is CustomEmojiTextPart.Text -> {
                    val partEnd = partStart + part.value.length
                    val partSpans = format.spans.mapNotNull { span ->
                        val start = maxOf(span.start, partStart)
                        val end = minOf(span.end, partEnd)
                        
                        if (end > start) {
                            MessageFormatSpan(span.style, start - partStart, end - partStart)
                        } else {
                            null
                        }
                    }
                    
                    appendMessageText(
                        text = part.value,
                        formatSpans = partSpans,
                        region = deviceRegion,
                        linkColor = linkColor,
                        pressedColor = pressedColor,
                        onLinkClicked = onLinkClicked,
                        onUsernameClicked = onUsernameClicked,
                        onEmailClicked = onEmailClicked,
                        onPhoneClicked = onPhoneClicked
                    )
                    
                    partStart = partEnd
                }
                
                is CustomEmojiTextPart.Emoji -> {
                    appendInlineContent(
                        id = part.emojiId.toString(),
                        alternateText = CustomEmojiText.PLACEHOLDER
                    )
                    
                    partStart += CustomEmojiText.token(part.packId, part.emojiId).length
                }
            }
        }
    }
    
    val selection = selectionRange
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = MESSAGE_SELECTION_ALPHA)
    var isHandleDragging by remember { mutableStateOf(false) }
    
    Text(
        text = annotatedString,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        inlineContent = inlineContent,
        onTextLayout = { textLayout = it },
        modifier = modifier
            .then(
                if (selection != null && selectionTextWidthPx != null) {
                    with(density) { Modifier.width(selectionTextWidthPx!!.toDp()) }
                } else {
                    Modifier
                }
            )
            .drawBehind {
                val range = selectionRange
                
                if (range != null && !range.collapsed) {
                    textLayout?.let { layout ->
                        drawPath(
                            path = layout.getPathForRange(range.min, range.max),
                            color = selectionColor
                        )
                    }
                }
            }
            .onGloballyPositioned {
                val origin = it.positionInWindow()
                
                textOrigin = IntOffset(origin.x.roundToInt(), origin.y.roundToInt())
            }
            .pointerInput(text, isTextSelectionEnabled) {
                if (!isTextSelectionEnabled) {
                    return@pointerInput
                }
                
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        awaitLongPressOrCancellation(down.id)
                    }
                    
                    if (longPress != null) {
                        longPress.consume()
                        
                        val layout = textLayout ?: return@awaitEachGesture
                        val position = layout.getOffsetForPosition(longPress.position)
                        val word = layout.getWordBoundary(position)
                        val trimmed = trimmedSelectionRange(annotatedString.text, word)
                        
                        if (!trimmed.collapsed) {
                            selectTextRange(trimmed)
                        }
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            event.changes.forEach { it.consume() }
                            
                            if (event.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    }
                }
            }
    )
    
    if (selection != null) {
        BackHandler {
            selectionRange = null
        }
        
        val density = LocalDensity.current
        val context = LocalContext.current
        val handleDrawable = remember {
            resolveSelectionHandleDrawable(context, android.R.attr.textSelectHandle)
        }
        val handleTint = MaterialTheme.colorScheme.primary
        
        val handleSize = selectionHandleSize(handleDrawable, density)
        val layout = textLayout
        
        val startHandlePosition = if (layout != null) {
            val cursor = layout.getCursorRect(selection.min)
            
            IntOffset(
                textOrigin.x + cursor.left.roundToInt() - handleSize.width / 2,
                textOrigin.y + cursor.bottom.roundToInt()
            )
        } else {
            IntOffset.Zero
        }
        
        val endHandlePosition = if (layout != null) {
            val cursor = layout.getCursorRect(selection.max)
            
            IntOffset(
                textOrigin.x + cursor.left.roundToInt() - handleSize.width / 2,
                textOrigin.y + cursor.bottom.roundToInt()
            )
        } else {
            IntOffset.Zero
        }
        
        val handleTapBounds = run {
            val touchMarginPx = with(density) { MESSAGE_HANDLE_TOUCH_MARGIN_DP.dp.roundToPx() }
            val touchExpandPx = with(density) { MESSAGE_HANDLE_TOUCH_EXPAND_DP.dp.roundToPx() }
            val touchSize = IntSize(
                width = handleSize.width + touchMarginPx * 2,
                height = handleSize.height + touchExpandPx
            )
            
            listOf(
                IntRect(
                    IntOffset(
                        startHandlePosition.x - touchMarginPx,
                        startHandlePosition.y - touchExpandPx
                    ),
                    touchSize
                ),
                IntRect(
                    IntOffset(
                        endHandlePosition.x - touchMarginPx,
                        endHandlePosition.y - touchExpandPx
                    ),
                    touchSize
                )
            )
        }
        
        val onCatcherTap: (Offset) -> Unit = { offset ->
            val tapped = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            
            if (handleTapBounds.none { it.contains(tapped) }) {
                selectionRange = null
            }
        }
        
        Popup(
            popupPositionProvider = MessageTextTopLeftPositionProvider,
            properties = PopupProperties(focusable = false, clippingEnabled = false)
        ) {
            Layout(
                content = {
                    val currentOnCatcherTap by rememberUpdatedState(onCatcherTap)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    currentOnCatcherTap(offset)
                                }
                            }
                    ) {
                        if (layout != null) {
                            MessageSelectionHandle(
                                position = startHandlePosition,
                                handleSize = handleSize,
                                drawable = handleDrawable,
                                tint = handleTint,
                                textOrigin = textOrigin,
                                textLayout = layout,
                                selectionRange = selection,
                                isStartHandle = true,
                                onDraggingChange = { isHandleDragging = it },
                                onSelectionChange = { range ->
                                    val trimmed = trimmedSelectionRange(annotatedString.text, range)
                                    
                                    selectionRange = if (trimmed.collapsed) null else trimmed
                                }
                            )
                            
                            MessageSelectionHandle(
                                position = endHandlePosition,
                                handleSize = handleSize,
                                drawable = handleDrawable,
                                tint = handleTint,
                                textOrigin = textOrigin,
                                textLayout = layout,
                                selectionRange = selection,
                                isStartHandle = false,
                                onDraggingChange = { isHandleDragging = it },
                                onSelectionChange = { range ->
                                    val trimmed = trimmedSelectionRange(annotatedString.text, range)
                                    
                                    selectionRange = if (trimmed.collapsed) null else trimmed
                                }
                            )
                        }
                    }
                },
                measurePolicy = ZeroIntrinsicsMeasurePolicy
            )
        }
        
        if (!isHandleDragging) {
            MessageTextSelectionMenu(
                selectionBoundsProvider = {
                    messageSelectionWindowRect(textOrigin, textLayout, selection)
                },
                canSelectAll = selection.min > 0 || selection.max < annotatedString.length,
                onSelectAllClick = {
                    val trimmed =
                        trimmedSelectionRange(
                            annotatedString.text,
                            TextRange(0, annotatedString.length)
                        )
                    
                    if (!trimmed.collapsed) {
                        selectTextRange(trimmed)
                    }
                },
                onCopyClick = {
                    val range = selectionRange
                    
                    if (range != null) {
                        onCopySelected?.invoke(
                            annotatedString.text.substring(
                                range.min,
                                range.max.coerceAtMost(annotatedString.length)
                            )
                        )
                    }
                    
                    selectionRange = null
                }
            )
        }
    }
}

@Composable
private fun MessageSelectionHandle(
    position: IntOffset,
    handleSize: IntSize,
    drawable: Drawable?,
    tint: Color,
    textOrigin: IntOffset,
    textLayout: TextLayoutResult,
    selectionRange: TextRange,
    isStartHandle: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    onSelectionChange: (TextRange) -> Unit
) {
    val density = LocalDensity.current
    val touchMarginPx = with(density) { MESSAGE_HANDLE_TOUCH_MARGIN_DP.dp.roundToPx() }
    val touchExpandPx = with(density) { MESSAGE_HANDLE_TOUCH_EXPAND_DP.dp.roundToPx() }
    val touchWidthDp = with(density) { (handleSize.width + touchMarginPx * 2).toDp() }
    val touchHeightDp = with(density) { (handleSize.height + touchExpandPx).toDp() }
    val tintedDrawable = remember(drawable, tint) {
        drawable?.mutate()?.apply { setTint(tint.toArgb()) }
    }
    val anchorOffset = Offset(
        x = touchMarginPx + handleSize.width / 2f,
        y = touchExpandPx.toFloat()
    )
    val currentPosition by rememberUpdatedState(position)
    val currentTextOrigin by rememberUpdatedState(textOrigin)
    val currentTextLayout by rememberUpdatedState(textLayout)
    val currentSelection by rememberUpdatedState(selectionRange)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)
    val grabOffset = remember { mutableStateOf(Offset.Zero) }
    var previousLine by remember { mutableIntStateOf(-1) }
    
    Box(
        modifier = Modifier
            .offset {
                IntOffset(position.x - touchMarginPx, position.y - touchExpandPx)
            }
            .size(touchWidthDp, touchHeightDp)
            .pointerInput(isStartHandle, handleSize) {
                detectDragGestures(
                    onDragStart = { start ->
                        grabOffset.value = start - anchorOffset
                        previousLine = currentTextLayout.getLineForOffset(
                            if (isStartHandle) currentSelection.min else currentSelection.max
                        )
                        
                        currentOnDraggingChange(true)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        
                        val finger = Offset(
                            x = currentPosition.x + change.position.x,
                            y = currentPosition.y + change.position.y
                        )
                        val anchor = finger - grabOffset.value
                        val anchorX = anchor.x - currentTextOrigin.x
                        val anchorY = anchor.y - currentTextOrigin.y
                        val trueLine = currentTextLayout.getLineForVerticalPosition(anchorY)
                            .coerceIn(0, currentTextLayout.lineCount - 1)
                        val lineHeight = currentTextLayout.getLineBottom(previousLine) -
                                currentTextLayout.getLineTop(previousLine)
                        val lineChangeSlop = lineHeight * LINE_CHANGE_SLOP_RATIO
                        val line = when {
                            abs(trueLine - previousLine) >= 2 -> trueLine
                            trueLine > previousLine &&
                                    anchorY >= currentTextLayout.getLineBottom(previousLine) + lineChangeSlop -> trueLine
                            
                            trueLine < previousLine &&
                                    anchorY <= currentTextLayout.getLineTop(previousLine) - lineChangeSlop -> trueLine
                            
                            else -> previousLine
                        }
                        previousLine = line
                        val lineCenterY = (currentTextLayout.getLineTop(line) +
                                currentTextLayout.getLineBottom(line)) / 2f
                        val offset = currentTextLayout.getOffsetForPosition(
                            Offset(x = anchorX, y = lineCenterY)
                        )
                        val range = currentSelection
                        val newRange = if (isStartHandle) {
                            TextRange(minOf(offset, range.max), range.max)
                        } else {
                            TextRange(range.min, maxOf(offset, range.min))
                        }
                        
                        if (newRange != range) {
                            currentOnSelectionChange(newRange)
                        }
                    },
                    onDragEnd = { currentOnDraggingChange(false) },
                    onDragCancel = { currentOnDraggingChange(false) }
                )
            }
            .drawBehind {
                tintedDrawable?.let { handle ->
                    handle.setBounds(
                        touchMarginPx,
                        touchExpandPx,
                        touchMarginPx + handleSize.width,
                        touchExpandPx + handleSize.height
                    )
                    handle.draw(drawContext.canvas.nativeCanvas)
                }
            }
    )
}

private fun resolveSelectionHandleDrawable(context: Context, attribute: Int): Drawable? {
    val typedValue = TypedValue()
    
    return if (context.theme.resolveAttribute(attribute, typedValue, true)) {
        ContextCompat.getDrawable(context, typedValue.resourceId)
    } else {
        null
    }
}

private fun selectionHandleSize(drawable: Drawable?, density: Density): IntSize {
    return if (drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
        IntSize(drawable.intrinsicWidth, drawable.intrinsicHeight)
    } else {
        IntSize(
            with(density) { MESSAGE_HANDLE_WIDTH_DP.dp.roundToPx() },
            with(density) { MESSAGE_HANDLE_HEIGHT_DP.dp.roundToPx() }
        )
    }
}

private fun trimmedSelectionRange(plain: String, range: TextRange): TextRange {
    var min = range.min.coerceIn(0, plain.length)
    var max = range.max.coerceIn(0, plain.length)
    
    while (min < max && plain[min].isWhitespace()) {
        min += 1
    }
    
    while (max > min && plain[max - 1].isWhitespace()) {
        max -= 1
    }
    
    return TextRange(min, max)
}

private fun messageSelectionWindowRect(
    textOrigin: IntOffset,
    layout: TextLayoutResult?,
    range: TextRange
): IntRect {
    if (layout == null) {
        return IntRect.Zero
    }
    
    val bounds: Rect = layout.getPathForRange(range.min, range.max).getBounds()
    
    return IntRect(
        left = textOrigin.x + bounds.left.roundToInt(),
        top = textOrigin.y + bounds.top.roundToInt(),
        right = textOrigin.x + bounds.right.roundToInt(),
        bottom = textOrigin.y + bounds.bottom.roundToInt()
    )
}

@Composable
private fun MessageTextSelectionMenu(
    selectionBoundsProvider: () -> IntRect,
    canSelectAll: Boolean,
    onSelectAllClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    val density = LocalDensity.current
    val gapPx = with(density) { MESSAGE_MENU_GAP_DP.dp.roundToPx() }
    val marginPx = with(density) { MESSAGE_MENU_MARGIN_DP.dp.roundToPx() }
    
    val positionProvider = remember(gapPx, marginPx) {
        SelectionMenuPositionProvider(
            selectionBounds = selectionBoundsProvider,
            gapPx = gapPx,
            marginPx = marginPx
        )
    }
    
    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false)
    ) {
        Layout(
            content = {
                Box(modifier = Modifier.padding(MESSAGE_MENU_SHADOW_PADDING_DP.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = MESSAGE_MENU_SHADOW_ELEVATION_DP.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canSelectAll) {
                                MessageSelectionMenuItem(
                                    labelRes = R.string.selection_select_all,
                                    onClick = onSelectAllClick
                                )
                            }
                            
                            MessageSelectionMenuItem(
                                labelRes = R.string.selection_copy,
                                onClick = onCopyClick
                            )
                        }
                    }
                }
            },
            measurePolicy = ZeroIntrinsicsMeasurePolicy
        )
    }
}

private val ZeroIntrinsicsMeasurePolicy = object : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = placeables.maxOfOrNull { it.height } ?: 0
        
        return layout(width, height) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
    
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int = 0
    
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int = 0
    
    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int = 0
    
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int = 0
}

@Composable
private fun MessageSelectionMenuItem(
    labelRes: Int,
    onClick: () -> Unit
) {
    Text(
        text = stringResource(labelRes),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

private object MessageTextTopLeftPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ) = IntOffset.Zero
}

private data class TextEntity(
    val start: Int,
    val end: Int,
    val value: String,
    val linkValue: String,
    val type: String
)

private fun AnnotatedString.Builder.appendMessageText(
    text: String,
    formatSpans: List<MessageFormatSpan>,
    region: String,
    linkColor: Color,
    pressedColor: Color,
    onLinkClicked: ((String) -> Unit)?,
    onUsernameClicked: ((String) -> Unit)?,
    onEmailClicked: ((String) -> Unit)?,
    onPhoneClicked: ((String) -> Unit)?
) {
    var lastIndex = 0
    
    val emailMatches = RegexPatterns.EMAIL_IN_TEXT.findAll(text).toList()
    val emailRanges = emailMatches.map { it.range }
    
    val urlMatches = RegexPatterns.URL.findAll(text)
        .filterNot { url -> emailRanges.any { url.range.first <= it.last && it.first <= url.range.last } }
        .toList()
    val urlRanges = urlMatches.map { it.range }
    
    val mentionMatches = RegexPatterns.MENTION.findAll(text).toList()
    
    val phoneEntities = PhoneLinks.findInText(text, region)
        .filterNot { phone ->
            (emailRanges + urlRanges).any { range ->
                phone.range.first <= range.last && range.first <= phone.range.last
            }
        }
        .map { phone ->
            TextEntity(
                start = phone.range.first,
                end = phone.range.last + 1,
                value = text.substring(phone.range.first, phone.range.last + 1),
                linkValue = phone.e164,
                type = "phone"
            )
        }
    
    val emailEntities = emailMatches.map { match ->
        TextEntity(
            start = match.range.first,
            end = match.range.last + 1,
            value = match.value,
            linkValue = match.value,
            type = "email"
        )
    }
    
    val urlEntities = urlMatches.map { match ->
        TextEntity(
            start = match.range.first,
            end = match.range.last + 1,
            value = match.value,
            linkValue = LinkNormalizer.normalize(match.value),
            type = "url"
        )
    }
    
    val mentionEntities = mentionMatches.map { match ->
        TextEntity(
            start = match.range.first,
            end = match.range.last + 1,
            value = match.value,
            linkValue = match.value,
            type = "username"
        )
    }
    
    val entities = (emailEntities + urlEntities + mentionEntities + phoneEntities)
        .sortedBy { it.start }
    
    if (entities.isEmpty()) {
        appendFormattedMessageRange(text, 0, text.length, formatSpans)
        
        return
    }
    
    entities.forEach { entity ->
        if (entity.start < lastIndex) return@forEach
        
        if (entity.start > lastIndex) {
            appendFormattedMessageRange(text, lastIndex, entity.start, formatSpans)
        }
        
        when (entity.type) {
            "url" if onLinkClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onLinkClicked(entity.linkValue)
                        }
                    )
                ) {
                    appendFormattedMessageRange(text, entity.start, entity.end, formatSpans)
                }
            }
            
            "email" if onEmailClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onEmailClicked(entity.linkValue)
                        }
                    )
                ) {
                    appendFormattedMessageRange(text, entity.start, entity.end, formatSpans)
                }
            }
            
            "phone" if onPhoneClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onPhoneClicked(entity.linkValue)
                        }
                    )
                ) {
                    appendFormattedMessageRange(text, entity.start, entity.end, formatSpans)
                }
            }
            
            "username" if onUsernameClicked != null -> {
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = entity.value,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor),
                            pressedStyle = SpanStyle(background = pressedColor)
                        ),
                        linkInteractionListener = {
                            onUsernameClicked(entity.linkValue)
                        }
                    )
                ) {
                    appendFormattedMessageRange(text, entity.start, entity.end, formatSpans)
                }
            }
            
            else -> {
                val style =
                    if (entity.type == "url" || entity.type == "email" || entity.type == "phone") {
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    } else {
                        SpanStyle(color = linkColor)
                    }
                
                withStyle(style = style) {
                    appendFormattedMessageRange(text, entity.start, entity.end, formatSpans)
                }
            }
        }
        
        lastIndex = entity.end
    }
    
    if (lastIndex < text.length) {
        appendFormattedMessageRange(text, lastIndex, text.length, formatSpans)
    }
}

private val CUSTOM_EMOJI_SIZE = 18.sp
private const val LINK_PRESSED_ALPHA = 0.4f
private const val MESSAGE_SELECTION_ALPHA = 0.3f
private const val MESSAGE_MENU_GAP_DP = 4
private const val MESSAGE_MENU_MARGIN_DP = 8
private const val MESSAGE_MENU_SHADOW_PADDING_DP = 8
private const val MESSAGE_MENU_SHADOW_ELEVATION_DP = 6
private const val MESSAGE_HANDLE_WIDTH_DP = 22
private const val MESSAGE_HANDLE_HEIGHT_DP = 30
private const val MESSAGE_HANDLE_TOUCH_MARGIN_DP = 8
private const val MESSAGE_HANDLE_TOUCH_EXPAND_DP = 16
private const val LINE_CHANGE_SLOP_RATIO = 0.8f
