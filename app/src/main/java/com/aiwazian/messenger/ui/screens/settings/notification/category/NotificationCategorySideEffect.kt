/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.category

import androidx.annotation.StringRes

sealed interface NotificationCategorySideEffect {
    data class ShowSnackbar(@param:StringRes val messageResId: Int) : NotificationCategorySideEffect
}
