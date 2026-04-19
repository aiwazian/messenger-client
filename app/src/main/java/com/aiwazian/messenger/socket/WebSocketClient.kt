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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

    private var _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val messageHandlers = mutableMapOf<String, MutableList<(JsonObject) -> Unit>>()

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) return

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

    private fun establishConnection() {
        _connectionState.value = ConnectionState.CONNECTING

        val opts = IO.Options.builder()
            .setAuth(mapOf("token" to sessionManager.getToken()))
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionDelay(RECONNECTION_DELAY_MS)
            .setReconnectionDelayMax(MAX_RECONNECT_DELAY_MS)
            .build()

        IO.socket(BuildConfig.WS_URL, opts).apply {
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

    private fun JSONArray.toJsonArray(): JsonArray {
        val list = mutableListOf<JsonElement>()
        for (i in 0 until length()) {
            val value = get(i)
            list.add(
                when (value) {
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
