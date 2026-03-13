package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.entity.UserEntity

fun UserInfo.toEntity(): UserEntity {
    return UserEntity(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        bio = this.bio,
        dateOfBirth = this.dateOfBirth
    )
}

fun UserEntity.toUser(): UserInfo {
    return UserInfo(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        bio = this.bio,
        dateOfBirth = this.dateOfBirth
    )
}