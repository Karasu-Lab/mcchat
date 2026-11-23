package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchatnative.model.ConnectionInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ConnectionStore(context: Context) {
    private val dao = AppDatabase.get(context).dao()

    val connections: Flow<List<ConnectionInfo>> = dao.connections()

    suspend fun add(host: String, port: Int) {
        dao.insertConnection(ConnectionInfo(UUID.randomUUID().toString(), host, port))
    }

    suspend fun setUser(id: String, userId: String, userName: String) {
        dao.setConnectionUser(id, userId, userName)
    }

    suspend fun remove(id: String) {
        dao.deleteConnection(id)
    }
}
