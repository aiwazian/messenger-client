package com.aiwazian.messenger.viewModels

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.Attachment
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.DeleteChatPayload
import com.aiwazian.messenger.data.DeleteMessagePayload
import com.aiwazian.messenger.data.DownloadItem
import com.aiwazian.messenger.data.Message
import com.aiwazian.messenger.data.ReadMessagePayload
import com.aiwazian.messenger.database.repository.ChannelRepository
import com.aiwazian.messenger.database.repository.ChatRepository
import com.aiwazian.messenger.database.repository.GroupRepository
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.WebSocketAction
import com.aiwazian.messenger.interfaces.Profile
import com.aiwazian.messenger.services.ChatService
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.services.UserService
import com.aiwazian.messenger.utils.ChatState
import com.aiwazian.messenger.utils.Constants
import com.aiwazian.messenger.utils.DownloadManager
import com.aiwazian.messenger.utils.WebSocketManager
import com.aiwazian.messenger.utils.getFileExtension
import com.aiwazian.messenger.utils.saveFileToApplicationFolder
import com.aiwazian.messenger.utils.saveFileToDownloadsFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val chatService: ChatService
) : ViewModel() {
    
    val myId = UserManager.user.value.id
    
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()
    
    private val _chatInfo = MutableStateFlow(ChatInfo())
    val chatInfo = _chatInfo.asStateFlow()
    
    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    
    private val _selectedMessages = MutableStateFlow<Set<Message>>(emptySet())
    val selectedMessages = _selectedMessages.asStateFlow()
    
    private val _userNamesCache = MutableStateFlow(mapOf<Long, String>())
    val userNamesCache = _userNamesCache.asStateFlow()
    
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads = _downloads.asStateFlow()
    
    val deleteChatDialog = DialogController()
    
    val clearHistoryDialog = DialogController()
    
    val deleteMessageDialog = DialogController()
    
    var onChatDeleted: (() -> Unit)? = null
    
    init {
        WebSocketManager.registerMessageHandler<Message>(WebSocketAction.NEW_MESSAGE) { message ->
            if (_chatInfo.value.id == message.senderId && message.senderId != myId) {
                _messages.update { it + message }
            }
        }
        
        WebSocketManager.registerMessageHandler<DeleteChatPayload>(WebSocketAction.DELETE_CHAT) { chat ->
            if (chat.chatId == _chatInfo.value.id) {
                onChatDeleted?.invoke()
            }
        }
        
        WebSocketManager.registerMessageHandler<DeleteMessagePayload>(WebSocketAction.DELETE_MESSAGE) { message ->
            if (_chatInfo.value.id == message.chatId) {
                deleteMessage(message.messageId)
            }
        }
        
        WebSocketManager.registerMessageHandler<ReadMessagePayload>(WebSocketAction.READ_MESSAGE) { message ->
            readMessage(message.messageId)
        }
        
        DownloadManager.onProgressUpdate = { url, progress ->
            _downloads.update { downloadItems ->
                downloadItems.map {
                    if (it.url == url.removePrefix(Constants.SERVER_URL)) it.copy(progress = progress)
                    else it
                }
            }
        }
    }
    
    fun changeText(newText: String) {
        _messageText.update { newText }
    }
    
    fun selectMessage(message: Message) {
        _selectedMessages.update { it + message }
    }
    
    fun unselectMessage(message: Message) {
        _selectedMessages.update { it - message }
    }
    
    suspend fun open(chatId: Long) {
        ChatState.openChat(chatId)
        
        _profile.update { null }
        
        _chatInfo.update {
            it.copy(id = chatId)
        }
        
        when (ChatType.fromId(chatId)) {
            ChatType.CHANNEL -> {
                val channel = channelRepository.get(chatId)
                
                _profile.update { channel }
            }
            
            ChatType.GROUP -> {
                val group = groupRepository.get(chatId)
                
                _profile.update { group }
            }
            
            else -> {}
        }
        
        val chatInfo = chatRepository.get(chatId)
        
        if (chatInfo == null) {
            _messages.update { emptyList() }
            return
        }
        
        _chatInfo.update { chatInfo }
        
        viewModelScope.launch {
            val chatMessages = chatRepository.getMessages(chatId)
            
            _messages.update { chatMessages }
        }
    }
    
    fun close() {
        _chatInfo.update { ChatInfo() }
        _messages.update { emptyList() }
        
        ChatState.closeChat()
    }
    
    suspend fun sendMessage(): Message? {
        if (_messageText.value.isBlank()) {
            return null
        }
        
        val validText = _messageText.value.trim()
        
        val lastMessageId = _messages.value.let {
            if (it.isNotEmpty()) {
                it.last().id + 1
            } else {
                1
            }
        }
        
        val messageId = Random.nextInt(
            lastMessageId + 1,
            Int.MAX_VALUE
        )
        
        val message = Message(
            id = messageId,
            senderId = myId,
            chatId = _chatInfo.value.id,
            text = validText,
            isRead = myId == _chatInfo.value.id,
            sendTime = System.currentTimeMillis()
        )
        
        changeText("")
        
        _messages.update { it + message }
        
        try {
            val sentMessage = chatRepository.sendMessage(message.chatId, message) ?: return null
            
            _messages.update { currentList ->
                currentList.map { message ->
                    if (message.id == messageId) {
                        message.copy(id = sentMessage.id)
                    } else {
                        message
                    }
                }
            }
            
            return sentMessage
        } catch (e: Exception) {
            Log.e(
                "ChatVM",
                "Ошибка отпаравки сррбщения",
                e
            )
            
            return null
        }
    }
    
    fun sendDocument(
        context: Context,
        fileUris: List<Uri>
    ) {
        if (fileUris.isEmpty()) {
            return
        }
        
        viewModelScope.launch {
            fileUris.forEach { fileUri ->
                try {
                    val contentResolver = context.contentResolver
                    val fileName = getFileName(
                        contentResolver,
                        fileUri
                    ) ?: "upload_file"
                    val mimeType = contentResolver.getType(fileUri) ?: "*/*"
                    
                    val requestBody = object : RequestBody() {
                        override fun contentType() =
                            mimeType.toMediaTypeOrNull()
                        
                        override fun writeTo(sink: BufferedSink) {
                            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    try {
                                        sink.write(
                                            buffer,
                                            0,
                                            bytesRead
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                            "ChatViewModel",
                                            "Ошибка записи в поток",
                                            e
                                        )
                                        throw e
                                    }
                                }
                                inputStream.close()
                            }
                        }
                    }
                    
                    val filePart = MultipartBody.Part.createFormData(
                        "file",
                        fileName,
                        requestBody
                    )
                    
                    val response = chatService.sendDocument(
                        filePart,
                        _chatInfo.value.id
                    )
                    
                    if (response != null) {
                        _messages.update { it + response }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "ChatViewModel",
                        "Ошибка при отправке файла $fileUri",
                        e
                    )
                    return@forEach
                }
            }
        }
    }
    
    fun addDownload(
        context: Context,
        attachment: Attachment
    ) {
        if (_downloads.value.none { it.url == attachment.url }) {
            _downloads.update {
                it + DownloadItem(
                    url = attachment.url,
                    fileName = attachment.name,
                    onComplete = {
                        viewModelScope.launch {
                            chatRepository.saveAttachment(attachment)
                        }
                    })
            }
            
            startDownload(
                item = _downloads.value.last(),
                onDownload = { body ->
                    if (body == null) {
                        return@startDownload
                    }
                    
                    val fileName = "${attachment.id}.${getFileExtension(attachment.name)}"
                    
                    viewModelScope.launch {
                        saveFileToApplicationFolder(
                            context,
                            body,
                            fileName
                        )
                    }
                    
                    _downloads.update { list ->
                        list.map {
                            if (it.url == attachment.url) {
                                it.copy(
                                    status = DownloadStatus.COMPLETED,
                                    progress = 100
                                ).also {
                                    it.onComplete?.invoke()
                                }
                            } else it
                        }
                    }
                },
                onError = {
                    _downloads.update { list ->
                        list.map {
                            if (it.url == attachment.url) it.copy(status = DownloadStatus.PENDING)
                            else it
                        }
                    }
                })
        }
    }
    
    fun openFile(
        context: Context,
        filePath: String
    ) {
        val appDir = context.getExternalFilesDir(null) ?: return
        val file = File(
            appDir,
            filePath
        )
        
        if (!file.exists()) {
            return
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "com.aiwazian.messenger.fileprovider",
            file
        )
        
        val mimeType = context.contentResolver.getType(uri)
        
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(
            uri,
            mimeType
        ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(
                "ChatViewModel",
                "Не найдено приложение для открытия файла.",
                e
            )
        } catch (e: Exception) {
            Log.e(
                "ChatViewModel",
                "Ошибка при открытии файла",
                e
            )
        }
    }
    
    private fun startDownload(
        item: DownloadItem,
        onDownload: (ResponseBody?) -> Unit,
        onError: () -> Unit
    ) {
        _downloads.update { list ->
            list.map {
                if (it.url == item.url) it.copy(status = DownloadStatus.DOWNLOADING)
                else it
            }
        }
        
        val call = RetrofitInstance.api.downloadFile(item.url)
        item.call = call
        
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    onDownload.invoke(response.body())
                } else {
                    onError.invoke()
                }
            }
            
            override fun onFailure(
                call: Call<ResponseBody>,
                t: Throwable
            ) {
                _downloads.update { list ->
                    list.map {
                        if (it.url == item.url) it.copy(status = DownloadStatus.PENDING)
                        else it
                    }
                }
            }
        })
    }
    
    fun cancelDownload(item: DownloadItem) {
        item.call?.cancel()
        _downloads.update { list ->
            list.map {
                if (it.url == item.url) it.copy(status = DownloadStatus.PENDING)
                else it
            }
        }
    }
    
    private fun getFileName(
        contentResolver: android.content.ContentResolver,
        uri: Uri
    ): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        return result ?: uri.pathSegments.lastOrNull()
    }
    
    suspend fun saveFile(
        context: Context,
        attachment: Attachment
    ) {
        val fileName = chatRepository.getAttachment(attachment.id).name
        
        saveFileToDownloadsFolder(
            context,
            attachment.id.toString() + '.' + getFileExtension(attachment.name),
            fileName
        )
    }
    
    suspend fun markAsReadMessage(message: Message) {
        if (message.senderId == myId) {
            return
        }
        
        val isRead = chatRepository.makeAsRead(
            _chatInfo.value.id,
            message.id
        )
        
        if (isRead) {
            readMessage(message.id)
        }
    }
    
    private val pendingRequests = mutableSetOf<Long>()
    
    fun loadUserName(userId: Long) {
        if (_userNamesCache.value.containsKey(userId)) {
            return
        }
        
        if (pendingRequests.contains(userId)) {
            return
        }
        
        pendingRequests.add(userId)
        
        viewModelScope.launch {
            try {
                val user = UserService().getById(userId)
                
                if (user != null) {
                    val userName = user.let { "${it.firstName} ${it.lastName}" }
                    
                    _userNamesCache.update { it + (userId to userName) }
                }
                
                pendingRequests.remove(userId)
            } catch (e: Exception) {
                Log.e(
                    "ChatViewModel",
                    "Не удалось получить имя отправителя",
                    e
                )
            }
        }
    }
    
    suspend fun tryDeleteMessage(
        messageId: Int,
        deleteForAll: Boolean
    ): Boolean {
        try {
            val isDeleted = chatRepository.deleteMessage(
                _chatInfo.value.id,
                messageId,
                deleteForAll
            )
            
            if (isDeleted) {
                deleteMessage(messageId)
            }
            
            return isDeleted
        } catch (e: Exception) {
            Log.e(
                "ChatVM",
                "Ошибка удаления сообщения",
                e
            )
            
            return false
        }
    }
    
    suspend fun tryDeleteChat(deleteForReceiver: Boolean): Boolean {
        try {
            val isDeleted = chatService.deleteChat(
                _chatInfo.value.id,
                deleteForReceiver
            )
            
            if (isDeleted) {
                deleteAllMessages()
            }
            
            return isDeleted
        } catch (e: Exception) {
            Log.e(
                "DeleteChat",
                "Ошибка при удалении чата",
                e
            )
            
            return false
        }
    }
    
    suspend fun tryDeleteChatMessages(deleteForReceiver: Boolean): Boolean {
        try {
            val isDeleted = chatRepository.deleteChatMessages(
                _chatInfo.value.id,
                deleteForReceiver
            )
            
            if (isDeleted) {
                deleteAllMessages()
            }
            
            return isDeleted
        } catch (e: Exception) {
            Log.e(
                "DeleteChat",
                "Ошибка при удалении сообщений в чате",
                e
            )
            
            return false
        }
    }
    
    private fun readMessage(messageId: Int) {
        _messages.update { currentList ->
            currentList.map { message ->
                if (message.id == messageId) {
                    val newMessage = message.copy(isRead = true)
                    newMessage
                } else {
                    message
                }
            }
        }
    }
    
    private fun deleteMessage(messageId: Int) {
        val messages = _messages.value.filter { it.id != messageId }
        _messages.update { messages }
    }
    
    private fun deleteAllMessages() {
        _messages.update { emptyList() }
    }
}

