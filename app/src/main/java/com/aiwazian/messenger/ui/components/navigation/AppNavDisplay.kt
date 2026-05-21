/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aiwazian.messenger.ui.screens.auth.login.LoginScreen
import com.aiwazian.messenger.ui.screens.auth.password.PasswordScreen
import com.aiwazian.messenger.ui.screens.auth.register.RegisterScreen
import com.aiwazian.messenger.ui.screens.channel.create.CreateChannelScreen
import com.aiwazian.messenger.ui.screens.channel.settings.ChannelSettingsScreen
import com.aiwazian.messenger.ui.screens.channel.settings.blockedUsers.ChannelBlockedUsersScreen
import com.aiwazian.messenger.ui.screens.channel.settings.invites.ChannelInviteLinksScreen
import com.aiwazian.messenger.ui.screens.channel.settings.invites.create.CreateInviteLinkScreen
import com.aiwazian.messenger.ui.screens.channel.settings.subscribers.ChannelSubscribersScreen
import com.aiwazian.messenger.ui.screens.channel.settings.type.ChannelTypeSettingsScreen
import com.aiwazian.messenger.ui.screens.chat.ChatScreen
import com.aiwazian.messenger.ui.screens.group.create.CreateGroupScreen
import com.aiwazian.messenger.ui.screens.group.settings.GroupSettingsScreen
import com.aiwazian.messenger.ui.screens.group.settings.addMember.AddMemberScreen
import com.aiwazian.messenger.ui.screens.group.settings.blockedUsers.GroupBlockedUsersScreen
import com.aiwazian.messenger.ui.screens.group.settings.invites.GroupInviteLinksScreen
import com.aiwazian.messenger.ui.screens.group.settings.invites.create.CreateGroupInviteLinkScreen
import com.aiwazian.messenger.ui.screens.group.settings.members.GroupMembersScreen
import com.aiwazian.messenger.ui.screens.group.settings.type.GroupTypeSettingsScreen
import com.aiwazian.messenger.ui.screens.logout.LogoutScreen
import com.aiwazian.messenger.ui.screens.main.MainScreen
import com.aiwazian.messenger.ui.screens.newmessage.NewMessageScreen
import com.aiwazian.messenger.ui.screens.profile.ProfileScreen
import com.aiwazian.messenger.ui.screens.settings.SettingsScreen
import com.aiwazian.messenger.ui.screens.settings.appearance.SettingsAppearanceScreen
import com.aiwazian.messenger.ui.screens.settings.appearance.SettingsDarkThemeScreen
import com.aiwazian.messenger.ui.screens.settings.language.SettingsLanguageScreen
import com.aiwazian.messenger.ui.screens.settings.notification.NotificationSettingsScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.SettingsPrivacyScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.bio.SettingsBioScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.dateOfBirth.SettingsDateOfBirthScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.invites.SettingsInvitesScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.lastSeen.SettingsLastSeenScreen
import com.aiwazian.messenger.ui.screens.settings.profile.SettingsProfileScreen
import com.aiwazian.messenger.ui.screens.settings.profile.username.SettingsUsernameScreen
import com.aiwazian.messenger.ui.screens.settings.security.SettingsCloudPasswordScreen
import com.aiwazian.messenger.ui.screens.settings.security.SettingsSecurityScreen
import com.aiwazian.messenger.ui.screens.settings.security.devices.SettingsDevicesScreen
import com.aiwazian.messenger.ui.screens.settings.security.passcode.SettingsPasscodeChangeScreen
import com.aiwazian.messenger.ui.screens.settings.security.passcode.SettingsPasscodeCreateScreen
import com.aiwazian.messenger.ui.screens.settings.security.passcode.SettingsPasscodeScreen
import com.aiwazian.messenger.ui.screens.settings.storage.StorageScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun AppNavDisplay(
    vararg startRoute: AppRoute,
    externalRouteFlow: Flow<AppRoute>? = null
) {
    val backStack = rememberNavBackStack(elements = startRoute)
    
    LaunchedEffect(externalRouteFlow) {
        externalRouteFlow?.collect { route ->
            backStack.add(route)
        }
    }
    
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalNavBackStack provides backStack,
            LocalSharedTransitionScope provides this
        ) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                onBack = backStack::removeLastOrNull,
                entryProvider = entryProvider {
                    entry<AppRoute.Main> { MainScreen() }
                    entry<AppRoute.Chat>(metadata = PredictiveBackMetadata) {
                        ChatScreen(
                            chatId = it.chatId,
                            chatName = it.chatName,
                            avatarUri = it.avatarUri
                        )
                    }
                    entry<AppRoute.Profile>(metadata = PredictiveBackMetadata) {
                        ProfileScreen(
                            profileId = it.profileId,
                            profileName = it.profileName,
                            avatarUri = it.avatarUri
                        )
                    }
                    entry<AppRoute.Settings> { SettingsScreen() }
                    entry<AppRoute.SettingsLanguage> { SettingsLanguageScreen() }
                    entry<AppRoute.SettingsDarkTheme> { SettingsDarkThemeScreen() }
                    entry<AppRoute.SettingsChat> { SettingsAppearanceScreen() }
                    entry<AppRoute.SettingsNotifications> { NotificationSettingsScreen() }
                    entry<AppRoute.SettingsPrivacy> { SettingsPrivacyScreen() }
                    entry<AppRoute.SettingsLastSeen> { SettingsLastSeenScreen(level = it.level) }
                    entry<AppRoute.SettingsSecurity> { SettingsSecurityScreen() }
                    entry<AppRoute.SettingsProfile> { SettingsProfileScreen() }
                    entry<AppRoute.SettingsUsername> { SettingsUsernameScreen(username = it.username) }
                    entry<AppRoute.SettingsBio> { SettingsBioScreen(level = it.level) }
                    entry<AppRoute.SettingsDateOfBirth> { SettingsDateOfBirthScreen(level = it.level) }
                    entry<AppRoute.SettingsInvites> { SettingsInvitesScreen(level = it.level) }
                    entry<AppRoute.SettingsDevices> { SettingsDevicesScreen() }
                    entry<AppRoute.SettingsPasscode> { SettingsPasscodeScreen() }
                    entry<AppRoute.SettingsPasscodeCreate> { SettingsPasscodeCreateScreen() }
                    entry<AppRoute.SettingsPasscodeChange> { SettingsPasscodeChangeScreen() }
                    entry<AppRoute.SettingsCloudPassword> { SettingsCloudPasswordScreen() }
                    entry<AppRoute.SettingsDataAndStorage> { StorageScreen() }
                    entry<AppRoute.NewMessage> { NewMessageScreen() }
                    entry<AppRoute.CreateGroup> { CreateGroupScreen() }
                    entry<AppRoute.CreateChannel> { CreateChannelScreen() }
                    entry<AppRoute.ChannelSettings> { ChannelSettingsScreen(channelId = it.channelId) }
                    entry<AppRoute.ChannelTypeSettings> { ChannelTypeSettingsScreen(channelId = it.channelId) }
                    entry<AppRoute.ChannelSubscribers> { ChannelSubscribersScreen(channelId = it.channelId) }
                    entry<AppRoute.ChannelBlackList> { ChannelBlockedUsersScreen(channelId = it.channelId) }
                    entry<AppRoute.ChannelInviteLinks> { ChannelInviteLinksScreen(channelId = it.channelId) }
                    entry<AppRoute.CreateInviteLink> { CreateInviteLinkScreen(channelId = it.channelId) }
                    entry<AppRoute.GroupSettings> { GroupSettingsScreen(groupId = it.groupId) }
                    entry<AppRoute.GroupTypeSettings> { GroupTypeSettingsScreen(groupId = it.groupId) }
                    entry<AppRoute.GroupInviteLinks> { GroupInviteLinksScreen(groupId = it.groupId) }
                    entry<AppRoute.CreateGroupInviteLink> { CreateGroupInviteLinkScreen(groupId = it.groupId) }
                    entry<AppRoute.GroupMembers> { GroupMembersScreen(groupId = it.groupId) }
                    entry<AppRoute.GroupBlackList> { GroupBlockedUsersScreen(groupId = it.groupId) }
                    entry<AppRoute.AddMember> { AddMemberScreen(groupId = it.groupId) }
                    entry<AppRoute.Logout> { LogoutScreen() }
                    entry<AppRoute.Login>(metadata = HorizontalMetadata) { LoginScreen() }
                    entry<AppRoute.Password>(metadata = HorizontalMetadata) { PasswordScreen(login = it.login) }
                    entry<AppRoute.Register>(metadata = HorizontalMetadata) { RegisterScreen(login = it.login) }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
                },
                popTransitionSpec = {
                    slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                }
            )
        }
    }
}
