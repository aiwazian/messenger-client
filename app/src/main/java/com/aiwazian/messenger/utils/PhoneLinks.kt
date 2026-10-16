/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class PhoneMatch(
    val range: IntRange,
    val e164: String
)

object PhoneLinks {
    private const val DEFAULT_REGION = "US"

    private val phoneNumberUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    fun currentDeviceRegion(): String {
        return Locale.getDefault().country.ifEmpty { DEFAULT_REGION }
    }

    fun findInText(text: String, region: String = currentDeviceRegion()): List<PhoneMatch> {
        return phoneNumberUtil
            .findNumbers(text, region, PhoneNumberUtil.Leniency.VALID, Long.MAX_VALUE)
            .map { match ->
                val e164 = phoneNumberUtil.format(
                    match.number(),
                    PhoneNumberUtil.PhoneNumberFormat.E164
                )

                PhoneMatch(
                    range = IntRange(match.start(), match.end() - 1),
                    e164 = e164
                )
            }
            .toList()
    }
}
