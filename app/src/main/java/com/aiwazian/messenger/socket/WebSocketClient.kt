/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.WebSocketAction
import com.aiwazian.messenger.utils.SessionManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        private const val RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
        private const val CONNECT_TIMEOUT_MS = 15000L
        private const val RECONNECTION_DELAY_MS = 1000L
        
        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
    
    private var socket: Socket? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private var _incomingMessages = MutableSharedFlow<JsonObject>(replay = 0)
    
    private var _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 1)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()
    
    private var serverUrl: String = ""
    private var isIntentionalDisconnect = false
    private var currentReconnectDelay = RECONNECT_DELAY_MS
    
    private val messageHandlers = mutableMapOf<WebSocketAction, MutableList<(JsonObject) -> Unit>>()
    
    private var lifecycleOwner: LifecycleOwner? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var autoReconnectOnLifecycle = true
    
    fun bindToLifecycle(
        owner: LifecycleOwner,
        autoReconnect: Boolean = true
    ) {
        unbindFromLifecycle()
        
        autoReconnectOnLifecycle = autoReconnect
        lifecycleOwner = owner
        
        lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (autoReconnectOnLifecycle && serverUrl.isNotEmpty()) {
                        if (_connectionState.value != ConnectionState.CONNECTED &&
                            _connectionState.value != ConnectionState.CONNECTING
                        ) {
                            Log.d(
                                TAG,
                                "Lifecycle ON_START: connecting Socket.io"
                            )
                            establishConnection()
                        }
                    }
                }
                
                Lifecycle.Event.ON_STOP -> {
                    if (autoReconnectOnLifecycle) {
                        Log.d(
                            TAG,
                            "Lifecycle ON_STOP: disconnecting Socket.io"
                        )
                        closeSocket()
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
                
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(
                        TAG,
                        "Lifecycle ON_DESTROY: cleaning up Socket.io"
                    )
                    disconnect()
                    unbindFromLifecycle()
                }
                
                else -> {}
            }
        }
        
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver!!)
        Log.d(
            TAG,
            "Socket.io bound to lifecycle"
        )
    }
    
    fun unbindFromLifecycle() {
        lifecycleObserver?.let { observer ->
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
        lifecycleOwner = null
        lifecycleObserver = null
        Log.d(
            TAG,
            "Socket.io unbound from lifecycle"
        )
    }
    
    fun connectWithLifecycle(
        url: String,
        owner: LifecycleOwner,
        autoReconnect: Boolean = true
    ) {
        connect(url)
        bindToLifecycle(
            owner,
            autoReconnect
        )
    }
    
    fun setOnDisconnectedCallback(callback: (Int, String) -> Unit) {
        scope.launch {
            connectionEvents.collect { event ->
                if (event is ConnectionEvent.Disconnected) {
                    callback(
                        event.code,
                        event.reason
                    )
                }
            }
        }
    }
    
    fun connect(url: String) {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) {
            Log.w(
                TAG,
                "Already connected or connecting"
            )
            return
        }
        
        serverUrl = url
        isIntentionalDisconnect = false
        currentReconnectDelay = RECONNECT_DELAY_MS
        
        establishConnection()
    }
    
    fun disconnect() {
        isIntentionalDisconnect = true
        connectionJob?.cancel()
        closeSocket()
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(
            TAG,
            "Intentional disconnect"
        )
    }
    
    fun emit(
        event: String,
        data: Any?
    ): Boolean {
        val socket = socket
        if (socket == null || _connectionState.value != ConnectionState.CONNECTED) {
            Log.w(
                TAG,
                "Cannot emit event: not connected"
            )
            return false
        }
        
        socket.emit(
            event,
            data
        )
        return true
    }
    
    fun on(
        action: WebSocketAction,
        handler: (JsonObject) -> Unit
    ) {
        val handlers = messageHandlers.getOrPut(action) { mutableListOf() }
        handlers.add(handler)
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            registerSocketListener(action)
        }
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
            val jsonString =
                defaultJson.encodeToString(
                    JsonObject.Companion.serializer(),
                    message
                )
            defaultJson.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error parsing event data: ${e.message}"
            )
            null
        }
    }
    
    private fun establishConnection() {
        _connectionState.value = ConnectionState.CONNECTING
        
        scope.launch {
            while (!SessionManager.isInit) {
                delay(100)
            }
            
            val token = sessionManager.getToken()
            
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionDelay(RECONNECTION_DELAY_MS)
                .setReconnectionDelayMax(MAX_RECONNECT_DELAY_MS)
                .setTimeout(CONNECT_TIMEOUT_MS)
                .build()
            
            try {
                socket =
                    IO.socket(
                        serverUrl,
                        opts
                    )
                socket?.on(
                    Socket.EVENT_CONNECT,
                    ::onConnect
                )
                    ?.on(
                        Socket.EVENT_CONNECT_ERROR,
                        ::onConnectError
                    )
                    ?.on(
                        Socket.EVENT_DISCONNECT,
                        ::onDisconnect
                    )
                
                socket?.connect()
                Log.d(
                    TAG,
                    "Connecting to $serverUrl with token"
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error creating socket: ${e.message}",
                    e
                )
                handleError(e)
            }
        }
    }
    
    private fun onConnect(vararg args: Any?) {
        Log.d(
            TAG,
            "Socket.io connected"
        )
        _connectionState.value = ConnectionState.CONNECTED
        currentReconnectDelay = RECONNECT_DELAY_MS
        
        messageHandlers.keys.forEach { event ->
            registerSocketListener(event)
        }
        
        scope.launch {
            _connectionEvents.emit(ConnectionEvent.Connected)
        }
    }
    
    private fun onConnectError(vararg args: Any?) {
        val error = args.firstOrNull() as? Exception
        Log.e(
            TAG,
            "Socket.io connect error: ${error?.message}",
            error
        )
        handleError(error ?: Exception("Unknown connection error"))
    }
    
    private fun onDisconnect(vararg args: Any?) {
        val reason = args.firstOrNull()?.toString() ?: "Unknown"
        Log.d(
            TAG,
            "Socket.io disconnected: $reason"
        )
        handleDisconnection(reason)
    }
    
    private fun registerSocketListener(event: WebSocketAction) {
        val socket = socket ?: return
        
        socket.off(event.name)
        
        socket.on(event.name) { args ->
            handleIncomingEvent(
                event,
                args
            )
        }
    }
    
    private fun handleIncomingEvent(
        event: WebSocketAction,
        args: Array<out Any?>
    ) {
        scope.launch {
            try {
                val jsonObject = when (val data = args.firstOrNull()) {
                    is JSONObject -> data.toJsonObject()
                    is Map<*, *> -> mapToJsonObject(data)
                    else -> {
                        Log.w(
                            TAG,
                            "Unknown data type for event $event: ${data?.javaClass}"
                        )
                        return@launch
                    }
                }
                
                val messageWithAction = JsonObject(
                    mapOf(
                        "action" to JsonPrimitive(event.name),
                        "data" to jsonObject
                    )
                )
                
                _incomingMessages.emit(messageWithAction)
                
                messageHandlers[event]?.forEach { handler ->
                    try {
                        handler(jsonObject)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error in message handler for $event: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error parsing event: ${e.message}"
                )
            }
        }
    }
    
    private fun handleDisconnection(reason: String) {
        val wasConnected = _connectionState.value == ConnectionState.CONNECTED
        _connectionState.value = ConnectionState.DISCONNECTED
        
        val code = when {
            reason.contains("io server disconnect") -> 1001
            reason.contains("io client disconnect") -> 1000
            reason.contains("ping timeout") -> 1006
            else -> 1006
        }
        
        scope.launch {
            _connectionEvents.emit(
                ConnectionEvent.Disconnected(
                    code,
                    reason
                )
            )
        }
        
        if (isIntentionalDisconnect) {
            Log.d(
                TAG,
                "Intentional disconnect, not reconnecting"
            )
            return
        }
        
        if (wasConnected) {
            _connectionState.value = ConnectionState.RECONNECTING
            Log.d(
                TAG,
                "Socket.io will auto-reconnect"
            )
        }
    }
    
    private fun handleError(throwable: Throwable) {
        val error = when {
            throwable.message?.contains("401") == true ->
                WebSocketError.UNAUTHORIZED
            
            throwable.message?.contains("timeout") == true ->
                WebSocketError.TIMEOUT
            
            else -> WebSocketError.CONNECTION_FAILED
        }
        
        _connectionState.value = ConnectionState.DISCONNECTED
        
        scope.launch {
            _connectionEvents.emit(ConnectionEvent.Error(error))
        }
    }
    
    private fun closeSocket() {
        socket?.off()
        socket?.disconnect()
        socket = null
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
    
    /**
     * Преобразовать JSONArray в JsonArray
     */
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
    
    private fun listToJsonArray(list: List<*>): JsonArray {
        return JsonArray(
            list.map { value ->
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
            }
        )
    }
    
    private fun JsonObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        forEach { (key, value) ->
            result[key] = when (value) {
                is JsonObject -> value.toMap()
                is JsonArray -> value.toList()
                is JsonPrimitive -> value.toNativeValue()
            }
        }
        return result
    }
    
    private fun JsonArray.toList(): List<Any?> {
        return map { it.toNativeValue() }
    }
    
    private fun JsonElement.toNativeValue(): Any {
        return when (this) {
            is JsonPrimitive -> when {
                isString -> content
                content.toBooleanOrNull() != null -> content.toBoolean()
                content.toLongOrNull() != null -> content.toLong()
                content.toDoubleOrNull() != null -> content.toDouble()
                else -> content
            }
            
            is JsonObject -> toMap()
            is JsonArray -> toList()
        }
    }
    
    private fun String.toBooleanOrNull(): Boolean? {
        return when (lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }
}