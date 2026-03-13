package com.aiwazian.messenger.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DialogController {
    
    var isVisible by mutableStateOf(false)
        private set
    
    fun show() {
        isVisible = true
    }
    
    fun hide() {
        isVisible = false
    }
}