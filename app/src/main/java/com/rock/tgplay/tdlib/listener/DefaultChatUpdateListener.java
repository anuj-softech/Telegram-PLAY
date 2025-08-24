package com.rock.tgplay.tdlib.listener;

import com.rock.tgplay.tdlib.chat.OrderedChat;

import org.drinkless.tdlib.TdApi;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentMap;

public class DefaultChatUpdateListener implements
        ChatUpdateListener {
    @Override
    public void onMainChatListChanged(NavigableSet<OrderedChat> mainChatList, ConcurrentMap<Long, TdApi.Chat> chats) {
    }
}
