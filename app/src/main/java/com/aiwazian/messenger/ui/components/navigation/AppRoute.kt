/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import com.aiwazian.messenger.enums.ChatFolderCategory
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
        val avatarUri: String? = null,
        val scrollToMessageId: Long? = null
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
    data object SettingsSelectChannel : AppRoute
    
    @Serializable
    data class SettingsUsername(val username: String?) : AppRoute
    
    @Serializable
    data class SettingsBio(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data class SettingsPhoto(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data class SettingsDateOfBirth(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data class SettingsInvites(val level: PrivacyLevel) : AppRoute
    
    @Serializable
    data class SettingsForwardedProfile(val level: PrivacyLevel) : AppRoute
    
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
    data object SettingsLogin : AppRoute
    
    @Serializable
    data object SettingsEmail : AppRoute
    
    @Serializable
    data object SettingsEmailVerify : AppRoute
    
    @Serializable
    data object SettingsEmailConfig : AppRoute
    
    @Serializable
    data object SettingsNotifications : AppRoute
    
    @Serializable
    data class SettingsNotificationCategory(val category: ChatFolderCategory) : AppRoute
    
    @Serializable
    data class SettingsNotificationException(val chatId: Long) : AppRoute
    
    @Serializable
    data class SelectNotificationExceptionChat(val category: ChatFolderCategory) : AppRoute
    
    @Serializable
    data object SettingsDataAndStorage : AppRoute
    
    @Serializable
    data object SettingsStorage : AppRoute
    
    @Serializable
    data object SettingsAutoDownloadMedia : AppRoute
    
    @Serializable
    data object ChatFolders : AppRoute
    
    @Serializable
    data class ChatFolderEditor(val folderId: Int? = null) : AppRoute
    
    @Serializable
    data class SelectFolderChats(
        val selectedChatIds: List<Long> = emptyList(),
        val selectedCategories: List<ChatFolderCategory> = emptyList()
    ) : AppRoute
    
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
    data class ChannelManagement(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelTransferOwnership(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelTypeSettings(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelSubscribers(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelBlackList(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelJoinRequests(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelInviteLinks(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelAdmins(val channelId: Long) : AppRoute
    
    @Serializable
    data class AddChannelAdmin(val channelId: Long) : AppRoute
    
    @Serializable
    data class ChannelAdminPermissions(val channelId: Long, val userId: Long) : AppRoute
    
    @Serializable
    data class CreateInviteLink(val channelId: Long) : AppRoute
    
    @Serializable
    data class GroupSettings(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupManagement(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupTransferOwnership(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupTypeSettings(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupInviteLinks(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupMembers(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupBlackList(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupJoinRequests(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupAdmins(val groupId: Long) : AppRoute
    
    @Serializable
    data class AddGroupAdmin(val groupId: Long) : AppRoute
    
    @Serializable
    data class GroupAdminPermissions(val groupId: Long, val userId: Long) : AppRoute
    
    @Serializable
    data class AddMember(val groupId: Long) : AppRoute
    
    @Serializable
    data object Logout : AppRoute
    
    @Serializable
    data object Login : AppRoute
    
    @Serializable
    data class Password(val login: String, val canReset: Boolean = false) : AppRoute
    
    @Serializable
    data class PasswordResetCode(val login: String) : AppRoute
    
    @Serializable
    data class ResetPassword(val login: String, val code: String) : AppRoute
    
    @Serializable
    data class Register(val login: String) : AppRoute
    
    @Serializable
    data object BlockedUserList : AppRoute
    
    @Serializable
    data object PendingJoinRequests : AppRoute
}
