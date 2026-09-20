/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.exceptions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.PrivacyExceptions
import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem

@Composable
fun PrivacyExceptionsSection(
    field: PrivacyField,
    exceptions: PrivacyExceptions?,
    onNavigate: (PrivacyField, PrivacyExceptionKind, List<Long>) -> Unit
) {
    val usesAllowDenyWording = field == PrivacyField.INVITES ||
            field == PrivacyField.FORWARDED_PROFILE ||
            field == PrivacyField.FORWARD_AND_COPY

    val items = if (usesAllowDenyWording) {
        listOf(
            PrivacyExceptionKind.ALWAYS_SHOW to R.string.always_allow,
            PrivacyExceptionKind.ALWAYS_HIDE to R.string.always_deny
        )
    } else {
        listOf(
            PrivacyExceptionKind.ALWAYS_SHOW to R.string.always_show,
            PrivacyExceptionKind.ALWAYS_HIDE to R.string.always_hide
        )
    }

    SectionContainer(header = {
        SectionHeader(stringResource(R.string.add_exceptions))
    }) {
        items.forEach { (kind, label) ->
            SectionItem(
                headlineText = stringResource(label),
                trailingText = exceptions?.forKind(kind)?.size?.toString() ?: "0",
                onClick = {
                    onNavigate(
                        field,
                        kind,
                        exceptions?.forKind(kind)?.toList() ?: emptyList()
                    )
                })
        }
    }
}
