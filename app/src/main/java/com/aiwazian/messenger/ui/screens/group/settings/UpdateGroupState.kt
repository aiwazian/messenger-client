/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

sealed interface UpdateGroupState {
    data object Idle : UpdateGroupState
    data object Loading : UpdateGroupState
    data class Success(val groupId: Long) : UpdateGroupState
    data class Error(val message: String) : UpdateGroupState
}
