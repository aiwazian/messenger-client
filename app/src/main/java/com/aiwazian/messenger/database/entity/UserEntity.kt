/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("user")
data class UserEntity(
    @PrimaryKey var id: Long,
    var firstName: String = "",
    var lastName: String? = null,
    var username: String? = null,
    var bio: String? = null,
    var dateOfBirth: Long? = null
)
