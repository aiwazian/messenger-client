/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PrivacyField {
    @SerialName("lastSeen")
    LAST_SEEN,

    @SerialName("messages")
    MESSAGES,

    @SerialName("bio")
    BIO,

    @SerialName("dateOfBirth")
    DATE_OF_BIRTH,

    @SerialName("invites")
    INVITES,

    @SerialName("profilePhoto")
    PROFILE_PHOTO,

    @SerialName("forwardedProfile")
    FORWARDED_PROFILE,

    @SerialName("forwardAndCopy")
    FORWARD_AND_COPY;
}
