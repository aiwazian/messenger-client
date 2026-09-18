/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.media.audiofx.Equalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerInfo(
    val bandCount: Int,
    val centerFrequenciesMilliHz: List<Int>,
    val minLevelMb: Int,
    val maxLevelMb: Int
)

@Singleton
class EqualizerManager @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var effect: Equalizer? = null
    private var currentLevels: List<Int> = List(DataStoreManager.EQUALIZER_BAND_COUNT) { 0 }
    private var levelsLoaded = false

    private val _info = MutableStateFlow<EqualizerInfo?>(null)
    val info = _info.asStateFlow()

    private val _bandLevels = MutableStateFlow(currentLevels)
    val bandLevels = _bandLevels.asStateFlow()

    fun attach(audioSessionId: Int) {
        detach()

        val equalizer = runCatching { Equalizer(PRIORITY, audioSessionId) }.getOrNull()
        if (equalizer == null) {
            _info.value = null
            return
        }
        effect = equalizer
        runCatching { equalizer.enabled = true }

        scope.launch {
            loadBandLevels()
            applyBandLevels()
        }

        val bandCount = minOf(equalizer.numberOfBands.toInt(), DataStoreManager.EQUALIZER_BAND_COUNT)
        val range = runCatching { equalizer.bandLevelRange }.getOrNull()
        _info.value = EqualizerInfo(
            bandCount = bandCount,
            centerFrequenciesMilliHz = (0 until bandCount).map { band ->
                runCatching { equalizer.getCenterFreq(band.toShort()) }.getOrDefault(0)
            },
            minLevelMb = range?.first()?.toInt() ?: DEFAULT_MIN_LEVEL_MB,
            maxLevelMb = range?.last()?.toInt() ?: DEFAULT_MAX_LEVEL_MB
        )
    }

    fun detach() {
        effect?.let { equalizer -> runCatching { equalizer.release() } }
        effect = null
        _info.value = null
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        if (band !in currentLevels.indices) return

        currentLevels = currentLevels.toMutableList().also { it[band] = levelMb }
        _bandLevels.value = currentLevels
        applyBandLevel(band)
    }

    fun persistBandLevels() {
        scope.launch { dataStoreManager.saveEqualizerBandLevels(currentLevels) }
    }

    private suspend fun loadBandLevels() {
        if (levelsLoaded) return
        levelsLoaded = true

        val stored = dataStoreManager.getEqualizerBandLevels().first()
        currentLevels = List(DataStoreManager.EQUALIZER_BAND_COUNT) { band ->
            stored.getOrElse(band) { 0 }
        }
        _bandLevels.value = currentLevels
    }

    private fun applyBandLevels() {
        currentLevels.indices.forEach(::applyBandLevel)
    }

    private fun applyBandLevel(band: Int) {
        val equalizer = effect ?: return
        if (band >= equalizer.numberOfBands) return
        runCatching { equalizer.setBandLevel(band.toShort(), currentLevels[band].toShort()) }
    }

    companion object {
        private const val PRIORITY = 0
        private const val DEFAULT_MIN_LEVEL_MB = -1500
        private const val DEFAULT_MAX_LEVEL_MB = 1500
    }
}
