/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception

import androidx.annotation.StringRes

sealed interface NotificationExceptionSideEffect {
    
    /** Исключение удалили — настраивать больше нечего, уходим назад. */
    data object NavigateBack : NotificationExceptionSideEffect
    
    data class ShowSnackbar(@param:StringRes val messageResId: Int) : NotificationExceptionSideEffect
}
