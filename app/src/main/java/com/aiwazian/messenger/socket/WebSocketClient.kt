/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import android.util.Log
import com.aiwazian.messenger.BuildConfig
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.WebSocketAction
import com.aiwazian.messenger.utils.SessionManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val sessionManager: SessionManager
) {
    companion object {
        const val TAG = "WebSocketClient"
        private const val MAX_RECONNECT_DELAY_MS = 10000L
        private const val RECONNECTION_DELAY_MS = 1000L
        
        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
    
    private var socket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private var _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 1)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    
    private val messageHandlers = mutableMapOf<WebSocketAction, MutableList<(JsonObject) -> Unit>>()
    
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) return
        
        establishConnection()
    }
    
    fun disconnect() {
        socket?.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun emit(event: String, data: Any?): Boolean {
        val s = socket ?: return false
        if (!s.connected()) return false
        s.emit(event, data)
        return true
    }
    
    fun on(action: WebSocketAction, handler: (JsonObject) -> Unit) {
        messageHandlers.getOrPut(action) { mutableListOf() }.add(handler)
        socket?.on(action.name) { args -> handleIncomingEvent(action, args) }
    }

    inline fun <reified T> subscribeToTypedMessages(
        action: WebSocketAction,
        crossinline handler: (T) -> Unit
    ) {
        on(action) { jsonObject ->
            val data = parseEventData<T>(jsonObject)
            data?.let { handler(it) }
        }
    }

    inline fun <reified T> parseEventData(message: JsonObject): T? {
        return try {
            val jsonString = defaultJson.encodeToString(message)
            defaultJson.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event data: ${e.message}")
            null
        }
    }
    
    private fun establishConnection() {
        _connectionState.value = ConnectionState.CONNECTING
        
        scope.launch {
            while (!SessionManager.isInit) delay(100)
            
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
                on("Unauthorized") {
                    Log.e(TAG, "Received Unauthorized event from server")
                    SessionManager.getUnauthorizedCallback()?.invoke()
                }
                
                // Register all pending handlers
                messageHandlers.keys.forEach { action ->
                    on(action.name) { args -> handleIncomingEvent(action, args) }
                }
                
                connect()
            }
        }
    }
    
    private fun onConnect() {
        Log.d(TAG, "Connected")
        _connectionState.value = ConnectionState.CONNECTED
        scope.launch { _connectionEvents.emit(ConnectionEvent.Connected) }
    }
    
    private fun onConnectError(e: Exception?) {
        Log.e(TAG, "Connect error: ${e?.message}")
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    private fun onDisconnect() {
        Log.d(TAG, "Disconnected")
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    private fun handleIncomingEvent(action: WebSocketAction, args: Array<out Any?>) {
        scope.launch {
            val data = args.firstOrNull() ?: return@launch
            val jsonObject = when (data) {
                is JSONObject -> data.toJsonObject()
                is Map<*, *> -> mapToJsonObject(data)
                else -> return@launch
            }
            messageHandlers[action]?.forEach { it(jsonObject) }
        }
    }
    
    private fun JSONObject.toJsonObject(): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        keys().forEach { key ->
            val value = get(key)
            map[key] = when (value) {
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
    
    private fun JSONArray.toJsonArray(): kotlinx.serialization.json.JsonArray {
        val list = mutableListOf<JsonElement>()
        for (i in 0 until length()) {
            val value = get(i)
            list.add(when (value) {
                is JSONObject -> value.toJsonObject()
                is JSONArray -> value.toJsonArray()
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                null -> JsonPrimitive("")
                else -> JsonPrimitive(value.toString())
            })
        }
        return kotlinx.serialization.json.JsonArray(list)
    }
    
    private fun mapToJsonObject(map: Map<*, *>): JsonObject {
        val result = mutableMapOf<String, JsonElement>()
        map.forEach { (key, value) ->
            if (key is String) {
                result[key] = when (value) {
                    is Map<*, *> -> mapToJsonObject(value)
                    is List<*> -> listToJsonArray(value)
                    is String -> JsonPrimitive(value)
                    is Int -> JsonPrimitive(value)
                    is Long -> JsonPrimitive(value)
                    is Double -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    null -> JsonPrimitive("")
                    else -> JsonPrimitive(value.toString())
                }
            }
        }
        return JsonObject(result)
    }
    
    private fun listToJsonArray(list: List<*>): kotlinx.serialization.json.JsonArray {
        return kotlinx.serialization.json.JsonArray(list.map { value ->
            when (value) {
                is Map<*, *> -> mapToJsonObject(value)
                is List<*> -> listToJsonArray(value)
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                null -> JsonPrimitive("")
                else -> JsonPrimitive(value.toString())
            }
        })
    }
}
