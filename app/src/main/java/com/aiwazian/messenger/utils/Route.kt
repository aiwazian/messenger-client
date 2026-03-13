package com.aiwazian.messenger.utils

object Route {
    
    /** Auth **/
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FIND_USER_BY_LOGIN = "auth/findUserByLogin/{login}"
    
    /** User **/
    const val ME = "api/me"
    const val LOGOUT = "api/logout"
    const val SEARCH_USER = "api/searchUser"
    const val GE_USER_BY_ID = "api/user/{id}"
    const val CHECK_USERNAME = "api/checkUsername/{username}"
    const val SAVE_USERNAME = "api/me/username/{username}"
    const val PROFILE_UPDATE = "api/profileUpdate"
    const val ARCHIVED_CHATS = "api/archivedChat"
    const val UNARCHIVED_CHATS = "api/unarchivedChat"
    const val GET_SESSIONS = "api/sessions"
    const val TERMINATE_SESSION = "api/session/{id}"
    const val GET_DEVICE_COUNT = "api/sessionCount"
    const val UPDATE_FCM_TOKEN = "api/updateFcmToken"
    const val TERMINATE_ALL_SESSIONS = "api/terminateAllSessions"
    const val CHANGE_CLOUD_PASSWORD = "api/changeCloudPassword"
    
    /** Chat **/
    const val CHATS = "api/chats"
    const val CHATS_WITH_USERS = "api/chats/users"
    const val CHAT_MESSAGES = "api/chat/{id}/messages"
    const val GET_CHAT_LAST_MESSAGE = "api/chat/{chatId}/messages/last"
    const val DELETE_CHAT = "api/chat/{id}"
    const val DELETE_CHAT_MESSAGES = "api/chat/{id}/messages"
    const val GET_CHAT_INFO = "api/chat/{id}"
    const val PIN_CHAT = "api/chat/{id}/pin"
    const val UNPIN_CHAT = "api/chat/{id}/pin"
    const val ADD_CHAT_TO_ARCHIVE = "api/chat/{id}/archive"
    const val DELETE_CHAT_FROM_ARCHIVE = "api/chat/{id}/archive"
    const val SEND_MESSAGE = "api/chat/{chatId}/messages"
    const val SEND_DOCUMENT = "api/v1/upload-file/{chatId}"
    const val DELETE_MESSAGE = "api/chat/{chatId}/messages/{messageId}"
    const val MAKE_AS_READ_MESSAGE = "api/chat/{chatId}/messages/{messageId}/read"
    
    /** Folder **/
    const val FOLDER = "api/folder"
    const val FOLDERS = "api/folders"
    const val DELETE_FOLDER = "api/folder/{id}"
    const val PIN_CHAT_IN_FOLDER = "api/folders/{folderId}/chats/{chatId}/pin"
    const val UNPIN_CHAT_IN_FOLDER = "api/folders/{folderId}/chats/{chatId}/pin"
    const val GET_MY_PRIVACY = "api/me/privacy"
    const val CHANGE_BIO_PRIVACY = "api/userPrivacy/bio/{value}"
    const val CHANGE_DATE_OF_BIRTH_PRIVACY = "api/userPrivacy/dateOfBirth/{value}"
    
    /** Channel **/
    const val CREATE_CHANNEL = "api/channel"
    const val SAVE_CHANNEL = "api/channel/{id}"
    const val DELETE_CHANNEL = "api/channel/{id}"
    const val GET_CHANNEL = "api/channel/{id}"
    const val JOIN_CHANNEL = "api/channel/{id}/join"
    const val LEAVE_CHANNEL = "api/channel/{id}/leave"
    const val GET_CHANNEL_SUBSCRIBERS = "api/channel/{id}/subscribers"
    const val CHECK_CHANNEL_PUBLIC_LINK = "api/channel/q/{link}"
    
    /** Group **/
    const val CREATE_GROUP = "api/group"
    const val GET_GROUP = "api/group/{id}"
    const val DELETE_GROUP = "api/group/{id}"
    const val GET_GROUP_MEMBERS = "api/group/{id}/members"
    const val INVITE_USER_TO_GROUP = "api/group/{groupId}/invite/{userId}"
    const val REMOVE_USER_FROM_GROUP = "api/group/{groupId}/remove/{userId}"
}