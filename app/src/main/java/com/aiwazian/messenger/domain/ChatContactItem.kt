/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class ChatContactItem(
    val contactId: Long,
    val messageId: Long,
    val senderId: Long,
    val sendTime: Long,
    val contact: MessageContact
) {
    val name: String get() = contact.name
    val firstPhone: String? get() = contact.firstPhone
}

data class ChatContactsPage(
    val items: List<ChatContactItem>,
    val nextCursorId: Int?
)
