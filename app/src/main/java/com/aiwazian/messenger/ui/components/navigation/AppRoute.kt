/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import com.aiwazian.messenger.enums.PrivacyLevel
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    
    @Serializable
    data object Main : AppRoute
    
    @Serializable
    data class Chat(
        val chatId: Long,
        val chatName: String? = null,
        val avatarUri: String? = null
    ) : AppRoute
    
    @Serializable
    data class Profile(
        val profileId: Long,
        val profileName: String? = null,
        val avatarUri: String? = null
    ) : AppRoute
    
    @Serializable
    data object Settings : AppRoute
    
    @Serializable
    data object SettingsLanguage : AppRoute
    
    @Serializable
    data object SettingsDarkTheme : AppRoute
    
    @Serializable
    data object SettingsChat : AppRoute
    
    @Serializable
    data object SettingsPrivacy : AppRoute
    
    @Serializable
    data class SettingsLastSeen(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data object SettingsSecurity : AppRoute
    
    @Serializable
    data object SettingsProfile : AppRoute
    
    @Serializable
    data class SettingsUsername(val username: String?) : AppRoute
    
    @Serializable
    data class SettingsBio(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data class SettingsDateOfBirth(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data class SettingsInvites(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data object SettingsDevices : AppRoute
    
    @Serializable
    data object SettingsPasscode : AppRoute
    
    @Serializable
    data object SettingsPasscodeCreate : AppRoute
    
    @Serializable
    data object SettingsPasscodeChange : AppRoute
    
    @Serializable
    data object SettingsCloudPassword : AppRoute
    
    @Serializable
    data object SettingsNotifications : AppRoute
    
    @Serializable
    data object SettingsDataAndStorage : AppRoute
    
    @Serializable
    data object NewMessage : AppRoute
    
    @Serializable
    data object CreateGroup : AppRoute
    
    @Serializable
    data class CreateGroupInviteLink(val groupId: Long) : AppRoute
    
    @Serializable
    data object CreateChannel : AppRoute
    
    @Serializable
    data class ChannelSettings(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelTypeSettings(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelSubscribers(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelBlackList(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelInviteLinks(val channelId: Long) : AppRoute
    
    @Serializable
    data class CreateInviteLink(val channelId: Long) : AppRoute
    
    @Serializable
    data class GroupSettings(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupTypeSettings(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupInviteLinks(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupMembers(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupBlackList(val groupId: Long) : AppRoute
    
    @Serializable
    data class AddMember(val groupId: Long) : AppRoute
    
    @Serializable
    data object Logout : AppRoute
    
    @Serializable
    data object Login : AppRoute
    
    @Serializable
    data class Password(val login: String) : AppRoute
    
    @Serializable
    data class Register(val login: String) : AppRoute
}
