package com.karasu256.mcchatnative.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import java.util.concurrent.ConcurrentHashMap

class JsonOkioSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : OkioSerializer<T> {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(source: BufferedSource): T =
        try {
            json.decodeFromString(serializer, source.readUtf8())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read ${serializer.descriptor.serialName}", e)
        }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(serializer, t))
    }
}

object DataStores {
    private val stores = ConcurrentHashMap<String, DataStore<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(
        context: Context,
        fileName: String,
        serializer: JsonOkioSerializer<T>
    ): DataStore<T> =
        stores.getOrPut(fileName) {
            DataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, serializer) {
                    context.applicationContext.filesDir.resolve("datastore")
                        .resolve(fileName).absolutePath.toPath()
                },
            )
        } as DataStore<T>
}
