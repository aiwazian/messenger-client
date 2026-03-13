package com.aiwazian.messenger.utils

import android.util.Log
import com.aiwazian.messenger.data.WebSocketMessage
import com.aiwazian.messenger.enums.WebSocketAction
import com.aiwazian.messenger.services.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object WebSocketManager {
    
    private const val SOCKET_URL = Constants.WEB_SOCKET_URL
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    
    private val _isConnectedState = MutableStateFlow(false)
    val isConnectedState = _isConnectedState.asStateFlow()
    
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    var onConnect: (() -> Unit)? = null
    var onClose: ((Int) -> Unit)? = null
    var onFailure: (() -> Unit)? = null
    
    private val messageHandlers = mutableMapOf<WebSocketAction, MutableList<(JsonObject) -> Unit>>()
    
    private fun registerMessageHandler(
        action: WebSocketAction,
        handler: (JsonObject) -> Unit
    ) {
        val handlersList = messageHandlers.getOrPut(action) { mutableListOf() }
        handlersList.add(handler)
    }
    
    private fun unregisterMessageHandler(
        action: WebSocketAction,
        handler: (JsonObject) -> Unit
    ) {
        val handlersList = messageHandlers.getOrPut(action) { mutableListOf() }
        handlersList.remove(handler)
    }
    
    internal inline fun <reified T> registerMessageHandler(
        action: WebSocketAction,
        crossinline handler: (T) -> Unit
    ) {
        registerMessageHandler(action) { webSocketData ->
            try {
                val typedData = json.decodeFromJsonElement<T>(webSocketData)
                handler.invoke(typedData)
            } catch (e: Exception) {
                Log.e(
                    "WebSocketManager",
                    "Ошибка при десериализации для действия ${action}: ${e.message}"
                )
            }
        }
    }
    
    internal inline fun <reified T> unregisterMessageHandler(
        action: WebSocketAction,
        crossinline handler: (T) -> Unit
    ) {
        unregisterMessageHandler(action) { webSocketData ->
            try {
                val typedData = json.decodeFromJsonElement<T>(webSocketData)
                handler.invoke(typedData)
            } catch (e: Exception) {
                Log.e(
                    "WebSocketManager",
                    "Ошибка при десериализации для действия ${action}: ${e.message}"
                )
            }
        }
    }
    
    fun connect() {
        if (_isConnectedState.value) {
            return
        }
        
        val token = TokenManager.getToken()
        
        val request = Request.Builder().url("$SOCKET_URL?token=$token").build()
        
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    _isConnectedState.update { true }
                    
                    onConnect?.invoke()
                }
                
                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    try {
                        val webSocketMessage = json.decodeFromString<WebSocketMessage>(text)
                        val handlerList = messageHandlers[webSocketMessage.action]
                        
                        handlerList?.forEach { handler ->
                            handler.invoke(webSocketMessage.data as JsonObject)
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "WebSocketManager",
                            "Ошибка парсинга: ${e.message}"
                        )
                    }
                }
                
                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    _isConnectedState.update { false }
                    
                    webSocket.close(
                        code,
                        null
                    )
                }
                
                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    _isConnectedState.update { false }
                    
                    onClose?.invoke(code)
                }
                
                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    _isConnectedState.update { false }
                    
                    onFailure?.invoke()
                }
            })
    }
    
    fun close() {
        _isConnectedState.update { false }
        
        webSocket?.close(
            1000,
            null
        )
    }
}
