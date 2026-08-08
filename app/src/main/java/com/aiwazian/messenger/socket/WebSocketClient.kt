/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import android.util.Log
import com.aiwazian.messenger.BuildConfig
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.utils.SessionManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val MAX_RECONNECT_DELAY_MS = 10000L
        private const val RECONNECTION_DELAY_MS = 1000L
        private const val DELAY_UI_UPDATE_MS = 3000L
        
        /**
         * Отказ авторизации на подключении сервер присылает как Unauthorized, а
         * отключение уже живущей сессии — как auth:error. Для клиента оба события
         * значат одно: текущий токен мёртв.
         */
        private const val EVENT_UNAUTHORIZED = "Unauthorized"
        private const val EVENT_AUTH_ERROR = "auth:error"
        
        private val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
    
    private var _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private val messageHandlers = mutableMapOf<String, MutableList<(JsonObject) -> Unit>>()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var socket: Socket? = null
    var socketId: String? = null
    
    init {
        /*
         * Клиент живёт в синглтоне и переживает смену аккаунта, а токен передаётся
         * при рукопожатии. Без переподключения сокет остался бы с токеном прошлой
         * сессии, который сервер уже не принимает.
         *
         * Пока сокет ни разу не создавали, подключением управляет вызывающий код.
         */
        scope.launch {
            sessionManager.token
                .drop(1)
                .distinctUntilChanged()
                .collect { token ->
                    if (socket == null) {
                        return@collect
                    }
                    
                    if (token.isEmpty()) {
                        disconnect()
                    } else {
                        Log.d(TAG, "Token changed, reconnecting socket")
                        reconnect()
                    }
                }
        }
    }
    
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) return
        
        _connectionState.value = ConnectionState.CONNECTING
        establishConnection()
    }
    
    fun disconnect() {
        socket?.let { activeSocket ->
            activeSocket.off()
            activeSocket.disconnect()
        }
        socket = null
        socketId = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    private fun reconnect() {
        disconnect()
        connect()
    }
    
    fun <Dto : Any, Domain : Any> subscribeToEvent(
        event: WebSocketEvent<Dto, Domain>,
        handler: (Domain) -> Unit
    ) {
        messageHandlers.getOrPut(event.eventName) { mutableListOf() }.add { jsonObject ->
            val jsonString = defaultJson.encodeToString(JsonObject.serializer(), jsonObject)
            val dto = defaultJson.decodeFromString(event.deserializer, jsonString)
            handler(event.mapper(dto))
        }
    }
    
    fun emitEvent(eventName: String, data: Map<String, String>) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot emit event $eventName: socket is not connected")
            return
        }
        
        val jsonString = try {
            val jsonObject = buildJsonObject {
                data.forEach { (key, value) ->
                    put(key, defaultJson.encodeToJsonElement(value))
                }
            }
            
            defaultJson.encodeToString(JsonObject.serializer(), jsonObject)
        } catch (e: Exception) {
            Log.e(TAG, "Serialization failed", e)
            return
        }
        socket?.emit(eventName, JSONObject(jsonString))
        Log.d(TAG, "Emitted event: $eventName with data: $jsonString")
    }
    
    private fun establishConnection() {
        val opts = IO.Options.builder()
            .setAuth(mapOf("token" to sessionManager.getToken()))
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionDelay(RECONNECTION_DELAY_MS)
            .setReconnectionDelayMax(MAX_RECONNECT_DELAY_MS)
            .build()
        
        socket = IO.socket(BuildConfig.WS_URL, opts).apply {
            on(Socket.EVENT_CONNECT) { onConnect() }
            on(Socket.EVENT_CONNECT_ERROR) { onConnectError(it.firstOrNull() as? Exception) }
            on(Socket.EVENT_DISCONNECT) { onDisconnect() }
            on(EVENT_UNAUTHORIZED) {
                Log.e(TAG, "Received Unauthorized event from server")
                onSessionRejected()
            }
            on(EVENT_AUTH_ERROR) {
                Log.e(TAG, "Received auth:error event from server: session was terminated")
                onSessionRejected()
            }
            
            onAnyIncoming { args ->
                val eventName = args.getOrNull(0) as? String ?: return@onAnyIncoming
                val data = args.getOrNull(1) as? JSONObject ?: return@onAnyIncoming
                handleIncomingEvent(eventName, data.toJsonObject())
                Log.d(TAG, "Received incoming event: $eventName $data")
            }
            
            connect()
        }
    }
    
    /**
     * Сессия отключена сервером. Переподключаться с тем же токеном бессмысленно,
     * поэтому сокет гасится, а дальше решает SessionManager: перейти на другой
     * аккаунт устройства или отправить на авторизацию.
     */
    private fun onSessionRejected() {
        SessionManager.notifyUnauthorized()
    }
    
    private fun onConnect() {
        Log.d(TAG, "Connected")
        socketId = socket?.id()
        _connectionState.value = ConnectionState.CONNECTED
    }
    
    private fun onConnectError(e: Exception?) {
        Log.e(TAG, "Connect error: ${e?.message}")
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    private fun onDisconnect() {
        Log.d(TAG, "Disconnected")
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    private fun handleIncomingEvent(eventName: String, jsonObject: JsonObject) {
        messageHandlers[eventName]?.forEach { it(jsonObject) }
    }
    
    private fun JSONObject.toJsonObject(): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        keys().forEach { key ->
            map[key] = when (val value = get(key)) {
                is JSONObject -> value.toJsonObject()
                is JSONArray -> value.toJsonArray()
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                null -> JsonPrimitive("")
                else -> JsonPrimitive(value.toString())
            }
        }
        return JsonObject(map)
    }
    
    private fun JSONArray.toJsonArray(): JsonArray {
        val list = mutableListOf<JsonElement>()
        for (i in 0 until length()) {
            list.add(
                when (val value = get(i)) {
                    is JSONObject -> value.toJsonObject()
                    is JSONArray -> value.toJsonArray()
                    is String -> JsonPrimitive(value)
                    is Int -> JsonPrimitive(value)
                    is Long -> JsonPrimitive(value)
                    is Double -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    null -> JsonPrimitive("")
                    else -> JsonPrimitive(value.toString())
                }
            )
        }
        return JsonArray(list)
    }
}
