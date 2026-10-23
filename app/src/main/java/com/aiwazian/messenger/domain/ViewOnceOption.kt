/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class ViewOnceOption(
    val seconds: Int? = null
) {
    val isOneView: Boolean
        get() = seconds == null
}
