package com.karasu256.mcchatnative.data.event

import com.karasu256.mcchat.common.event.CapturedEvent
import com.karasu256.mcchat.common.event.EventContext

class AppEventContext(val clientId: String) : EventContext {
    override fun accept(event: CapturedEvent) {}
}
