package com.karasu256.mcchatnative.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.karasu256.mcchatnative.model.World
import com.karasu256.mcchatnative.model.WorldStoreData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object WorldRegistry {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + ErrorReporter.coroutineHandler)
    private val _worlds = MutableStateFlow<Map<String, World>>(emptyMap())
    private var store: DataStore<WorldStoreData>? = null

    val worlds: StateFlow<Map<String, World>> = _worlds.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val worldStore = DataStores.get(
            context,
            "worlds.json",
            JsonOkioSerializer(WorldStoreData.serializer(), WorldStoreData()),
        )
        store = worldStore
        scope.launch {
            worldStore.data.collect { data -> _worlds.update { data.worlds.associateBy { it.id } + it } }
        }
    }

    fun register(world: World) {
        if (_worlds.value[world.id]?.name == world.name) return
        _worlds.update { it + (world.id to world) }
        scope.launch {
            store?.updateData { data -> data.copy(worlds = data.worlds.filter { it.id != world.id } + world) }
        }
    }
}
