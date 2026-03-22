/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class Group(
    val id: Long = 0,
    val ownerId: Long? = null,
    val name: String = "",
    val bio: String? = null,
    val members: Int = 0
)
