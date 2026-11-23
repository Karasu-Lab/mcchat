package com.karasu256.mcchatnative.data.event

import com.karasu256.mcchat.common.WorldData
import com.karasu256.mcchatnative.model.ChatEvent

interface ChatEventProducer {
    fun toChatEvent(world: WorldData): ChatEvent?
}
