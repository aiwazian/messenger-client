package com.aiwazian.messenger.interfaces

import androidx.annotation.Keep
import com.aiwazian.messenger.data.ApiResponse
import com.aiwazian.messenger.data.AuthRequest
import com.aiwazian.messenger.data.ChangeCloudPasswordRequest
import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.FolderInfo
import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.data.Message
import com.aiwazian.messenger.data.NotificationTokenRequest
import com.aiwazian.messenger.data.PrivacySettings
import com.aiwazian.messenger.data.RegisterRequest
import com.aiwazian.messenger.data.SearchInfo
import com.aiwazian.messenger.data.SessionInfo
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.utils.Route
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

@Keep
interface ApiService {
    
    @GET(Route.FIND_USER_BY_LOGIN)
    suspend fun findUserByLogin(@Path("login") login: String): Response<ApiResponse>
    
    @POST(Route.LOGIN)
    suspend fun login(@Body request: AuthRequest): Response<ApiResponse>
    
    @POST(Route.REGISTER)
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse>
    
    @DELETE(Route.LOGOUT)
    suspend fun logout(): Response<ApiResponse>
    
    @GET(Route.ME)
    suspend fun getMe(): Response<UserInfo>
    
    @GET(Route.UNARCHIVED_CHATS)
    suspend fun getUnarchivedChats(): Response<List<ChatInfo>>
    
    @GET(Route.ARCHIVED_CHATS)
    suspend fun getArchivedChats(): Response<List<ChatInfo>>
    
    @GET(Route.GET_SESSIONS)
    suspend fun getSessions(): Response<List<SessionInfo>>
    
    @POST(Route.UPDATE_FCM_TOKEN)
    suspend fun updateFcmToken(@Body newToken: NotificationTokenRequest): Response<ApiResponse>
    
    @DELETE(Route.TERMINATE_ALL_SESSIONS)
    suspend fun terminateAllSessions(): Response<ApiResponse>
    
    @DELETE(Route.TERMINATE_SESSION)
    suspend fun terminateSession(@Path("id") id: Int): Response<ApiResponse>
    
    @GET(Route.GET_DEVICE_COUNT)
    suspend fun getDeviceCount(): Response<Int>
    
    @DELETE(Route.DELETE_CHAT)
    suspend fun deleteChat(
        @Path("id") chatId: Long,
        @Query("deleteForReceiver") deleteForReceiver: Boolean
    ): Response<ApiResponse>
    
    @DELETE(Route.DELETE_CHAT_MESSAGES)
    suspend fun deleteChatMessages(
        @Path("id") id: Long,
        @Query("deleteForReceiver") deleteForReceiver: Boolean
    ): Response<ApiResponse>
    
    @PATCH(Route.CHANGE_CLOUD_PASSWORD)
    suspend fun changeCloudPassword(@Body body: ChangeCloudPasswordRequest): Response<ApiResponse>
    
    @PATCH(Route.CHANGE_BIO_PRIVACY)
    suspend fun changeBioPrivacy(@Path("value") body: Int): Response<ApiResponse>
    
    @PATCH(Route.CHANGE_DATE_OF_BIRTH_PRIVACY)
    suspend fun changeDateOfBirthPrivacy(@Path("value") body: Int): Response<ApiResponse>
    
    @GET(Route.CHAT_MESSAGES)
    suspend fun getMessagesBetweenUsers(@Path("id") chatId: Long): Response<List<Message>>
    
    @GET(Route.GET_CHAT_LAST_MESSAGE)
    suspend fun getChatLastMessage(@Path("chatId") chatId: Long): Response<Message>
    
    @GET(Route.GET_CHAT_INFO)
    suspend fun getChatInfo(@Path("id") id: Long): Response<ChatInfo?>
    
    @PUT(Route.PROFILE_UPDATE)
    suspend fun updateProfile(@Body profile: UserInfo): Response<Unit>
    
    @GET(Route.SEARCH_USER)
    suspend fun searchUser(@Query("search") query: String): Response<List<SearchInfo>>
    
    @GET(Route.GE_USER_BY_ID)
    suspend fun getUserById(@Path("id") id: Long): Response<UserInfo>
    
    @POST(Route.ADD_CHAT_TO_ARCHIVE)
    suspend fun archiveChat(@Path("id") chatId: Long): Response<ApiResponse>
    
    @DELETE(Route.DELETE_CHAT_FROM_ARCHIVE)
    suspend fun unarchiveChat(@Path("id") chatId: Long): Response<ApiResponse>
    
    @POST(Route.SEND_MESSAGE)
    suspend fun sendMessage(
        @Path("chatId") chatId: Long,
        @Body requestBody: Message
    ): Response<Message>
    
    @Multipart
    @POST(Route.SEND_DOCUMENT)
    suspend fun sendDocument(
        @Part file: MultipartBody.Part,
        @Path("chatId") chatId: Long
    ): Response<Message>
    
    @DELETE(Route.DELETE_MESSAGE)
    suspend fun deleteMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Int,
        @Query("deleteForAllUsers") deleteForAllUsers: Boolean
    ): Response<ApiResponse>
    
    @PATCH(Route.MAKE_AS_READ_MESSAGE)
    suspend fun makeAsReadMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Int
    ): Response<ApiResponse>
    
    @POST(Route.FOLDER)
    suspend fun saveFolder(@Body requestBody: FolderInfo): Response<ApiResponse>
    
    @DELETE(Route.DELETE_FOLDER)
    suspend fun deleteFolder(@Path("id") id: Int): Response<ApiResponse>
    
    @GET(Route.FOLDERS)
    suspend fun getFolders(): Response<List<FolderInfo>>
    
    @GET(Route.CHATS)
    suspend fun getAllChats(): Response<List<ChatInfo>>
    
    @GET(Route.CHATS_WITH_USERS)
    suspend fun getAllChatsWithOtherUser(): Response<List<ChatInfo>>
    
    @POST(Route.PIN_CHAT)
    suspend fun pinChat(
        @Path("id") chatId: Long
    ): Response<ApiResponse>
    
    @DELETE(Route.UNPIN_CHAT)
    suspend fun unpinChat(
        @Path("id") chatId: Long
    ): Response<ApiResponse>
    
    @POST(Route.PIN_CHAT_IN_FOLDER)
    suspend fun pinChatInFolder(
        @Path("folderId") folderId: Int,
        @Path("chatId") chatId: Long
    ): Response<ApiResponse>
    
    @DELETE(Route.UNPIN_CHAT_IN_FOLDER)
    suspend fun unpinChatInFolder(
        @Path("folderId") folderId: Int,
        @Path("chatId") chatId: Long
    ): Response<ApiResponse>
    
    @GET(Route.GET_MY_PRIVACY)
    suspend fun getMyPrivacy(): Response<PrivacySettings>
    
    @GET(Route.CHECK_USERNAME)
    suspend fun checkUsername(@Path("username") username: String): Response<ApiResponse>
    
    @PATCH(Route.SAVE_USERNAME)
    suspend fun saveUsername(@Path("username") username: String): Response<ApiResponse>
    
    @POST(Route.CREATE_CHANNEL)
    suspend fun createChannel(@Body channelInfo: ChannelInfo): Response<ApiResponse>
    
    @POST(Route.SAVE_CHANNEL)
    suspend fun saveChannel(
        @Path("id") id: Long,
        @Body channelInfo: ChannelInfo
    ): Response<ApiResponse>
    
    @DELETE(Route.DELETE_CHANNEL)
    suspend fun deleteChannel(@Path("id") id: Long): Response<ApiResponse>
    
    @GET(Route.GET_CHANNEL)
    suspend fun getChannel(@Path("id") id: Long): Response<ChannelInfo>
    
    @POST(Route.JOIN_CHANNEL)
    suspend fun joinChannel(@Path("id") id: Long): Response<ApiResponse>
    
    @DELETE(Route.LEAVE_CHANNEL)
    suspend fun leaveChannel(@Path("id") id: Long): Response<ApiResponse>
    
    @GET(Route.GET_CHANNEL_SUBSCRIBERS)
    suspend fun getChannelSubscribers(@Path("id") id: Long): Response<List<UserInfo>>
    
    @GET(Route.CHECK_CHANNEL_PUBLIC_LINK)
    suspend fun checkChannelPublicLink(@Path("link") link: String): Response<ApiResponse>
    
    @POST(Route.CREATE_GROUP)
    suspend fun createGroup(@Body groupInfo: GroupInfo): Response<ApiResponse>
    
    @GET(Route.GET_GROUP)
    suspend fun getGroup(@Path("id") id: Long): Response<GroupInfo>
    
    @DELETE(Route.DELETE_GROUP)
    suspend fun deleteGroup(@Path("id") id: Long): Response<ApiResponse>
    
    @GET(Route.GET_GROUP_MEMBERS)
    suspend fun getGroupMembers(@Path("id") id: Long): Response<List<UserInfo>>
    
    @POST(Route.INVITE_USER_TO_GROUP)
    suspend fun inviteUserToGroup(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<ApiResponse>
    
    @DELETE(Route.REMOVE_USER_FROM_GROUP)
    suspend fun removeUserFromGroup(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<ApiResponse>
    
    @Streaming
    @GET
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>
}
