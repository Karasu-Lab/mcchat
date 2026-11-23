package com.karasu256.mcchatnative.data.event

import com.karasu256.mcchat.common.MinecraftScreen
import com.karasu256.mcchat.common.Origin
import com.karasu256.mcchat.common.PlayerRef
import com.karasu256.mcchat.common.WorldData
import com.karasu256.mcchat.common.WorldIcon
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchat.common.event.CapturedEvent
import com.karasu256.mcchat.common.event.ChatReceivedEvent
import com.karasu256.mcchat.common.event.ChatSentEvent
import com.karasu256.mcchat.common.event.CommandExecutedEvent
import com.karasu256.mcchat.common.event.CommandSentEvent
import com.karasu256.mcchat.common.event.EventContext
import com.karasu256.mcchat.common.event.EventRegistry
import com.karasu256.mcchat.common.event.MessageEvent
import com.karasu256.mcchat.common.event.OfflineEvent
import com.karasu256.mcchat.common.event.OnlineEvent
import com.karasu256.mcchat.common.event.StatusEvent
import com.karasu256.mcchat.common.event.TypingEvent
import com.karasu256.mcchat.common.event.TypingStartedEvent
import com.karasu256.mcchat.common.event.TypingStoppedEvent
import com.karasu256.mcchat.common.event.WorldChangedEvent
import com.karasu256.mcchat.common.event.WorldIconEvent
import com.karasu256.mcchatnative.data.ConnectionManager
import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import com.karasu256.mcchatnative.model.World

object AppEvents {
    val registry: EventRegistry = EventRegistry()
        .register(AppChatReceivedEvent::class.java)
        .register(AppChatSentEvent::class.java)
        .register(AppCommandSentEvent::class.java)
        .register(AppCommandExecutedEvent::class.java)
        .register(AppTypingStartedEvent::class.java)
        .register(AppTypingStoppedEvent::class.java)
        .register(AppWorldChangedEvent::class.java)
        .register(AppOnlineEvent::class.java)
        .register(AppOfflineEvent::class.java)
        .register(AppWorldIconEvent::class.java)
}

private fun senderOf(event: CapturedEvent): User =
    event.player().orElse(null)?.let { User(UserId(it.uuid().toString()), it.name()) }
        ?: User(UserId("server"), "Server")

private fun messageOf(event: MessageEvent, isCommand: Boolean): ChatEvent? {
    val plain = event.text().plain()
    if (plain.isBlank()) return null
    val text = if (isCommand && !plain.startsWith("/")) ChatText.raw("/$plain") else event.text()
    return ChatEvent.Message(
        id = event.id().leastSignificantBits,
        sender = senderOf(event),
        text = text,
        replyId = text.replyTarget().orElse(null)?.leastSignificantBits,
        timestampMillis = event.timestampMillis(),
        eventId = event.id(),
    )
}

private fun typingOf(event: TypingEvent, isTyping: Boolean): ChatEvent? =
    if (event.player().isPresent && System.currentTimeMillis() - event.timestampMillis() < 5_000) {
        ChatEvent.Typing(senderOf(event), isTyping)
    } else {
        null
    }

private fun clientIdOf(event: CapturedEvent): String? =
    (event.context() as? AppEventContext)?.clientId

class AppChatReceivedEvent(context: EventContext?, origin: Origin, player: PlayerRef?, text: ChatText) :
    ChatReceivedEvent(context, origin, player, text), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = messageOf(this, isCommand = false)
}

class AppChatSentEvent(context: EventContext?, origin: Origin, player: PlayerRef?, text: ChatText) :
    ChatSentEvent(context, origin, player, text), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = messageOf(this, isCommand = false)
}

class AppCommandSentEvent(context: EventContext?, origin: Origin, player: PlayerRef?, content: String?) :
    CommandSentEvent(context, origin, player, content), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = messageOf(this, isCommand = true)
}

class AppCommandExecutedEvent(context: EventContext?, origin: Origin, player: PlayerRef?, content: String?) :
    CommandExecutedEvent(context, origin, player, content), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = messageOf(this, isCommand = true)
}

class AppTypingStartedEvent(context: EventContext?, origin: Origin, player: PlayerRef?) :
    TypingStartedEvent(context, origin, player), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = typingOf(this, isTyping = true)
}

class AppTypingStoppedEvent(context: EventContext?, origin: Origin, player: PlayerRef?) :
    TypingStoppedEvent(context, origin, player), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = typingOf(this, isTyping = false)
}

class AppWorldChangedEvent(context: EventContext?, origin: Origin, worldName: String?) :
    WorldChangedEvent(context, origin, worldName), ChatEventProducer {
    override fun toChatEvent(world: WorldData): ChatEvent? = ChatEvent.WorldChanged(
        id = id().leastSignificantBits,
        world = World(world.id(), content().orElse("")),
        timestampMillis = timestampMillis(),
    )
}

class AppOnlineEvent(context: EventContext?, origin: Origin, player: PlayerRef?, screen: MinecraftScreen?) :
    OnlineEvent(context, origin, player, screen) {
    override fun handle() = reportStatus(this, inWorld = true)
}

class AppOfflineEvent(context: EventContext?, origin: Origin, player: PlayerRef?, screen: MinecraftScreen?) :
    OfflineEvent(context, origin, player, screen) {
    override fun handle() = reportStatus(this, inWorld = false)
}

class AppWorldIconEvent(context: EventContext?, origin: Origin, icon: WorldIcon?) :
    WorldIconEvent(context, origin, icon) {
    override fun handle() {
        val clientId = clientIdOf(this) ?: return
        ConnectionManager.updateIcon(clientId, icon().orElse(null))
    }
}

private fun reportStatus(event: StatusEvent, inWorld: Boolean) {
    val clientId = clientIdOf(event) ?: return
    ConnectionManager.updateStatus(clientId, inWorld, event.screen().orElse(null), event.player().orElse(null))
}
