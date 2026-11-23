package com.karasu256.mcchatnative.data

import com.karasu256.mcchat.common.CapturedData
import com.karasu256.mcchat.common.CapturedPacket
import com.karasu256.mcchat.common.Origin
import com.karasu256.mcchat.common.WorldContext
import com.karasu256.mcchat.common.WorldData
import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import com.karasu256.mcchatnative.model.World
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object ConnectedChatEventSource : ChatEventSource {
    private val serverUser = User(UserId("server"), "Server")
    private val commandKinds =
        setOf(CapturedData.Kind.COMMAND_SENT, CapturedData.Kind.COMMAND_EXECUTED)
    private val chatKinds =
        setOf(CapturedData.Kind.CHAT_RECEIVED, CapturedData.Kind.CHAT_SENT) + commandKinds
    private val pending = ConcurrentHashMap<String, MutableList<String>>()

    override fun events(clientId: String): Flow<ChatEvent> = flow {
        val seen = HashSet<Long>()
        ConnectionManager.packets(clientId).collect { packet ->
            val worldChange = packet.data().orElse(null)
                ?.takeIf { packet.type() == CapturedPacket.Type.RESET && it.kind() == CapturedData.Kind.WORLD_CHANGED }
            if (worldChange != null && seen.add(worldChange.id().leastSignificantBits)) {
                emit(
                    ChatEvent.WorldChanged(
                        worldChange.id().leastSignificantBits,
                        World(packet.world().id(), worldChange.content().orElse("")),
                        worldChange.timestampMillis(),
                    ),
                )
            }
            typingOf(packet)?.let { data ->
                emit(
                    ChatEvent.Typing(
                        senderOf(data),
                        data.kind() == CapturedData.Kind.TYPING_STARTED
                    )
                )
            }
            chatOf(packet).forEach { data ->
                val body = bodyOf(data) ?: return@forEach
                val id = data.id().leastSignificantBits
                if (seen.add(id) && !consumePending(clientId, body)) {
                    emit(
                        ChatEvent.Message(
                            id,
                            senderOf(data),
                            body,
                            timestampMillis = data.timestampMillis()
                        )
                    )
                }
            }
        }
    }

    override suspend fun send(clientId: String, message: ChatMessage) {
        val kind =
            if (message.body.startsWith("/")) CapturedData.Kind.COMMAND_SENT else CapturedData.Kind.CHAT_SENT
        pending.getOrPut(clientId) { java.util.Collections.synchronizedList(ArrayList()) }
            .add(message.body)
        val packet = CapturedPacket(
            CapturedPacket.Type.EVENT,
            Origin.CLIENT,
            WorldData.empty("", ""),
            Optional.of(CapturedData.of(Origin.CLIENT, kind, null, message.body)),
        )
        ConnectionManager.send(clientId, packet)
    }

    override suspend fun sendTyping(clientId: String, isTyping: Boolean) {
        val kind =
            if (isTyping) CapturedData.Kind.TYPING_STARTED else CapturedData.Kind.TYPING_STOPPED
        val packet = CapturedPacket(
            CapturedPacket.Type.EVENT,
            Origin.CLIENT,
            WorldData.empty("", ""),
            Optional.of(CapturedData.of(Origin.CLIENT, kind, null, null)),
        )
        ConnectionManager.send(clientId, packet)
    }

    override fun activeUsers(clientId: String): Flow<List<User>> =
        ConnectionManager.packets(clientId)
            .scan(WorldContext.EMPTY, ::applyPlayers)
            .map { context ->
                context.players().map { User(UserId(it.uuid().toString()), it.name()) }
            }
            .distinctUntilChanged()

    private fun typingOf(packet: CapturedPacket): CapturedData? =
        packet.data().orElse(null)?.takeIf {
            packet.type() == CapturedPacket.Type.EVENT &&
                    (it.kind() == CapturedData.Kind.TYPING_STARTED || it.kind() == CapturedData.Kind.TYPING_STOPPED) &&
                    it.player().isPresent &&
                    System.currentTimeMillis() - it.timestampMillis() < 5_000
        }

    private fun chatOf(packet: CapturedPacket): List<CapturedData> = when (packet.type()) {
        CapturedPacket.Type.SNAPSHOT -> packet.world().context().history()
            .filter { it.kind() in chatKinds }

        CapturedPacket.Type.EVENT -> listOfNotNull(
            packet.data().orElse(null)?.takeIf { it.kind() in chatKinds })

        else -> emptyList()
    }

    private fun applyPlayers(context: WorldContext, packet: CapturedPacket): WorldContext =
        when (packet.type()) {
            CapturedPacket.Type.SNAPSHOT -> packet.world().context()
            CapturedPacket.Type.RESET -> WorldContext.EMPTY
            CapturedPacket.Type.EVENT -> {
                val data = packet.data().orElse(null)
                val playerKinds = setOf(
                    CapturedData.Kind.PLAYER_LIST,
                    CapturedData.Kind.PLAYER_JOINED,
                    CapturedData.Kind.PLAYER_LEFT,
                )
                if (data != null && data.kind() in playerKinds) context.apply(data, 0) else context
            }

            else -> context
        }

    private fun bodyOf(data: CapturedData): String? {
        val content = data.content().orElse(null)?.takeIf { it.isNotBlank() } ?: return null
        return if (data.kind() in commandKinds && !content.startsWith("/")) "/$content" else content
    }

    private fun senderOf(data: CapturedData): User =
        data.player().orElse(null)?.let { User(UserId(it.uuid().toString()), it.name()) }
            ?: serverUser

    private fun consumePending(clientId: String, body: String): Boolean =
        pending[clientId]?.remove(body) == true
}
