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
    
    val EMAIL = Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")
    
    /**
     * Адрес внутри произвольного текста — без якорей ^ и $, иначе EMAIL находит
     * только строку, целиком состоящую из адреса.
     *
     * Поддерживает точки, дефисы, плюсы и теги в локальной части (work.time+tag@…),
     * многоуровневые домены (mail.ru, co.uk, mail.yandex.com.tr) и длинные зоны (.company).
     * Границы — чтобы не цеплять часть ссылки типа site.com/a@b и не брать точку в конце фразы.
     */
    val EMAIL_IN_TEXT = Regex(
        "(?<![\\w.+-])[a-zA-Z0-9](?:[a-zA-Z0-9._%+\\-]*[a-zA-Z0-9])?" +
                "@[a-zA-Z0-9](?:[a-zA-Z0-9\\-]*[a-zA-Z0-9])?" +
                "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*" +
                "\\.[a-zA-Z]{2,24}\\b"
    )
}
