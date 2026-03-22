/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdEventListener
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdViewModel @Inject constructor() : ViewModel() {
    
    private val reloadInterval = 60_000L
    
    private var nativeAdLoader: NativeAdLoader? = null
    
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded = _isAdLoaded.asStateFlow()
    
    var nativeAd: NativeAd? = null
        private set
    
    private var _isImpressed = false
    
    private var loadStartTime: Long = 0
    
    init {
        viewModelScope.launch {
            while (true) {
                while (!(elapsedTime() >= reloadInterval && _isImpressed)) {
                    delay(1000)
                }
                
                if (_isAdLoaded.value) {
                    _isAdLoaded.value = false
                    loadAd()
                }
            }
        }
    }
    
    fun initialize(context: Context) {
        if (nativeAdLoader == null) {
            nativeAdLoader = createNativeAdLoader(context)
            loadAd()
        }
    }
    
    private fun createNativeAdLoader(context: Context): NativeAdLoader {
        return NativeAdLoader(context).apply {
            setNativeAdLoadListener(object : NativeAdLoadListener {
                override fun onAdLoaded(nativeAd: NativeAd) {
                    this@AdViewModel.nativeAd = nativeAd
                    this@AdViewModel.nativeAd?.setNativeAdEventListener(
                        NativeAdEventLogger(onImpressed = {
                            _isImpressed = true
                        })
                    )
                    
                    _isAdLoaded.update { true }
                    
                    loadStartTime = System.currentTimeMillis()
                    
                    Log.d("YandexAds", "Ad loaded")
                }
                
                override fun onAdFailedToLoad(error: AdRequestError) {
                    loadAd()
                    
                    Log.e("YandexAds", "Ad failed to load: ${error.description}")
                }
            })
        }
    }
    
    private fun loadAd() {
        nativeAdLoader?.loadAd(NativeAdRequestConfiguration.Builder("R-M-15520718-2").build())
        _isAdLoaded.update { false }
        _isImpressed = false
        loadStartTime = 0
    }
    
    private fun elapsedTime(): Long {
        return if (loadStartTime == 0L) 0L
        else System.currentTimeMillis() - loadStartTime
    }
    
    override fun onCleared() {
        super.onCleared()
        nativeAdLoader?.cancelLoading()
    }
}

private class NativeAdEventLogger(private val onImpressed: (() -> Unit)? = null) :
    NativeAdEventListener {
    override fun onAdClicked() {
        Log.d("YandexAds", "Ad clicked")
    }
    
    override fun onLeftApplication() {
        Log.d("YandexAds", "Left application")
    }
    
    override fun onReturnedToApplication() {
        Log.d("YandexAds", "Returned to application")
    }
    
    override fun onImpression(impressionData: ImpressionData?) {
        onImpressed?.invoke()
        Log.d("YandexAds", "Impression, ${impressionData?.rawData}")
    }
}



