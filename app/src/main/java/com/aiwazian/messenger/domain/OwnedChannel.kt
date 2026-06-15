/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class OwnedChannel(
    val id: Long,
    val name: String,
    val subscribers: Int,
    val avatar: Avatar? = null
)
