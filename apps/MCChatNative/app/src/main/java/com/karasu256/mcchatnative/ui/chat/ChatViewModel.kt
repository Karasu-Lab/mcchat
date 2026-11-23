package com.karasu256.mcchatnative.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasu256.mcchatnative.data.ChatEventSource
import com.karasu256.mcchatnative.data.ChatHistoryStore
import com.karasu256.mcchatnative.data.ChatRepository
import com.karasu256.mcchatnative.data.ClientRepository
import com.karasu256.mcchatnative.data.ConnectionManager
import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.MessageReaction
import com.karasu256.mcchatnative.model.ReactionData
import com.karasu256.mcchatnative.model.ReactionIntent
import com.karasu256.mcchatnative.model.Reactions
import com.karasu256.mcchatnative.model.UserId
import kotlinx.coroutines.Job
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

private const val LocalTypingIdleMillis = 3_000L
private const val RemoteTypingTimeoutMillis = 8_000L
private const val ReactionLimit = 6
private const val HeaderUserLimit = 5

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
) {
    val replyTarget: ChatMessage? get() = messages.firstOrNull { it.id == replyingToId }
    val canSend: Boolean get() = inWorld && draft.isNotBlank()
    val headerUsers: List<User> get() = activeUsers.take(HeaderUserLimit)
}

class ChatViewModel(
    private val clientId: String,
    private val source: ChatEventSource = ChatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            currentUserId = (ConnectionManager.selfOf(clientId) ?: ClientRepository.currentUser).id,
            client = ConnectionManager.findClient(clientId),
            reactions = Reactions.generate().take(ReactionLimit),
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
    private var localTypingIdle: Job? = null
    private val remoteTypingTimeouts = HashMap<UserId, Job>()

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
                    _uiState.map { it.messages }.distinctUntilChanged().collect { ChatHistoryStore.save(host, it) }
                }
            }
            source.events(clientId).collect { event ->
                when (event) {
                    is ChatEvent.Message ->
                        receiveMessage(event.id, event.sender, event.body, event.replyId, event.isReply, event.timestampMillis)
                    is ChatEvent.WorldChanged -> receiveWorldChange(event.id, event.worldName, event.timestampMillis)
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
        val body = _uiState.value.draft.trim()
        stopLocalTyping()
        val state = _uiState.value
        val message = newMessage(
            id = nextLocalId--,
            sender = selfUser,
            body = body,
            replyId = state.replyingToId,
            isReply = state.replyingToId != null && state.mentionEnabled,
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
            if (state.replyingToId == messageId) {
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
        body: String,
        replyId: Long? = null,
        isReply: Boolean = false,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        clearRemoteTyping(sender.id)
        if (timestampMillis <= clearedAt) return
        _uiState.update {
            if (it.messages.any { message -> message.id == id }) {
                it
            } else {
                it.copy(messages = it.messages + newMessage(id, sender, body, replyId, isReply, timestampMillis))
            }
        }
    }

    private fun receiveWorldChange(id: Long, worldName: String, timestampMillis: Long) {
        if (timestampMillis <= clearedAt) return
        _uiState.update {
            if (it.messages.any { message -> message.id == id }) {
                it
            } else {
                it.copy(
                    messages = it.messages + ChatMessage(
                        id = id,
                        sender = User(UserId("world"), worldName),
                        body = worldName,
                        timestampMillis = timestampMillis,
                        isWorldChange = true,
                    ),
                )
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
            remoteTypingTimeouts.remove(user.id)?.cancel()
            remoteTypingTimeouts[user.id] = viewModelScope.launch {
                delay(RemoteTypingTimeoutMillis)
                clearRemoteTyping(user.id)
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
        localTypingIdle?.cancel()
        localTypingIdle = viewModelScope.launch {
            delay(LocalTypingIdleMillis)
            stopLocalTyping()
        }
    }

    private fun stopLocalTyping() {
        localTypingIdle?.cancel()
        localTypingIdle = null
        val selfId = selfUser.id
        if (_uiState.value.typingUsers.any { it.id == selfId }) {
            _uiState.update { state -> state.copy(typingUsers = state.typingUsers.filter { it.id != selfId }) }
            _outgoingTyping.tryEmit(false)
        }
    }

    private fun clearRemoteTyping(userId: UserId) {
        remoteTypingTimeouts.remove(userId)?.cancel()
        _uiState.update { state -> state.copy(typingUsers = state.typingUsers.filter { it.id != userId }) }
    }

    private fun newMessage(
        id: Long,
        sender: User,
        body: String,
        replyId: Long?,
        isReply: Boolean,
        timestampMillis: Long = System.currentTimeMillis(),
    ) = ChatMessage(
        id = id,
        sender = sender,
        body = body,
        timestampMillis = timestampMillis,
        replyId = replyId,
        isReply = isReply,
    )
}
