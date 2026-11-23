package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchatnative.model.ChatThread
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.ThreadCategory
import com.karasu256.mcchatnative.model.ThreadStoreData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ThreadRepository(context: Context) {
    private val store = DataStores.get(
        context,
        "threads.json",
        JsonOkioSerializer(ThreadStoreData.serializer(), ThreadStoreData()),
    )

    val threads: Flow<List<ChatThread>> = store.data.map { it.threads }

    suspend fun syncWithClients(clients: List<Client>) {
        store.updateData { data ->
            val known = data.threads.mapTo(HashSet()) { it.clientId }
            val created = clients
                .filter { it.id !in known }
                .map {
                    ChatThread(
                        id = UUID.randomUUID().toString(),
                        title = it.name,
                        clientId = it.id,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                }
            if (created.isEmpty()) data else data.copy(threads = data.threads + created)
        }
    }

    suspend fun removeClient(clientId: String) {
        store.updateData { data -> data.copy(threads = data.threads.filter { it.clientId != clientId }) }
    }

    suspend fun setCategory(threadId: String, category: ThreadCategory) {
        store.updateData { data ->
            data.copy(
                threads = data.threads.map { if (it.id == threadId) it.copy(category = category) else it },
            )
        }
    }
}
