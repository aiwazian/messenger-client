/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawToolViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _drawColor = MutableStateFlow(Color(DataStoreManager.DEFAULT_DRAW_COLOR))
    val drawColor = _drawColor.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.getDrawColor().collect { argb ->
                _drawColor.value = Color(argb)
            }
        }
    }

    fun setDrawColor(color: Color) {
        _drawColor.value = color

        viewModelScope.launch {
            dataStoreManager.saveDrawColor(color.toArgb().toLong() and DRAW_COLOR_MASK)
        }
    }

    private companion object {
        const val DRAW_COLOR_MASK = 0xFFFFFFFFL
    }
}
