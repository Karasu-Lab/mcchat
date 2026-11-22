package com.karasu256.mcchatnative.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.karasu256.mcchatnative.model.ChatThread
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.ThreadCategory
import com.karasu256.mcchatnative.model.ThreadStoreData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
object ThreadStoreSerializer : Serializer<ThreadStoreData> {
    override val defaultValue: ThreadStoreData = ThreadStoreData()

    override suspend fun readFrom(input: InputStream): ThreadStoreData =
        try {
            Json.decodeFromStream(input)
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read thread store", e)
        }

    override suspend fun writeTo(t: ThreadStoreData, output: OutputStream) =
        Json.encodeToStream(t, output)
}

private val Context.threadDataStore: DataStore<ThreadStoreData> by dataStore(
    fileName = "threads.json",
    serializer = ThreadStoreSerializer,
)

class ThreadRepository(context: Context) {
    private val store = context.applicationContext.threadDataStore

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

    suspend fun setCategory(threadId: String, category: ThreadCategory) {
        store.updateData { data ->
            data.copy(
                threads = data.threads.map { if (it.id == threadId) it.copy(category = category) else it },
            )
        }
    }
}
