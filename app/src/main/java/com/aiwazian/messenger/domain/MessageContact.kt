package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
data class MessageContact(
    val name: String,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val organization: String? = null,
    val jobTitle: String? = null,
    val birthday: String? = null
) {
    val firstPhone: String? get() = phones.firstOrNull()
}
