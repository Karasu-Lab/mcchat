package com.karasu256.mcchatnative.data

import android.content.Context
import com.karasu256.mcchat.common.CapturedPacket
import com.karasu256.mcchat.common.MinecraftChatScreen
import com.karasu256.mcchat.common.MinecraftScreen
import com.karasu256.mcchat.common.Origin
import com.karasu256.mcchat.common.PlayerRef
import com.karasu256.mcchat.common.WorldData
import com.karasu256.mcchat.common.WorldIcon
import com.karasu256.mcchatnative.data.event.AppEventContext
import com.karasu256.mcchatnative.data.event.AppEvents
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.ConnectionInfo
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import com.karasu256.mcchatnative.model.World
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object ConnectionManager {
    private const val InitialBackoffMillis = 1_000L
    private const val ConnectTimeoutMillis = 5_000

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + ErrorReporter.coroutineHandler)
    private val loops = HashMap<String, Job>()
    private val sockets = ConcurrentHashMap<String, Socket>()
    private val streams = ConcurrentHashMap<String, MutableSharedFlow<CapturedPacket>>()
    private val connections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    private val online = MutableStateFlow<Set<String>>(emptySet())
    private val inWorld = MutableStateFlow<Set<String>>(emptySet())
    private val inChatScreen = MutableStateFlow<Set<String>>(emptySet())
    private val icons = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    private val selves = MutableStateFlow<Map<String, User>>(emptyMap())
    private val requestedSkins = ConcurrentHashMap.newKeySet<String>()
    private val ping = CapturedPacket(
        CapturedPacket.Type.PING,
        Origin.CLIENT,
        WorldData.empty("", ""),
        Optional.empty()
    )
    private var store: ConnectionStore? = null

    val clients: StateFlow<List<Client>> = combine(connections, online, icons) { list, up, icons ->
        list.map { Client(it.id, it.host, "${it.host}:${it.port}", it.id in up, icons[it.id]) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    @Synchronized
    fun start(context: Context) {
        if (store != null) return
        SkinCache.init(context)
        ChatHistoryStore.init(context)
        WorldRegistry.init(context)
        val connectionStore = ConnectionStore(context)
        store = connectionStore
        scope.launch {
            connectionStore.connections.collect { list ->
                connections.value = list
                selves.update { current ->
                    current + list
                        .filter { it.userId != null && it.id !in current }
                        .associate { it.id to User(UserId(it.userId!!), it.userName.orEmpty()) }
                }
                connect(list)
            }
        }
    }

    suspend fun add(host: String, port: Int) {
        store?.add(host, port)
    }

    suspend fun remove(id: String) {
        synchronized(this) { loops.remove(id) }?.cancel()
        sockets.remove(id)?.close()
        streams.remove(id)
        online.update { it - id }
        inWorld.update { it - id }
        inChatScreen.update { it - id }
        icons.update { it - id }
        store?.remove(id)
    }

    suspend fun probe(host: String, port: Int): Boolean {
        if (port !in 1..65535) return false
        val result = CompletableDeferred<Boolean>()
        scope.launch {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), ConnectTimeoutMillis)
                socket.soTimeout = ConnectTimeoutMillis
                val line = socket.getInputStream().bufferedReader(Charsets.UTF_8).readLine()
                result.complete(line != null && CapturedPacket.decode(line).isPresent)
            }
        }.invokeOnCompletion { result.complete(false) }
        return result.await()
    }

    fun findClient(id: String): Client? = clients.value.firstOrNull { it.id == id }

    fun self(clientId: String): Flow<User?> = selves.map { it[clientId] }

    fun selfOf(clientId: String): User? = selves.value[clientId]

    fun inWorld(clientId: String): Flow<Boolean> = inWorld.map { clientId in it }

    fun inChatScreen(clientId: String): Flow<Boolean> = inChatScreen.map { clientId in it }

    fun requestStatus(clientId: String) = send(clientId, ping)

    fun hostOf(id: String): String? = connections.value.firstOrNull { it.id == id }?.host

    fun packets(clientId: String): Flow<CapturedPacket> = stream(clientId)

    fun send(clientId: String, packet: CapturedPacket) {
        val socket = sockets[clientId] ?: return
        scope.launch {
            synchronized(socket) {
                socket.getOutputStream().apply {
                    write((packet.encode() + "\n").toByteArray(Charsets.UTF_8))
                    flush()
                }
            }
        }
    }

    private fun stream(clientId: String): MutableSharedFlow<CapturedPacket> =
        streams.getOrPut(clientId) { MutableSharedFlow(replay = 256, extraBufferCapacity = 256) }

    @Synchronized
    private fun connect(list: List<ConnectionInfo>) {
        list.filter { it.id !in loops }.forEach { info ->
            loops[info.id] = scope.launch { keepConnected(info) }
        }
    }

    private suspend fun keepConnected(info: ConnectionInfo) {
        var backoff = InitialBackoffMillis
        while (currentCoroutineContext().isActive) {
            supervisorScope {
                launch {
                    Socket().use { socket ->
                        socket.connect(
                            InetSocketAddress(info.host, info.port),
                            ConnectTimeoutMillis
                        )
                        sockets[info.id] = socket
                        online.update { it + info.id }
                        backoff = InitialBackoffMillis
                        send(info.id, ping)
                        socket.getInputStream().bufferedReader(Charsets.UTF_8)
                            .forEachLine { receive(info.id, it) }
                    }
                }
            }
            sockets.remove(info.id)
            online.update { it - info.id }
            inWorld.update { it - info.id }
            inChatScreen.update { it - info.id }
            delay(backoff)
            backoff = minOf(backoff * 2, 30_000L)
        }
    }

    private fun receive(clientId: String, line: String) {
        val packet = CapturedPacket.decode(line, AppEvents.registry).orElse(null) ?: return
        packet.bind(AppEventContext(clientId))
        if (packet.world().id().isNotEmpty()) {
            WorldRegistry.register(World(packet.world().id(), packet.world().worldName()))
        }
        packet.event().ifPresent { it.handle() }
        if (packet.type() == CapturedPacket.Type.STATUS) return
        if (packet.type() == CapturedPacket.Type.RESET || packet.type() == CapturedPacket.Type.SNAPSHOT) {
            updateIcon(clientId, packet.world().context().icon().orElse(null))
        }
        cacheSkins(packet)
        stream(clientId).tryEmit(packet)
    }

    internal fun updateStatus(clientId: String, online: Boolean, screen: MinecraftScreen?, player: PlayerRef?) {
        inWorld.update { if (online) it + clientId else it - clientId }
        inChatScreen.update { if (screen is MinecraftChatScreen) it + clientId else it - clientId }
        player?.let {
            val user = User(UserId(it.uuid().toString()), it.name())
            if (selves.value[clientId] != user) {
                selves.update { current -> current + (clientId to user) }
                scope.launch { store?.setUser(clientId, user.id.value, user.name) }
            }
        }
    }

    internal fun updateIcon(clientId: String, icon: WorldIcon?) {
        icons.update { if (icon != null) it + (clientId to icon.png()) else it - clientId }
    }

    private fun cacheSkins(packet: CapturedPacket) {
        val players = packet.world().context().players() +
                packet.event().map { it.referencedPlayers() }.orElse(emptyList())
        players.forEach { cacheSkin(it) }
    }

    private fun cacheSkin(player: PlayerRef) {
        val id = player.uuid().toString()
        val skin = player.skin().orElse(null)
        if (skin != null) {
            SkinCache.put(id, skin.png())
        } else if (SkinCache.get(id) == null && requestedSkins.add(id)) {
            scope.launch { MojangSkinFetcher.fetch(id)?.let { SkinCache.put(id, it) } }
                .invokeOnCompletion { if (it != null) requestedSkins.remove(id) }
        }
    }
}
