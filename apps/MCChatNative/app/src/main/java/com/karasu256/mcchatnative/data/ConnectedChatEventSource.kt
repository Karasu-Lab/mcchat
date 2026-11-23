package com.karasu256.mcchatnative.data

import com.karasu256.mcchat.common.CapturedPacket
import com.karasu256.mcchat.common.Origin
import com.karasu256.mcchat.common.WorldContext
import com.karasu256.mcchat.common.WorldData
import com.karasu256.mcchat.common.event.CapturedEvent
import com.karasu256.mcchatnative.data.event.AppChatSentEvent
import com.karasu256.mcchatnative.data.event.AppCommandSentEvent
import com.karasu256.mcchatnative.data.event.AppEventContext
import com.karasu256.mcchatnative.data.event.AppTypingStartedEvent
import com.karasu256.mcchatnative.data.event.AppTypingStoppedEvent
import com.karasu256.mcchatnative.data.event.ChatEventProducer
import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scan
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

object ConnectedChatEventSource : ChatEventSource {
    private val pending = ConcurrentHashMap<String, MutableList<String>>()

    override fun events(clientId: String): Flow<ChatEvent> = flow {
        val seen = HashSet<Long>()
        ConnectionManager.packets(clientId).collect { packet ->
            eventsOf(packet).forEach { event ->
                val chatEvent = (event as? ChatEventProducer)?.toChatEvent(packet.world())
                    ?: return@forEach
                when (chatEvent) {
                    is ChatEvent.Message ->
                        if (seen.add(chatEvent.id) && !consumePending(clientId, chatEvent.body)) {
                            emit(chatEvent)
                        }

                    is ChatEvent.WorldChanged -> if (seen.add(chatEvent.id)) emit(chatEvent)
                    else -> emit(chatEvent)
                }
            }
        }
    }

    override suspend fun send(clientId: String, message: ChatMessage) {
        val context = AppEventContext(clientId)
        val event = if (message.body.startsWith("/")) {
            AppCommandSentEvent(context, Origin.CLIENT, null, message.body)
        } else {
            AppChatSentEvent(context, Origin.CLIENT, null, message.body)
        }
        pending.getOrPut(clientId) { java.util.Collections.synchronizedList(ArrayList()) }
            .add(message.body)
        ConnectionManager.send(clientId, eventPacket(event))
    }

    override suspend fun sendTyping(clientId: String, isTyping: Boolean) {
        val context = AppEventContext(clientId)
        val event = if (isTyping) {
            AppTypingStartedEvent(context, Origin.CLIENT, null)
        } else {
            AppTypingStoppedEvent(context, Origin.CLIENT, null)
        }
        ConnectionManager.send(clientId, eventPacket(event))
    }

    override fun activeUsers(clientId: String): Flow<List<User>> =
        combine(
            ConnectionManager.packets(clientId).scan(WorldContext.EMPTY, ::applyPlayers),
            ConnectionManager.inWorld(clientId),
        ) { context, inWorld ->
            if (inWorld) {
                context.players().map { User(UserId(it.uuid().toString()), it.name()) }
            } else {
                emptyList()
            }
        }.distinctUntilChanged()

    private fun eventPacket(event: CapturedEvent) = CapturedPacket(
        CapturedPacket.Type.EVENT,
        Origin.CLIENT,
        WorldData.empty("", ""),
        Optional.of(event),
    )

    private fun eventsOf(packet: CapturedPacket): List<CapturedEvent> = when (packet.type()) {
        CapturedPacket.Type.SNAPSHOT -> packet.world().context().history()
        else -> packet.event().map { listOf(it) }.orElse(emptyList())
    }

    private fun applyPlayers(context: WorldContext, packet: CapturedPacket): WorldContext =
        when (packet.type()) {
            CapturedPacket.Type.SNAPSHOT -> packet.world().context()
            CapturedPacket.Type.RESET -> WorldContext.EMPTY
            CapturedPacket.Type.EVENT -> packet.event().map { context.apply(it, 0) }.orElse(context)
            else -> context
        }

    private fun consumePending(clientId: String, body: String): Boolean =
        pending[clientId]?.remove(body) == true
}
