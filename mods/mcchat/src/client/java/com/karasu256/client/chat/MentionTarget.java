package com.karasu256.client.chat;

import com.karasu256.mcchat.common.PlayerRef;

public interface MentionTarget {
  void chatnative$insertMention(PlayerRef player);
}
