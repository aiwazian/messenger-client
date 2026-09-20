package com.aiwazian.messenger.ui.screens.settings.privacy.forwardedProfile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.PrivacyExceptions
import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.repository.PrivacyRepository
import com.aiwazian.messenger.ui.screens.settings.privacy.exceptions.PrivacyExceptionSelection
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsForwardedProfileViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    private val _initialLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)

    private val _currentLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentLevel = _currentLevel.asStateFlow()

    private val _initialForwardAndCopyLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)

    private val _currentForwardAndCopyLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentForwardAndCopyLevel = _currentForwardAndCopyLevel.asStateFlow()

    private val _initialForwardedProfileExceptions = MutableStateFlow<PrivacyExceptions?>(null)

    private val _currentForwardedProfileExceptions = MutableStateFlow<PrivacyExceptions?>(null)
    val currentForwardedProfileExceptions = _currentForwardedProfileExceptions.asStateFlow()

    private val _initialForwardAndCopyExceptions = MutableStateFlow<PrivacyExceptions?>(null)

    private val _currentForwardAndCopyExceptions = MutableStateFlow<PrivacyExceptions?>(null)
    val currentForwardAndCopyExceptions = _currentForwardAndCopyExceptions.asStateFlow()

    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton = _showSaveButton.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsForwardedProfileEffect>()
    val effect = _effect.asSharedFlow()

    private var initializedLevel: PrivacyLevel? = null

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }

    fun init(initialValue: PrivacyLevel) {
        if (initializedLevel == initialValue) {
            return
        }
        initializedLevel = initialValue

        _initialLevel.update { initialValue }
        _currentLevel.update { initialValue }
        hideSaveButton()
        loadRemoteState()
    }

    fun selectValue(value: PrivacyLevel) {
        _currentLevel.update { value }
        updateSaveButtonVisibility()
    }

    fun selectForwardAndCopyValue(value: PrivacyLevel) {
        _currentForwardAndCopyLevel.update { value }
        updateSaveButtonVisibility()
    }

    fun applyExceptionSelection(selection: PrivacyExceptionSelection) {
        when (selection.field) {
            PrivacyField.FORWARDED_PROFILE -> _currentForwardedProfileExceptions.update { current ->
                applySelection(current, selection)
            }

            PrivacyField.FORWARD_AND_COPY -> _currentForwardAndCopyExceptions.update { current ->
                applySelection(current, selection)
            }

            else -> return
        }
        updateSaveButtonVisibility()
    }

    fun onSaveClick() {
        viewModelScope.launch {
            try {
                privacyRepository.updateForwardingPrivacy(
                    forwardedProfile = _currentLevel.value,
                    forwardAndCopy = _currentForwardAndCopyLevel.value,
                    forwardedProfileExceptions = _currentForwardedProfileExceptions.value,
                    forwardAndCopyExceptions = _currentForwardAndCopyExceptions.value
                ).onSuccess {
                    _effect.emit(SettingsForwardedProfileEffect.Back)
                }.onFailure {
                    vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsForwardedProfileViewModel",
                    "Failed to update message forwarding privacy settings",
                    e
                )
                vibrate(VibrationPattern.Error)
            }
        }
    }

    private fun applySelection(
        current: PrivacyExceptions?,
        selection: PrivacyExceptionSelection
    ): PrivacyExceptions {
        val base = current ?: PrivacyExceptions()
        val userIds = selection.userIds.toSet()
        return when (selection.kind) {
            PrivacyExceptionKind.ALWAYS_SHOW -> base.copy(
                alwaysShow = userIds,
                alwaysHide = base.alwaysHide - userIds
            )

            PrivacyExceptionKind.ALWAYS_HIDE -> base.copy(
                alwaysHide = userIds,
                alwaysShow = base.alwaysShow - userIds
            )
        }
    }

    private fun loadRemoteState() {
        viewModelScope.launch {
            privacyRepository.getPrivacySettings().onSuccess { settings ->
                if (!_showSaveButton.value) {
                    _initialForwardAndCopyLevel.update { settings.forwardAndCopy }
                    _currentForwardAndCopyLevel.update { settings.forwardAndCopy }
                }

                if (_currentForwardedProfileExceptions.value == null) {
                    val exceptions = settings.exceptionsFor(PrivacyField.FORWARDED_PROFILE)
                    _initialForwardedProfileExceptions.update { exceptions }
                    _currentForwardedProfileExceptions.update { exceptions }
                }

                if (_currentForwardAndCopyExceptions.value == null) {
                    val exceptions = settings.exceptionsFor(PrivacyField.FORWARD_AND_COPY)
                    _initialForwardAndCopyExceptions.update { exceptions }
                    _currentForwardAndCopyExceptions.update { exceptions }
                }
            }
        }
    }

    private fun updateSaveButtonVisibility() {
        val hasChanges = _currentLevel.value != _initialLevel.value ||
                _currentForwardAndCopyLevel.value != _initialForwardAndCopyLevel.value ||
                exceptionsChanged(
                    _currentForwardedProfileExceptions.value,
                    _initialForwardedProfileExceptions.value
                ) ||
                exceptionsChanged(
                    _currentForwardAndCopyExceptions.value,
                    _initialForwardAndCopyExceptions.value
                )
        _showSaveButton.update { hasChanges }
    }

    private fun exceptionsChanged(
        current: PrivacyExceptions?,
        initial: PrivacyExceptions?
    ): Boolean {
        val currentExceptions = current ?: return false
        return currentExceptions != initial
    }

    private fun hideSaveButton() {
        _showSaveButton.update { false }
    }
}
