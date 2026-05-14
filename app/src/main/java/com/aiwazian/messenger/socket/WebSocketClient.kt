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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
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
        
        private val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
    
    private var _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var delayedUpdateJob: Job? = null
    
    private val messageHandlers = mutableMapOf<String, MutableList<(JsonObject) -> Unit>>()
    
    private var socket: Socket? = null
    var socketId: String? = null
    
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) return
        
        _connectionState.value = ConnectionState.CONNECTING
        scheduleDelayedUiUpdate(ConnectionState.CONNECTING)
        establishConnection()
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
    
    fun emitEvent(eventName: String, data: Any) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot emit event $eventName: socket is not connected")
            return
        }
        
        val jsonString = try {
            when (data) {
                is Map<*, *> -> {
                    val casted = data as Map<String, Any?>
                    
                    val jsonObject = buildJsonObject {
                        casted.forEach { (key, value) ->
                            put(key, defaultJson.encodeToJsonElement(value))
                        }
                    }
                    
                    defaultJson.encodeToString(JsonObject.serializer(), jsonObject)
                }
                
                else -> defaultJson.encodeToString(serializer(data::class.java), data)
            }
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
            on("Unauthorized") {
                Log.e(TAG, "Received Unauthorized event from server")
                SessionManager.getUnauthorizedCallback()?.invoke()
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
    
    private fun onConnect() {
        Log.d(TAG, "Connected")
        socketId = socket?.id()
        _connectionState.value = ConnectionState.CONNECTED
        delayedUpdateJob?.cancel()
    }
    
    private fun onConnectError(e: Exception?) {
        Log.e(TAG, "Connect error: ${e?.message}")
        _connectionState.value = ConnectionState.DISCONNECTED
        scheduleDelayedUiUpdate(ConnectionState.DISCONNECTED)
    }
    
    private fun onDisconnect() {
        Log.d(TAG, "Disconnected")
        _connectionState.value = ConnectionState.DISCONNECTED
        scheduleDelayedUiUpdate(ConnectionState.DISCONNECTED)
    }
    
    private fun scheduleDelayedUiUpdate(state: ConnectionState) {
        delayedUpdateJob?.cancel()
        delayedUpdateJob = coroutineScope.launch {
            delay(DELAY_UI_UPDATE_MS)
            if (_connectionState.value != ConnectionState.CONNECTED) {
                _connectionState.value = state
            }
        }
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
