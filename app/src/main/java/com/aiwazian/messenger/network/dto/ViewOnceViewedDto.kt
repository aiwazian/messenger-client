/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ViewOnceViewedDto(
    @SerialName("deleteAt") val deleteAt: Long? = null,
    @SerialName("viewOnceSeconds") val viewOnceSeconds: Int? = null,
    @SerialName("alreadyOpened") val alreadyOpened: Boolean = false
)

@Serializable
data class ViewOnceFileIdsDto(
    @SerialName("fileIds") val fileIds: List<String> = emptyList()
)
