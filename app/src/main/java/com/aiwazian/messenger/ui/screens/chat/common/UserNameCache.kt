/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.common

import com.aiwazian.messenger.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UserNameCache(
    private val scope: CoroutineScope,
    private val userRepository: UserRepository
) {
    private val requested = mutableSetOf<Long>()

    fun load(userId: Long, onName: (Long, String) -> Unit) {
        if (!requested.add(userId)) return

        scope.launch {
            try {
                userRepository.getById(userId).collect { user ->
                    val name = "${user.firstName} ${user.lastName.orEmpty()}".trim()
                    onName(userId, name)
                }
            } catch (_: Exception) {
            }
        }
    }
}
