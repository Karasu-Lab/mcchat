package com.karasu256.mcchatnative.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasu256.mcchat.common.component.ChatText
import com.karasu256.mcchat.common.component.ReplyData
import com.karasu256.mcchatnative.data.ChatEventSource
import com.karasu256.mcchatnative.data.ChatHistoryStore
import com.karasu256.mcchatnative.data.ChatRepository
import com.karasu256.mcchatnative.data.ClientRepository
import com.karasu256.mcchatnative.data.ConnectionManager
import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.MessageReaction
import com.karasu256.mcchatnative.model.ReactionData
import com.karasu256.mcchatnative.model.ReactionIntent
import com.karasu256.mcchatnative.model.Reactions
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import com.karasu256.mcchatnative.model.World
import com.karasu256.mcchatnative.model.uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private fun List<MessageReaction>.applied(intent: ReactionIntent): List<MessageReaction> {
    val current = firstOrNull { it.data == intent.data }
    if (intent.isAdded) {
        if (current == null) return this + MessageReaction(intent.data, listOf(intent.userId))
        if (current.isReactedBy(intent.userId)) return this
        return map { if (it === current) it.copy(userIds = it.userIds + intent.userId) else it }
    }
    if (current == null) return this
    val remaining = current.userIds - intent.userId
    if (remaining.isEmpty()) return this - current
    return map { if (it === current) it.copy(userIds = remaining) else it }
}

data class ChatUiState(
    val currentUserId: UserId,
    val client: Client? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val typingUsers: List<User> = emptyList(),
    val reactions: List<ReactionData> = emptyList(),
    val activeUsers: List<User> = emptyList(),
    val replyingToId: Long? = null,
    val mentionEnabled: Boolean = true,
    val inWorld: Boolean = false,
    val inChatScreen: Boolean = false,
    val headerUserLimit: Int = 5,
) {
    val replyTarget: ChatMessage? get() = messages.firstOrNull { it.id == replyingToId }
    val replyableIds: Set<Long>
        get() {
            val currentWorldId = messages.lastOrNull { it.world != null }?.world?.id
            var worldId: String? = null
            return buildSet {
                messages.forEach { message ->
                    val divider = message.world
                    if (divider != null) {
                        worldId = divider.id
                    } else if (worldId == currentWorldId) {
                        add(message.id)
                    }
                }
            }
        }
    val canSend: Boolean get() = inWorld && draft.isNotBlank() && inChatScreen
    val headerUsers: List<User> get() = activeUsers.take(headerUserLimit)
}

class ChatViewModel(
    private val clientId: String,
    private val source: ChatEventSource = ChatRepository,
    reactionLimit: Int = 6,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            currentUserId = (ConnectionManager.selfOf(clientId) ?: ClientRepository.currentUser).id,
            client = ConnectionManager.findClient(clientId),
            reactions = Reactions.generate().take(reactionLimit),
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _outgoingTyping = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    val outgoingTyping: SharedFlow<Boolean> = _outgoingTyping.asSharedFlow()

    private val _outgoingReactions = MutableSharedFlow<ReactionIntent>(extraBufferCapacity = 8)
    val outgoingReactions: SharedFlow<ReactionIntent> = _outgoingReactions.asSharedFlow()

    private var selfUser = ConnectionManager.selfOf(clientId) ?: ClientRepository.currentUser
    private var clearedAt = 0L
    private var nextLocalId = -1L

    init {
        viewModelScope.launch {
            ConnectionManager.clients.collect { list ->
                _uiState.update { state -> state.copy(client = list.firstOrNull { it.id == clientId }) }
            }
        }
        viewModelScope.launch {
            ConnectionManager.inWorld(clientId).collect { inWorld ->
                _uiState.update { it.copy(inWorld = inWorld) }
            }
        }
        viewModelScope.launch {
            ConnectionManager.inChatScreen(clientId).collect { inChatScreen ->
                _uiState.update { it.copy(inChatScreen = inChatScreen) }
            }
        }
        viewModelScope.launch {
            while (true) {
                ConnectionManager.requestStatus(clientId)
                delay(1_000L)
            }
        }
        viewModelScope.launch {
            ConnectionManager.self(clientId).collect { user ->
                if (user != null) {
                    selfUser = user
                    _uiState.update { it.copy(currentUserId = user.id) }
                }
            }
        }
        viewModelScope.launch {
            outgoingTyping.collect { source.sendTyping(clientId, it) }
        }
        viewModelScope.launch {
            source.activeUsers(clientId).collect { users ->
                _uiState.update { it.copy(activeUsers = users) }
            }
        }
        viewModelScope.launch {
            ConnectionManager.hostOf(clientId)?.let { host ->
                clearedAt = ChatHistoryStore.clearedAt(host)
                val restored = ChatHistoryStore.load(host)
                nextLocalId = minOf(restored.minOfOrNull { it.id } ?: 0, 0) - 1
                _uiState.update { it.copy(messages = restored + it.messages) }
                launch {
                    _uiState.map { it.messages }.distinctUntilChanged()
                        .collect { ChatHistoryStore.save(host, it) }
                }
            }
            source.events(clientId).collect { event ->
                when (event) {
                    is ChatEvent.Message ->
                        receiveMessage(
                            event.id,
                            event.sender,
                            event.text,
                            event.replyId,
                            event.timestampMillis,
                            event.eventId,
                        )

                    is ChatEvent.Delivered -> markDelivered(event.body, event.eventId)

                    is ChatEvent.WorldChanged -> receiveWorldChange(
                        event.id,
                        event.world,
                        event.timestampMillis
                    )

                    is ChatEvent.Typing -> receiveTyping(event.user, event.isTyping)
                    is ChatEvent.Reaction -> receiveReaction(event.intent)
                }
            }
        }
    }

    fun onDraftChange(draft: String) {
        _uiState.update { it.copy(draft = draft) }
        if (draft.isBlank()) {
            stopLocalTyping()
        } else {
            startLocalTyping()
        }
    }

    fun sendMessage() {
        if (!_uiState.value.canSend) return
        stopLocalTyping()
        val state = _uiState.value
        var text = ChatText.parse(state.draft.trim(), state.activeUsers.toPlayerRefs())
        val target = state.replyTarget
        val targetEvent = target?.eventId
        if (target != null && targetEvent != null) {
            text = text.withReply(
                ReplyData.reply(targetEvent, target.sender.id.uuid(), state.mentionEnabled),
            )
        }
        val message = newMessage(
            id = nextLocalId--,
            sender = selfUser,
            text = text,
            replyId = state.replyingToId,
        )
        _uiState.update {
            it.copy(
                draft = "",
                replyingToId = null,
                mentionEnabled = true,
                messages = it.messages + message,
            )
        }
        viewModelScope.launch { source.send(clientId, message) }
    }

    fun onReplyToggle(messageId: Long) {
        _uiState.update { state ->
            if (messageId !in state.replyableIds) {
                state
            } else if (state.replyingToId == messageId) {
                state.copy(replyingToId = null, mentionEnabled = true)
            } else {
                state.copy(replyingToId = messageId, mentionEnabled = true)
            }
        }
    }

    fun onReplyCancel() {
        _uiState.update { it.copy(replyingToId = null, mentionEnabled = true) }
    }

    fun onMentionToggle() {
        _uiState.update { it.copy(mentionEnabled = !it.mentionEnabled) }
    }

    fun onReact(messageId: Long, data: ReactionData) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val currentUserId = _uiState.value.currentUserId
        val isAdded = message.reactions.none { it.data == data && it.isReactedBy(currentUserId) }
        val intent = ReactionIntent(messageId, data, currentUserId, isAdded)
        updateReactions(messageId) { it.applied(intent) }
        _outgoingReactions.tryEmit(intent)
    }

    fun receiveReaction(intent: ReactionIntent) {
        updateReactions(intent.messageId) { it.applied(intent) }
    }

    private fun updateReactions(
        messageId: Long,
        transform: (List<MessageReaction>) -> List<MessageReaction>,
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) message.copy(reactions = transform(message.reactions)) else message
                },
            )
        }
    }

    fun receiveMessage(
        id: Long,
        sender: User,
        text: ChatText,
        replyId: Long? = null,
        timestampMillis: Long = System.currentTimeMillis(),
        eventId: UUID? = null,
    ) {
        clearRemoteTyping(sender.id)
        if (timestampMillis <= clearedAt) return
        _uiState.update {
            if (it.messages.any { message -> message.id == id }) {
                it
            } else {
                it.copy(
                    messages = it.messages + newMessage(
                        id,
                        sender,
                        text,
                        replyId,
                        timestampMillis,
                        eventId,
                    )
                )
            }
        }
    }

    private fun markDelivered(body: String, eventId: UUID) {
        _uiState.update { state ->
            val index = state.messages.indexOfFirst {
                it.eventId == null && it.sender.id == selfUser.id && it.text.plain() == body
            }
            if (index < 0) {
                state
            } else {
                state.copy(
                    messages = state.messages.toMutableList().also {
                        it[index] = it[index].copy(eventId = eventId)
                    },
                )
            }
        }
    }

    private fun receiveWorldChange(id: Long, world: World, timestampMillis: Long) {
        if (timestampMillis <= clearedAt) return
        _uiState.update {
            if (it.messages.any { message -> message.id == id }) {
                it
            } else {
                val next = it.copy(
                    messages = it.messages + ChatMessage(
                        id = id,
                        sender = User(UserId("world"), world.name),
                        body = world.name,
                        timestampMillis = timestampMillis,
                        world = world,
                    ),
                )
                if (next.replyingToId in next.replyableIds) {
                    next
                } else {
                    next.copy(replyingToId = null, mentionEnabled = true)
                }
            }
        }
    }

    fun clearHistory() {
        clearedAt = System.currentTimeMillis()
        _uiState.update { it.copy(messages = emptyList()) }
        ConnectionManager.hostOf(clientId)?.let { host ->
            viewModelScope.launch { ChatHistoryStore.clear(host) }
        }
    }

    fun receiveTyping(user: User, isTyping: Boolean) {
        if (isTyping) {
            _uiState.update { state ->
                if (state.typingUsers.any { it.id == user.id }) state else state.copy(typingUsers = state.typingUsers + user)
            }
        } else {
            clearRemoteTyping(user.id)
        }
    }

    private fun startLocalTyping() {
        val self = selfUser
        if (_uiState.value.typingUsers.none { it.id == self.id }) {
            _uiState.update { it.copy(typingUsers = it.typingUsers + self) }
            _outgoingTyping.tryEmit(true)
        }
    }

    private fun stopLocalTyping() {
        val selfId = selfUser.id
        if (_uiState.value.typingUsers.any { it.id == selfId }) {
            _uiState.update { state -> state.copy(typingUsers = state.typingUsers.filter { it.id != selfId }) }
            _outgoingTyping.tryEmit(false)
        }
    }

    private fun clearRemoteTyping(userId: UserId) {
        _uiState.update { state -> state.copy(typingUsers = state.typingUsers.filter { it.id != userId }) }
    }

    private fun newMessage(
        id: Long,
        sender: User,
        text: ChatText,
        replyId: Long?,
        timestampMillis: Long = System.currentTimeMillis(),
        eventId: UUID? = null,
    ) = ChatMessage(
        id = id,
        sender = sender,
        body = text.plain(),
        timestampMillis = timestampMillis,
        replyId = replyId,
        text = text,
        eventId = eventId,
    )
}
