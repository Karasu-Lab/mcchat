package com.karasu256.client.chat;

import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import java.util.UUID;
import net.minecraft.network.chat.Component;

public record ReceivedChat(
    UUID id,
    PlayerRef sender,
    ChatText text,
    Component component,
    boolean mention,
    long timestampMillis) {}
