package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchatnative.model.ConnectionInfo
import com.karasu256.mcchatnative.model.ConnectionStoreData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConnectionStore(context: Context) {
    private val store = DataStores.get(
        context,
        "connections.json",
        JsonOkioSerializer(ConnectionStoreData.serializer(), ConnectionStoreData()),
    )

    val connections: Flow<List<ConnectionInfo>> = store.data.map { it.connections }

    suspend fun add(host: String, port: Int) {
        store.updateData { data ->
            if (data.connections.any { it.host == host && it.port == port }) {
                data
            } else {
                data.copy(
                    connections = data.connections + ConnectionInfo(
                        UUID.randomUUID().toString(), host, port
                    )
                )
            }
        }
    }

    suspend fun setUser(id: String, userId: String, userName: String) {
        store.updateData { data ->
            data.copy(
                connections = data.connections.map {
                    if (it.id == id) it.copy(userId = userId, userName = userName) else it
                },
            )
        }
    }

    suspend fun remove(id: String) {
        store.updateData { data -> data.copy(connections = data.connections.filter { it.id != id }) }
    }
}
