/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aiwazian.messenger.ui.screens.channel.CreateChannelScreen
import com.aiwazian.messenger.ui.screens.channel.settings.ChannelBlackListScreen
import com.aiwazian.messenger.ui.screens.channel.settings.ChannelSettingsScreen
import com.aiwazian.messenger.ui.screens.channel.settings.ChannelSubscribersScreen
import com.aiwazian.messenger.ui.screens.channel.settings.type.ChannelInviteLinksScreen
import com.aiwazian.messenger.ui.screens.channel.settings.type.ChannelTypeSettingsScreen
import com.aiwazian.messenger.ui.screens.chat.ChatScreen
import com.aiwazian.messenger.ui.screens.group.AddMemberScreen
import com.aiwazian.messenger.ui.screens.group.create.CreateGroupScreen
import com.aiwazian.messenger.ui.screens.group.settings.GroupMembersScreen
import com.aiwazian.messenger.ui.screens.group.settings.GroupSettingsScreen
import com.aiwazian.messenger.ui.screens.login.LoginScreen
import com.aiwazian.messenger.ui.screens.login.PasswordScreen
import com.aiwazian.messenger.ui.screens.logout.LogoutScreen
import com.aiwazian.messenger.ui.screens.main.MainScreen
import com.aiwazian.messenger.ui.screens.newmessage.NewMessageScreen
import com.aiwazian.messenger.ui.screens.profile.ProfileScreen
import com.aiwazian.messenger.ui.screens.search.SearchScreen
import com.aiwazian.messenger.ui.screens.settings.SettingsNotificationsScreen
import com.aiwazian.messenger.ui.screens.settings.SettingsScreen
import com.aiwazian.messenger.ui.screens.settings.appearance.SettingsChatScreen
import com.aiwazian.messenger.ui.screens.settings.appearance.SettingsDarkThemeScreen
import com.aiwazian.messenger.ui.screens.settings.language.SettingsLanguageScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.SettingsBioScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.SettingsDateOfBirthScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.SettingsLastSeenScreen
import com.aiwazian.messenger.ui.screens.settings.privacy.SettingsPrivacyScreen
import com.aiwazian.messenger.ui.screens.settings.profile.SettingsProfileColorScreen
import com.aiwazian.messenger.ui.screens.settings.profile.SettingsProfileScreen
import com.aiwazian.messenger.ui.screens.settings.profile.SettingsUsernameScreen
import com.aiwazian.messenger.ui.screens.settings.security.SettingsCloudPasswordScreen
import com.aiwazian.messenger.ui.screens.settings.security.SettingsSecurityScreen
import com.aiwazian.messenger.ui.screens.settings.security.devices.SettingsDevicesScreen
import com.aiwazian.messenger.ui.screens.settings.security.passcode.SettingsPasscodeChangeScreen
import com.aiwazian.messenger.ui.screens.settings.security.passcode.SettingsPasscodeCreateScreen
import com.aiwazian.messenger.ui.screens.settings.security.passcode.SettingsPasscodeScreen
import com.aiwazian.messenger.ui.screens.settings.storage.StorageScreen

val LocalNavHost = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("No NavHost provided")
}

@Composable
fun AppNavHost(startRoute: AppRoute = AppRoute.Main) {
    val backStack = rememberNavBackStack(startRoute)
    
    CompositionLocalProvider(LocalNavHost provides backStack) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<AppRoute.Main> { MainScreen() }
                entry<AppRoute.Search> { SearchScreen() }
                entry<AppRoute.Chat> { ChatScreen(chatId = it.chatId) }
                entry<AppRoute.Profile> { ProfileScreen(profileId = it.profileId) }
                entry<AppRoute.Settings> { SettingsScreen() }
                entry<AppRoute.SettingsLanguage> { SettingsLanguageScreen() }
                entry<AppRoute.SettingsDesign> { SettingsDarkThemeScreen() }
                entry<AppRoute.SettingsChat> { SettingsChatScreen() }
                entry<AppRoute.SettingsPrivacy> { SettingsPrivacyScreen() }
                entry<AppRoute.SettingsLastSeen> { SettingsLastSeenScreen() }
                entry<AppRoute.SettingsSecurity> { SettingsSecurityScreen() }
                entry<AppRoute.SettingsProfile> { SettingsProfileScreen() }
                entry<AppRoute.SettingsUsername> { SettingsUsernameScreen(username = it.username) }
                entry<AppRoute.SettingsBio> { SettingsBioScreen(level = it.level) }
                entry<AppRoute.SettingsDateOfBirth> { SettingsDateOfBirthScreen(level = it.level) }
                entry<AppRoute.SettingsDevices> { SettingsDevicesScreen() }
                entry<AppRoute.SettingsPasscode> { SettingsPasscodeScreen() }
                entry<AppRoute.SettingsPasscodeCreate> { SettingsPasscodeCreateScreen() }
                entry<AppRoute.SettingsPasscodeChange> { SettingsPasscodeChangeScreen() }
                entry<AppRoute.SettingsCloudPassword> { SettingsCloudPasswordScreen() }
                entry<AppRoute.SettingsNotifications> { SettingsNotificationsScreen() }
                entry<AppRoute.SettingsDataAndStorage> { StorageScreen() }
                entry<AppRoute.SettingsProfileColor> { SettingsProfileColorScreen() }
                entry<AppRoute.NewMessage> { NewMessageScreen() }
                entry<AppRoute.CreateGroup> { CreateGroupScreen() }
                entry<AppRoute.CreateChannel> { CreateChannelScreen() }
                entry<AppRoute.ChannelSettings> { ChannelSettingsScreen(channelId = it.channelId) }
                entry<AppRoute.ChannelTypeSettings> { ChannelTypeSettingsScreen(channelId = it.channelId) }
                entry<AppRoute.ChannelSubscribers> { ChannelSubscribersScreen(channelId = it.channelId) }
                entry<AppRoute.ChannelBlackList> { ChannelBlackListScreen() }
                entry<AppRoute.ChannelInviteLinks> { ChannelInviteLinksScreen() }
                entry<AppRoute.GroupSettings> { GroupSettingsScreen(groupId = it.groupId) }
                entry<AppRoute.GroupMembers> { GroupMembersScreen() }
                entry<AppRoute.AddMember> { AddMemberScreen() }
                entry<AppRoute.Logout> { LogoutScreen() }
                entry<AppRoute.Login> { LoginScreen() }
                entry<AppRoute.Password> { PasswordScreen() }
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
