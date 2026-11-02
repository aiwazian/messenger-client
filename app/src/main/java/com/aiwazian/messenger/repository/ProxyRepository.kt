/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.entity.ProxyEntity
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.enums.ProxyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyRepository @Inject constructor(
    private val appDatabase: AppDatabase
) {
    fun observeProxies(): Flow<List<ProxyServer>> {
        return appDatabase.proxyDao().observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProxy(id: Long): ProxyServer? {
        return appDatabase.proxyDao().getById(id)?.toDomain()
    }

    suspend fun addProxy(
        type: ProxyType,
        host: String,
        port: Int,
        username: String?,
        password: String?
    ): ProxyServer {
        val entity = ProxyEntity(
            type = type,
            host = host,
            port = port,
            username = username?.takeIf { it.isNotBlank() },
            password = password?.takeIf { it.isNotBlank() }
        )
        val id = appDatabase.proxyDao().insert(entity)

        return entity.copy(id = id).toDomain()
    }

    suspend fun updateProxy(proxy: ProxyServer): ProxyServer {
        appDatabase.proxyDao().update(
            ProxyEntity(
                id = proxy.id,
                type = proxy.type,
                host = proxy.host,
                port = proxy.port,
                username = proxy.username,
                password = proxy.password
            )
        )
        return proxy
    }

    suspend fun deleteProxy(proxy: ProxyServer) {
        appDatabase.proxyDao().delete(
            ProxyEntity(
                id = proxy.id,
                type = proxy.type,
                host = proxy.host,
                port = proxy.port,
                username = proxy.username,
                password = proxy.password
            )
        )
    }
}

private fun ProxyEntity.toDomain(): ProxyServer {
    return ProxyServer(
        id = id,
        type = type,
        host = host,
        port = port,
        username = username,
        password = password
    )
}
