/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.os.Build
import javax.inject.Inject

class DeviceInfoProvider @Inject constructor() {
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    fun getOsName(): String {
        return "Android"
    }
}
