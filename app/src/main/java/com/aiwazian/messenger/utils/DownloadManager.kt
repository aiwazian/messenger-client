/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import com.ketch.DownloadConfig
import com.ketch.Ketch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext
    context: Context
) {
    private val ketch = Ketch.builder()
        .setDownloadConfig(
            DownloadConfig(
                connectTimeOutInMs = 15000,
                readTimeOutInMs = 15000
            )
        ).build(context)
}
