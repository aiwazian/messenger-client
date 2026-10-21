package com.aiwazian.messenger.domain

data class DeviceContact(
    val displayName: String,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val organization: String? = null,
    val jobTitle: String? = null,
    val birthday: String? = null
) {
    val hasMultipleFields: Boolean
        get() = phones.size > 1 ||
                emails.isNotEmpty() ||
                urls.isNotEmpty() ||
                addresses.isNotEmpty() ||
                !organization.isNullOrBlank() ||
                !jobTitle.isNullOrBlank() ||
                !birthday.isNullOrBlank()
}
