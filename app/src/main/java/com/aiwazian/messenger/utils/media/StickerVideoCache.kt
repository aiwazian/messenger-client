/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.aiwazian.messenger.network.dns.DnsController
import com.aiwazian.messenger.network.proxy.applyAppProxy
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient

object StickerVideoCache {

    private const val DIRECTORY = "sticker_videos"

    private const val MAX_BYTES = 128L * 1024 * 1024

    private val cacheReference = AtomicReference<SimpleCache?>()

    val callFactory: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(DnsController)
            .applyAppProxy()
            .build()
    }

    fun get(context: Context): SimpleCache =
        cacheReference.get() ?: synchronized(this) {
            cacheReference.get() ?: SimpleCache(
                File(context.applicationContext.cacheDir, DIRECTORY),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { cacheReference.set(it) }
        }

    fun dataSourceFactory(context: Context): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(callFactory))
}
