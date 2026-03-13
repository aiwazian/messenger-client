package com.aiwazian.messenger.services

import android.os.Build
import javax.inject.Inject

class DeviceHelper @Inject constructor() {

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}