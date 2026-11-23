package com.karasu256.mcchatnative.data

import com.karasu256.mcchatnative.model.ChatEvent
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.ChatUser
import kotlinx.coroutines.flow.Flow

interface ChatEventSource {
    fun events(clientId: String): Flow<ChatEvent>

    suspend fun send(clientId: String, message: ChatMessage)

    fun activeUsers(clientId: String): Flow<List<ChatUser>>
}

object ChatRepository : ChatEventSource by DemoChatEventSource
