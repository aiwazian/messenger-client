package com.aiwazian.messenger.viewModels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.data.ScreenEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NavigationViewModel() : ViewModel() {
    
    private val _screenStack = MutableStateFlow<List<ScreenEntry>>(emptyList())
    val screenStack = _screenStack.asStateFlow()
    
    private val _offsetStack =
        MutableStateFlow<List<Animatable<Float, AnimationVector1D>>>(emptyList())
    
    val offsetStack = _offsetStack.asStateFlow()
    
    val tweenDurationMillis = 200
    
    var screenWidth = 0f
    
    lateinit var scope: CoroutineScope
    
    fun addScreenInStack(
        canGoBackBySwipe: Boolean = true,
        screen: @Composable () -> Unit
    ) {
        _screenStack.update {
            it + ScreenEntry(
                screen,
                canGoBackBySwipe
            )
        }
        _offsetStack.update { it + Animatable(screenWidth) }
    }
    
    fun removeLastScreenInStack() {
        if (_screenStack.value.isEmpty() && _offsetStack.value.isEmpty()) {
            return
        }
        
        val screenStack = _screenStack.value.toMutableList()
        val offsetStack = _offsetStack.value.toMutableList()
        
        val lastIndex = screenStack.lastIndex
        
        if (lastIndex >= offsetStack.size) {
            return
        }
        
        if (lastIndex < screenStack.size) {
            viewModelScope.launch {
                withContext(AndroidUiDispatcher.Main) {
                    offsetStack.last().animateTo(
                        screenWidth,
                        tween(tweenDurationMillis)
                    )
                    
                    _screenStack.update { it.dropLast(1) }
                    _offsetStack.update { it.dropLast(1) }
                }
            }
        }
    }
    
    fun goToMain() {
        _screenStack.update { emptyList() }
        _offsetStack.update { emptyList() }
    }
}