package com.karasu256.mcchatnative.data

import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.User
import kotlinx.coroutines.flow.Flow

interface ChatEventSource {
    fun events(clientId: String): Flow<ChatEvent>

    suspend fun send(clientId: String, message: ChatMessage)

    suspend fun sendTyping(clientId: String, isTyping: Boolean) {}

    fun activeUsers(clientId: String): Flow<List<User>>
}

object ChatRepository : ChatEventSource by ConnectedChatEventSource
