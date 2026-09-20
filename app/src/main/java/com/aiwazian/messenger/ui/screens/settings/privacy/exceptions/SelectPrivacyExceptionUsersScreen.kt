/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.exceptions

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.ProfileCard
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun SelectPrivacyExceptionUsersScreen(
    field: PrivacyField,
    kind: PrivacyExceptionKind,
    selectedUserIds: List<Long> = emptyList(),
    viewModel: SelectPrivacyExceptionUsersViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val resultBus = LocalResultEventBus.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.restoreSelection(field, kind, selectedUserIds)
    }

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.add_exceptions))
                },
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = {
                            resultBus.sendResult<PrivacyExceptionSelection>(
                                result = viewModel.buildSelection()
                            )
                            navBackStack.removeLastOrNull()
                        })
                )
            )
        }) {
        SectionContainer {
            FramelessTextBox(
                placeholder = stringResource(R.string.search),
                value = uiState.query,
                onValueChange = viewModel::onQueryChange
            )
        }

        SectionContainer {
            uiState.users.forEach { user ->
                ProfileCard(
                    id = user.id,
                    headlineText = user.chatName.asString(),
                    avatarUri = user.avatarUri,
                    trailingContent = {
                        Checkbox(
                            checked = user.id in uiState.selectedUserIds,
                            onCheckedChange = null,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp)
                        )
                    },
                    onClick = { viewModel.toggleUser(user.id) })
            }
        }
    }
}
