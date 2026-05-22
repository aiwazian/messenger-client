/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

object RegexPatterns {
    val INVITE_LINK = Regex("(https?://)?[\\w-.]+/([a-f0-9]{32})")
    
    val URL =
        Regex("(https?://)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)")
    
    val SET_USERNAME = Regex("^[a-zA-Z0-9_]{0,32}$")
    
    val MENTION = Regex("@[a-zA-Z0-9_]{5,32}\\b")
    
    val PASSWORD = Regex("^\\S{0,64}$")
    
    val LOGIN = Regex("^\\S{0,64}$")
}
