/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.BuildConfig
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.AdTheme
import com.yandex.mobile.ads.common.ImpressionData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class AdBannerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private var reloadJob: Job? = null
    private var initialLoadDone = false
    private var bannerVisible = false
    private var adTheme = AdTheme.LIGHT

    private val bannerEventListener = object : BannerAdEventListener {
        override fun onAdLoaded() {}

        override fun onAdFailedToLoad(error: AdRequestError) {
            Log.e(LOG_TAG, error.description)
            scheduleReload(RETRY_DELAY)
        }

        override fun onAdClicked() {}

        override fun onImpression(data: ImpressionData?) {
            Log.d(LOG_TAG, "Impression: ${data?.rawData}")
            if (bannerVisible) scheduleReload(IMPRESSION_RELOAD_DELAY)
        }
    }

    val bannerView: BannerAdView = BannerAdView(context).apply {
        setAdSize(bannerAdSize())
        setBannerAdEventListener(bannerEventListener)
    }

    fun ensureInitialLoad(theme: AdTheme) {
        if (initialLoadDone) return
        initialLoadDone = true
        adTheme = theme
        bannerView.loadAd(buildAdRequest())
    }

    fun setBannerVisible(visible: Boolean) {
        if (bannerVisible == visible) return
        bannerVisible = visible
        if (visible) {
            scheduleReload(IMPRESSION_RELOAD_DELAY)
        } else {
            reloadJob?.cancel()
            reloadJob = null
        }
    }

    private fun scheduleReload(reloadDelay: Duration) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(reloadDelay)
            bannerView.loadAd(buildAdRequest())
        }
    }

    private fun buildAdRequest(): AdRequest =
        AdRequest.Builder(BuildConfig.AD_BANNER_ID).setPreferredTheme(adTheme).build()

    private fun bannerAdSize(): BannerAdSize {
        val maxHeight = if (context.resources.configuration.screenHeightDp < 400) 100 else 300
        return BannerAdSize.inline(context, BANNER_WIDTH_DP, maxHeight)
    }

    override fun onCleared() {
        reloadJob?.cancel()
        bannerView.destroy()
    }

    private companion object {
        const val BANNER_WIDTH_DP = 300
        val RETRY_DELAY = 4.seconds
        val IMPRESSION_RELOAD_DELAY = 60.seconds
        const val LOG_TAG = "YandexAds"
    }
}
