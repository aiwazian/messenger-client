/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    UPLOADING,
    UPLOADED,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}
