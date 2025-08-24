package com.rock.tgplay.adapter;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rock.tgplay.databinding.ItemChatBinding;
import com.rock.tgplay.helper.ProfileUtils;
import com.rock.tgplay.helper.TelegramUtils;
import com.rock.tgplay.helper.TimeUtils;
import com.rock.tgplay.tdlib.chat.OrderedChat;

import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private final List<OrderedChat> orderedChats = new ArrayList<>();
    private Map<Long, TdApi.Chat> chats = new HashMap<>();
    private final OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(TdApi.Chat chat);
    }

    public ChatsAdapter(Map<Long, TdApi.Chat> chats, NavigableSet<OrderedChat> mainChatList, OnChatClickListener listener) {
        this.chats.putAll(chats);
        this.onChatClickListener = listener;
        orderedChats.addAll(mainChatList);
    }

    public void submitList(ConcurrentMap<Long, TdApi.Chat> newChats, NavigableSet<OrderedChat> newOrderedChats) {
        orderedChats.clear();
        orderedChats.addAll(newOrderedChats);
        this.chats.clear();
        this.chats.putAll(newChats);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        OrderedChat orderedChat = orderedChats.get(position);
        TdApi.Chat chat = chats.get(orderedChat.chatId);
        if (chat != null) {
            try {
                holder.bind(chat);
            } catch (Exception e) {
            }
        }

    }

    @Override
    public int getItemCount() {
        return orderedChats.size();
    }


    public class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatBinding b;
        CompositeDisposable profileDownloader = new CompositeDisposable();

        ChatViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(TdApi.Chat chat) {
            profileDownloader.clear();
            b.name.setText(chat.title);
            b.subtitle.setText("");
            if (chat.lastMessage != null) {
                b.subtitle.setText(TelegramUtils.getTagFromMsg(chat.lastMessage));
            }

            b.time.setText(TimeUtils.formatMessageTime(chat.lastMessage.date));

            itemView.setOnClickListener(v -> onChatClickListener.onChatClick(chat));

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    itemView.setBackgroundColor(Color.parseColor("#191A1A"));
                } else {
                    itemView.setBackgroundColor(Color.TRANSPARENT);
                }
            });

            ProfileUtils.loadProfile(b.profileImg, chat.title);

            if (chat.photo != null) {
                Disposable disposable = ProfileUtils.download(chat.photo.small)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> b.profileImg.setImageURI(Uri.fromFile(file)));
                profileDownloader.add(disposable);
            }
        }

    }
}
