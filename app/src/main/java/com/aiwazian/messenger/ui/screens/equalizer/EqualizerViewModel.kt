/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.utils.EqualizerInfo
import com.aiwazian.messenger.utils.EqualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerManager: EqualizerManager
) : ViewModel() {

    val info: StateFlow<EqualizerInfo?> = equalizerManager.info

    val bandLevels: StateFlow<List<Int>> = equalizerManager.bandLevels

    fun setBandLevel(band: Int, levelMb: Int) {
        equalizerManager.setBandLevel(band, levelMb)
    }

    fun persistBandLevels() {
        equalizerManager.persistBandLevels()
    }

    fun resetBandLevels() {
        equalizerManager.resetBandLevels()
    }
}
