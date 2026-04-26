/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SystemMessageEventType {
    CHANNEL_CREATED,
    GROUP_CREATED,
    HISTORY_CLEARED;
}
