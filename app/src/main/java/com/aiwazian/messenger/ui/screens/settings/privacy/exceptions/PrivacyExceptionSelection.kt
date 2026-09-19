/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.exceptions

import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField

data class PrivacyExceptionSelection(
    val field: PrivacyField,
    val kind: PrivacyExceptionKind,
    val userIds: List<Long> = emptyList()
)
