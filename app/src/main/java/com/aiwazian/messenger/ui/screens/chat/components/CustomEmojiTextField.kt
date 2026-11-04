/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.PopupMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.extensions.findActivity

@Composable
internal fun CustomEmojiTextField(
    text: String,
    hint: String,
    onTextChange: (String) -> Unit,
    onResolveEmoji: suspend (Long) -> CustomEmoji?,
    modifier: Modifier = Modifier,
    onReceiveUris: ((List<Uri>) -> Unit)? = null,
    onViewReady: (EditText) -> Unit = {},
    onSelectionChanged: ((IntRect?) -> Unit)? = null,
    isFormattingEnabled: Boolean = false
) {
    val density = LocalDensity.current

    var inputView by remember { mutableStateOf<EditText?>(null) }
    val textSync = remember { CustomEmojiTextSync() }

    val currentOnTextChange by rememberUpdatedState(onTextChange)
    val currentOnReceiveUris by rememberUpdatedState(onReceiveUris)
    val currentOnViewReady by rememberUpdatedState(onViewReady)
    val currentOnSelectionChanged by rememberUpdatedState(onSelectionChanged)
    val currentIsFormattingEnabled by rememberUpdatedState(isFormattingEnabled)
    
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val cursorColor = MaterialTheme.colorScheme.primary.toArgb()
    val selectionColor = MaterialTheme.colorScheme.primary
        .copy(alpha = INPUT_SELECTION_ALPHA)
        .toArgb()
    val verticalPadding = with(density) { 12.dp.roundToPx() }
    val cursorWidth = with(density) { 2.dp.roundToPx() }
    
    val currentText by rememberUpdatedState(text)

    LaunchedEffect(inputView, isFormattingEnabled) {
        val view = inputView ?: return@LaunchedEffect

        snapshotFlow { currentText }.collect { value ->
            if (textSync.hasReported(value)) {
                return@collect
            }

            if (currentIsFormattingEnabled) {
                if (toMarkedText(view.text) == value) {
                    textSync.rememberReported(value)

                    return@collect
                }

                textSync.lastReportedText = value
                textSync.rememberReported(value)

                val content = buildFormattedFieldContent(view, value, onResolveEmoji)

                view.setText(content)

                return@collect
            }

            if (CustomEmojiText.serialize(view.text) == value) {
                textSync.rememberReported(value)

                return@collect
            }

            val content = buildCustomEmojiText(view, value, onResolveEmoji)

            textSync.isApplyingExternalText = true
            textSync.lastReportedText = value
            textSync.rememberReported(value)

            view.setText(content)
            view.setSelection(view.text.length)

            textSync.isApplyingExternalText = false
        }
    }
    
    AndroidView(
        factory = { viewContext ->
            MessageInputEditText(viewContext).apply {
                background = null
                setPadding(0, verticalPadding, 0, verticalPadding)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, INPUT_TEXT_SIZE_SP)
                imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                minLines = 1
                maxLines = INPUT_MAX_LINES
                
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) = Unit
                    
                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) = Unit
                    
                    override fun afterTextChanged(s: Editable?) {
                        if (s == null) {
                            return
                        }

                        if (currentIsFormattingEnabled) {
                            val value = toMarkedText(s)

                            if (value != textSync.lastReportedText) {
                                textSync.lastReportedText = value
                                textSync.rememberReported(value)

                                currentOnTextChange(value)
                            }

                            return
                        }

                        if (textSync.isApplyingExternalText) {
                            return
                        }

                        val value = CustomEmojiText.serialize(s)

                        if (value == textSync.lastReportedText) {
                            return
                        }

                        textSync.lastReportedText = value
                        textSync.rememberReported(value)

                        currentOnTextChange(value)
                    }
                })
                
                ViewCompat.setOnReceiveContentListener(
                    this,
                    arrayOf(INPUT_IMAGE_MIME_TYPE)
                ) { _, payload ->
                    val onUris = currentOnReceiveUris
                    val clip = payload.clip
                    val uris = mutableListOf<Uri>()
                    
                    for (index in 0 until clip.itemCount) {
                        clip.getItemAt(index).uri?.let { uris.add(it) }
                    }
                    
                    if (onUris == null || uris.isEmpty()) {
                        payload
                    } else {
                        onUris(uris)
                        
                        null
                    }
                }
                
                inputView = this
                
                currentOnViewReady(this)
            }
        },
        modifier = modifier,
        update = { view ->
            view.hint = hint
            view.setTextColor(textColor)
            view.setHintTextColor(hintColor)
            view.highlightColor = selectionColor
            view.textCursorDrawable = GradientDrawable().apply {
                setColor(cursorColor)
                setSize(cursorWidth, 0)
            }
            view.textSelectHandle?.mutate()?.setTint(cursorColor)
            view.textSelectHandleLeft?.mutate()?.setTint(cursorColor)
            view.textSelectHandleRight?.mutate()?.setTint(cursorColor)
            view.isSelectionToolbarSuppressed = currentOnSelectionChanged != null
            view.onSelectionBoundsChanged = currentOnSelectionChanged
        })
}

internal fun focusMessageInput(view: EditText) {
    view.requestFocus()
    
    view.post {
        view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.ime())
        }
    }
}

internal fun hideKeyboardKeepFocus(view: EditText) {
    view.context.findActivity()?.window?.let { window ->
        WindowCompat.getInsetsController(window, view).hide(WindowInsetsCompat.Type.ime())
    }
}

internal class MessageInputEditText(context: Context) : EditText(context) {
    var onSelectionBoundsChanged: ((IntRect?) -> Unit)? = null

    var isSelectionToolbarSuppressed = false

    init {
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            reportSelectionBounds()
        }
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        if (isSelectionToolbarSuppressed && type == ActionMode.TYPE_FLOATING && hasSelection()) {
            return EmptyActionMode(context)
        }

        return super.startActionMode(callback, type)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        reportSelectionBounds(selStart, selEnd)
    }

    override fun onScrollChanged(
        horizontal: Int,
        vertical: Int,
        oldHorizontal: Int,
        oldVertical: Int
    ) {
        super.onScrollChanged(horizontal, vertical, oldHorizontal, oldVertical)

        reportSelectionBounds()
    }

    private fun reportSelectionBounds(
        selStart: Int = selectionStart,
        selEnd: Int = selectionEnd
    ) {
        onSelectionBoundsChanged?.invoke(computeSelectionBounds(selStart, selEnd))
    }

    private fun computeSelectionBounds(selStart: Int, selEnd: Int): IntRect? {
        if (selStart == selEnd) return null

        val textLayout = layout ?: return null

        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        val startLine = textLayout.getLineForOffset(start)
        val endLine = textLayout.getLineForOffset(end)
        val left = minOf(
            textLayout.getPrimaryHorizontal(start),
            textLayout.getPrimaryHorizontal(end)
        ) - scrollX
        val right = maxOf(
            textLayout.getPrimaryHorizontal(start),
            textLayout.getPrimaryHorizontal(end)
        ) - scrollX
        val top = textLayout.getLineTop(startLine) - scrollY
        val bottom = textLayout.getLineBottom(endLine) - scrollY

        val windowLocation = intArrayOf(0, 0)
        getLocationInWindow(windowLocation)

        return IntRect(
            left = left.toInt() + windowLocation[0],
            top = top + windowLocation[1],
            right = right.toInt() + windowLocation[0],
            bottom = bottom + windowLocation[1]
        )
    }
}

private class EmptyActionMode(context: Context) : ActionMode() {
    private val emptyMenu = PopupMenu(context, null).menu
    private val menuInflater = MenuInflater(context)

    override fun setTitle(title: CharSequence) = Unit

    override fun setTitle(resId: Int) = Unit

    override fun setSubtitle(subtitle: CharSequence) = Unit

    override fun setSubtitle(resId: Int) = Unit

    override fun setCustomView(view: View) = Unit

    override fun invalidate() = Unit

    override fun finish() = Unit

    override fun getMenu(): Menu = emptyMenu

    override fun getTitle(): CharSequence = ""

    override fun getSubtitle(): CharSequence = ""

    override fun getCustomView(): View? = null

    override fun getMenuInflater(): MenuInflater = menuInflater
}

private class CustomEmojiTextSync {
    var isApplyingExternalText = false
    var lastReportedText = ""
    private val reportedValues = ArrayDeque<String>()

    fun hasReported(value: String): Boolean = reportedValues.contains(value)

    fun rememberReported(value: String) {
        if (value.isEmpty()) {
            return
        }

        reportedValues.remove(value)
        reportedValues.addLast(value)

        while (reportedValues.size > REPORTED_HISTORY_LIMIT) {
            reportedValues.removeFirst()
        }
    }
}

private const val INPUT_TEXT_SIZE_SP = 16f
private const val INPUT_MAX_LINES = 5
private const val INPUT_SELECTION_ALPHA = 0.4f
private const val INPUT_IMAGE_MIME_TYPE = "image/*"
private const val REPORTED_HISTORY_LIMIT = 20
