package com.aiwazian.messenger.ui.element

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NavigationController(startScreen: @Composable () -> Unit) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val screenStack by navViewModel.screenStack.collectAsState()
    val offsetStack by navViewModel.offsetStack.collectAsState()
    
    val tweenDurationMillis = navViewModel.tweenDurationMillis
    
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    
    navViewModel.screenWidth = screenWidthPx
    
    val scope = rememberCoroutineScope()
    
    navViewModel.scope = scope
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        startScreen()
        
        screenStack.forEachIndexed { index, screenEntry ->
            val offsetX = offsetStack[index]
            val isTop = index == screenStack.lastIndex
            
            val canGoBackBySwipe = screenEntry.canGoBackBySwipe
            
            BackHandler(enabled = screenStack.isNotEmpty() && canGoBackBySwipe) {
                navViewModel.removeLastScreenInStack()
            }
            
            LaunchedEffect(key1 = screenEntry.content) {
                keyboardController?.hide()
                offsetX.animateTo(
                    0f,
                    tween(tweenDurationMillis)
                )
            }
            
            val backgroundAlpha = ((1f - (offsetX.value / screenWidthPx)).coerceIn(
                0f,
                1f
            )) * 0.5f
            
            BoxShadow(
                index,
                backgroundAlpha
            )
            
            val draggableState = rememberDraggableState { delta ->
                keyboardController?.hide()
                scope.launch {
                    offsetX.snapTo((offsetX.value + delta).coerceAtLeast(0f))
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            offsetX.value.roundToInt(),
                            0
                        )
                    }
                    .zIndex(index + 0.2f)
                    .then(
                        if (isTop && canGoBackBySwipe) {
                            Modifier.draggable(
                                orientation = Orientation.Horizontal,
                                state = draggableState,
                                onDragStopped = {
                                    scope.launch {
                                        if (offsetX.value > screenWidthPx / 4) {
                                            navViewModel.removeLastScreenInStack()
                                        } else {
                                            offsetX.animateTo(
                                                0f,
                                                tween(tweenDurationMillis)
                                            )
                                        }
                                    }
                                })
                        } else Modifier
                    )
            ) {
                screenEntry.content()
            }
        }
    }
}

@Composable
private fun BoxShadow(
    index: Int,
    backgroundAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .zIndex(zIndex = index + 0.1f)
            .clickable(
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                }) { })
}