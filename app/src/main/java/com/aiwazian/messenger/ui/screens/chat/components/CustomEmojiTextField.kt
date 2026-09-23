/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    onViewReady: (EditText) -> Unit = {}
) {
    val density = LocalDensity.current
    
    var inputView by remember { mutableStateOf<EditText?>(null) }
    val textSync = remember { CustomEmojiTextSync() }
    
    val currentOnTextChange by rememberUpdatedState(onTextChange)
    val currentOnReceiveUris by rememberUpdatedState(onReceiveUris)
    val currentOnViewReady by rememberUpdatedState(onViewReady)
    
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val cursorColor = MaterialTheme.colorScheme.primary.toArgb()
    val selectionColor = MaterialTheme.colorScheme.primary
        .copy(alpha = INPUT_SELECTION_ALPHA)
        .toArgb()
    val verticalPadding = with(density) { 12.dp.roundToPx() }
    val cursorWidth = with(density) { 2.dp.roundToPx() }
    
    LaunchedEffect(inputView, text) {
        val view = inputView ?: return@LaunchedEffect
        
        if (CustomEmojiText.serialize(view.text) == text) {
            return@LaunchedEffect
        }
        
        val content = buildCustomEmojiText(view, text, onResolveEmoji)
        
        textSync.isApplyingExternalText = true
        textSync.lastReportedText = text
        
        view.setText(content)
        view.setSelection(view.text.length)
        
        textSync.isApplyingExternalText = false
    }
    
    AndroidView(
        factory = { viewContext ->
            EditText(viewContext).apply {
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
                        if (textSync.isApplyingExternalText || s == null) {
                            return
                        }
                        
                        val value = CustomEmojiText.serialize(s)
                        
                        if (value == textSync.lastReportedText) {
                            return
                        }
                        
                        textSync.lastReportedText = value
                        
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

private class CustomEmojiTextSync {
    var isApplyingExternalText = false
    var lastReportedText = ""
}

private const val INPUT_TEXT_SIZE_SP = 16f
private const val INPUT_MAX_LINES = 5
private const val INPUT_SELECTION_ALPHA = 0.4f
private const val INPUT_IMAGE_MIME_TYPE = "image/*"
