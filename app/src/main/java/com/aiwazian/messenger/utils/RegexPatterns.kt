/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

object RegexPatterns {
    private const val SCHEME = "https?://"
    
    private const val OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)"
    
    private const val IPV4 = "$OCTET(?:\\.$OCTET){3}"
    
    private const val LABEL = "[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?"
    
    private const val DOMAIN = "(?:$LABEL\\.)+[a-z]{2,24}"
    
    private const val PORT = "(?::\\d{1,5})?"
    
    private const val PATH = "(?:[/?#][-a-z0-9()@:%_+.~#?&/=]*)?"
    
    val INVITE_LINK by lazy {
        Regex("(https?://)?[\\w\\-.]+/([a-f0-9]{32})", RegexOption.IGNORE_CASE)
    }
    
    val URL by lazy {
        Regex(
            "(?<![\\w@.\\-])(?:$SCHEME$LABEL(?:\\.$LABEL)*|$DOMAIN|$IPV4(?!\\.\\d))$PORT$PATH",
            RegexOption.IGNORE_CASE
        )
    }
    
    val URL_SCHEME by lazy {
        Regex("^$SCHEME", RegexOption.IGNORE_CASE)
    }
    
    val SET_USERNAME by lazy {
        Regex("^[a-zA-Z0-9_]{0,32}$")
    }
    
    val MENTION by lazy {
        Regex("@[a-zA-Z0-9_]{5,32}\\b")
    }
    
    val PASSWORD by lazy {
        Regex("^\\S{0,64}$")
    }
    
    val LOGIN by lazy {
        Regex("^\\S{0,64}$")
    }
    
    val EMAIL by lazy {
        Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")
    }
    
    val EMAIL_IN_TEXT by lazy {
        Regex(
            "(?<![\\w.+-])[a-zA-Z0-9](?:[a-zA-Z0-9._%+\\-]*[a-zA-Z0-9])?" +
                    "@[a-zA-Z0-9](?:[a-zA-Z0-9\\-]*[a-zA-Z0-9])?" +
                    "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*" +
                    "\\.[a-zA-Z]{2,24}\\b"
        )
    }
    
    val STICKER_LINK by lazy {
        Regex(
            "(?:https?://)?(?:www\\.)?aiwazian\\.ru/addstickers/([A-Za-z0-9_]+)",
            RegexOption.IGNORE_CASE
        )
    }
    
    val SINGLE_EMOJI by lazy {
        Regex("^[\\p{So}\\p{Cntrl}\\p{InEmoticons}\\p{InMiscellaneousSymbolsAndPictographs}\\p{InSupplementalSymbolsAndPictographs}\\uD83C\\uDFF0-\\uD83D\\uDFFF]+$")
    }
}
