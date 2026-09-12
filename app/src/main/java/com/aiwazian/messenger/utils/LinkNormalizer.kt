/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

object LinkNormalizer {
    private const val HTTPS_PREFIX = "https://"
    
    fun normalize(url: String): String {
        val scheme = RegexPatterns.URL_SCHEME.find(url) ?: return HTTPS_PREFIX + url
        
        return url.replaceRange(scheme.range, scheme.value.lowercase())
    }
}
