package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchatnative.model.ChatThread
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.ThreadCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ThreadRepository(context: Context) {
    private val dao = AppDatabase.get(context).dao()

    val threads: Flow<List<ChatThread>> = dao.threads()

    suspend fun syncWithClients(clients: List<Client>) {
        dao.insertThreads(
            clients.map {
                ChatThread(
                    id = UUID.randomUUID().toString(),
                    title = it.name,
                    clientId = it.id,
                    createdAtMillis = System.currentTimeMillis(),
                )
            },
        )
    }

    suspend fun removeClient(clientId: String) {
        dao.deleteThreadsByClient(clientId)
    }

    suspend fun setCategory(threadId: String, category: ThreadCategory) {
        dao.setThreadCategory(threadId, category)
    }
}
