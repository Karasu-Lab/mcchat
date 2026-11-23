package com.karasu256.client.chat;

import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import java.util.UUID;

public record ReceivedChat(
    UUID id, PlayerRef sender, ChatText text, boolean mention, long timestampMillis) {}
