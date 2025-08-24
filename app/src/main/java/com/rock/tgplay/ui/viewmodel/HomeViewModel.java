package com.rock.tgplay.ui.viewmodel;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.tdlib.chat.OrderedChat;

import org.drinkless.tdlib.TdApi;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentMap;

public class HomeViewModel extends ViewModel {
    public MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.IDLE);
    public MutableLiveData<TdApi.Chat> chatMessagesData = new MutableLiveData<>();
    public MutableLiveData<Pair<NavigableSet<OrderedChat>, ConcurrentMap<Long, TdApi.Chat>>> chatsData = new MutableLiveData<>();
    private String TAG = "HomeViewModel";

    public void showChatList() {
        uiState.postValue(HomeUiState.LOADING);
        Log.e(TAG, "showChatList: loading");
        TelegramClient telegramClient = TelegramClient.getInstance();
        telegramClient.setOnChatUpdateListener((mainChatList, chats) -> {
            chatsData.postValue(new Pair<>(mainChatList, chats));
        });
        telegramClient.getMainChatList(10);
    }

    public void openMessages(long id) {
        TdApi.GetChat getChat = new TdApi.GetChat(id);
        TelegramClient.getInstance().getClient().send(getChat, object -> {
            if (object instanceof TdApi.Chat) {
                TdApi.Chat chat = (TdApi.Chat) object;
                chatMessagesData.postValue(chat);
            }
        });
    }

    public MutableLiveData<TdApi.Chat> getChatMessagesData() {
        return chatMessagesData;
    }

    public MutableLiveData<Pair<NavigableSet<OrderedChat>, ConcurrentMap<Long, TdApi.Chat>>> getChatListLiveData() {
        return chatsData;
    }
}
