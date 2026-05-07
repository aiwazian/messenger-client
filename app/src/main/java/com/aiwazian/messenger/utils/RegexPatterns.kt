/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

object RegexPatterns {
    val USERNAME = Regex("^[a-zA-Z0-9_]{0,32}$")
    
    val PASSWORD = Regex("^\\S{0,64}$")
    
    val LOGIN = Regex("^\\S{0,64}$")
}
