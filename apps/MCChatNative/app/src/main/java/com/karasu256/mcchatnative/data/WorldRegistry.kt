package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchatnative.model.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object WorldRegistry {
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + ErrorReporter.coroutineHandler)
    private val _worlds = MutableStateFlow<Map<String, World>>(emptyMap())
    private var dao: AppDao? = null

    val worlds: StateFlow<Map<String, World>> = _worlds.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (dao != null) return
        val worldDao = AppDatabase.get(context).dao()
        dao = worldDao
        scope.launch {
            worldDao.worlds().collect { list -> _worlds.update { list.associateBy { it.id } + it } }
        }
    }

    fun register(world: World) {
        if (_worlds.value[world.id]?.name == world.name) return
        _worlds.update { it + (world.id to world) }
        scope.launch { dao?.upsertWorld(world) }
    }
}
