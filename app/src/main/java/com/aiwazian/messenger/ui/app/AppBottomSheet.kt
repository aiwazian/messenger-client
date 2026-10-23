/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    stickyFooter: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val collapseZonePx = with(density) { 400.dp.toPx() }
    
    var containerHeight by remember { mutableIntStateOf(0) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    var stickyContentHeight by remember { mutableIntStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier.then(
            if (stickyFooter != null) {
                Modifier.layout { measurable, constraints ->
                    containerHeight = constraints.maxHeight
                    val placeable = measurable.measure(constraints)
                    sheetHeight = placeable.height
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            } else {
                Modifier
            }),
        contentWindowInsets = { WindowInsets() }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val stickyHeight = with(density) { stickyContentHeight.toDp() }
            val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
            val bottomBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
            val bottomPadding = if (stickyFooter == null) {
                stickyHeight + bottomBarHeight + 10.dp
            } else stickyHeight
            
            val contentPadding = PaddingValues(
                start = 10.dp,
                top = statusBarHeight,
                end = 10.dp,
                bottom = bottomPadding.coerceAtLeast(10.dp)
            )
            
            Column {
                content(contentPadding)
            }
            
            if (stickyFooter != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { stickyContentHeight = it.height }
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .stickyBarTranslation(
                            sheetState = sheetState,
                            containerHeight = containerHeight,
                            sheetHeight = sheetHeight,
                            collapseZonePx = collapseZonePx
                        )) {
                    stickyFooter()
                }
            }
        }
    }
}

object AppBottomSheetFooterActions {
    
    @Composable
    fun PrimaryButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        color: Color = MaterialTheme.colorScheme.primary
    ) {
        TextButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp),
            enabled = enabled,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.textButtonColors(
                contentColor = color
            )
        ) {
            Text(
                text = text, fontSize = 16.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold
            )
        }
    }
    
    @Composable
    fun DestructiveButton(
        text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true
    ) {
        PrimaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            color = MaterialTheme.colorScheme.error
        )
    }
    
    @Composable
    inline fun Custom(content: @Composable () -> Unit) = content()
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.stickyBarTranslation(
    sheetState: SheetState, containerHeight: Int, sheetHeight: Int, collapseZonePx: Float
): Modifier = graphicsLayer {
    val offset = sheetState.offsetOrNull() ?: return@graphicsLayer
    if (containerHeight == 0 || sheetHeight == 0) return@graphicsLayer
    
    val hidden = offset + sheetHeight - containerHeight
    val limit = (sheetHeight - collapseZonePx).coerceAtLeast(0f)
    translationY = -hidden.coerceIn(0f, limit)
}

@OptIn(ExperimentalMaterial3Api::class)
private fun SheetState.offsetOrNull(): Float? = try {
    requireOffset()
} catch (_: IllegalStateException) {
    null
}
