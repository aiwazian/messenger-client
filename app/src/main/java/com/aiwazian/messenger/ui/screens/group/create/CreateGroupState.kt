/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

sealed class CreateGroupState {
    data object Idle : CreateGroupState()
    data object Loading : CreateGroupState()
    data class Success(val groupId: Long, val groupName: String) : CreateGroupState()
    data class Error(val message: String) : CreateGroupState()
}