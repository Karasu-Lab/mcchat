package com.karasu256.mcchatnative.model

import androidx.annotation.StringRes
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchatnative.R
import java.util.UUID

data class Client(
    val id: String,
    val name: String,
    val address: String,
    val isOnline: Boolean,
    val icon: ByteArray? = null,
)

@Entity(tableName = "connections", indices = [Index("host", "port", unique = true)])
data class ConnectionInfo(
    @PrimaryKey val id: String,
    val host: String,
    val port: Int,
    val userId: String? = null,
    val userName: String? = null,
)

@Entity(tableName = "messages", primaryKeys = ["host", "position"])
data class StoredMessage(
    val host: String,
    val position: Int,
    val id: Long,
    val senderId: String,
    val senderName: String,
    val body: String,
    val timestampMillis: Long,
    val replyId: Long? = null,
    val text: String? = null,
    val eventId: String? = null,
    @Embedded(prefix = "world_") val world: World? = null,
)

@Entity(tableName = "history_cleared")
data class HistoryCleared(
    @PrimaryKey val host: String,
    val clearedAt: Long,
)

@Entity(tableName = "worlds")
data class World(@PrimaryKey val id: String, val name: String)

@JvmInline
value class UserId(val value: String)

data class User(val id: UserId, val name: String)

data class ChatMessage(
    val id: Long,
    val sender: User,
    val body: String,
    val timestampMillis: Long,
    val replyId: Long? = null,
    val text: ChatText = ChatText.raw(body),
    val eventId: UUID? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val world: World? = null,
) {
    fun isSentBy(userId: UserId): Boolean = sender.id == userId

    fun isMentioning(userId: UserId): Boolean = text.notifies(userId.uuid(), sender.id.uuid())
}

fun UserId.uuid(): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

enum class ThreadCategory(@StringRes val labelRes: Int) {
    Pinned(R.string.drawer_pinned),
    Recent(R.string.drawer_recent),
}

@Entity(tableName = "threads", indices = [Index("clientId", unique = true)])
data class ChatThread(
    @PrimaryKey val id: String,
    val title: String,
    val clientId: String,
    val category: ThreadCategory = ThreadCategory.Recent,
    val createdAtMillis: Long,
) {
    val isPinned: Boolean get() = category == ThreadCategory.Pinned
}

sealed interface ChatEvent {
    data class Message(
        val id: Long,
        val sender: User,
        val text: ChatText,
        val replyId: Long? = null,
        val timestampMillis: Long = System.currentTimeMillis(),
        val eventId: UUID? = null,
    ) : ChatEvent {
        val body: String get() = text.plain()
    }

    data class Delivered(val body: String, val eventId: UUID) : ChatEvent

    data class WorldChanged(
        val id: Long,
        val world: World,
        val timestampMillis: Long,
    ) : ChatEvent

    data class Typing(val user: User, val isTyping: Boolean) : ChatEvent
    data class Reaction(val intent: ReactionIntent) : ChatEvent
}

data class DrawerSection(
    val category: ThreadCategory,
    val threads: List<ChatThread>,
)

data class DrawerUser(
    val name: String,
    val subtitle: String,
)
