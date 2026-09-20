package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField

data class PrivacyExceptions(
    val alwaysShow: Set<Long> = emptySet(),
    val alwaysHide: Set<Long> = emptySet()
) {
    fun forKind(kind: PrivacyExceptionKind) = when (kind) {
        PrivacyExceptionKind.ALWAYS_SHOW -> alwaysShow
        PrivacyExceptionKind.ALWAYS_HIDE -> alwaysHide
    }
}
