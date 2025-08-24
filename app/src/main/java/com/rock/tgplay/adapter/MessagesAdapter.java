package com.rock.tgplay.adapter;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import com.rock.tgplay.CF;
import com.rock.tgplay.R;
import com.rock.tgplay.databinding.ItemDocumentBinding;
import com.rock.tgplay.databinding.ItemMessageBinding;
import com.rock.tgplay.databinding.ItemMetaBinding;
import com.rock.tgplay.databinding.ItemPhotoBinding;
import com.rock.tgplay.helper.ProfileUtils;
import com.rock.tgplay.helper.TimeUtils;
import com.rock.tgplay.player.RockPlayer;
import com.rock.tgplay.tdlib.TelegramClient;


import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ChatViewHolder> {
    private final Client client;
    private final SharedPreferences sharedpref;
    private final RecyclerView gridView;
    private ArrayList<TdApi.Message> adapterMessages = new ArrayList<TdApi.Message>();
    private TdApi.Chat chat;
    Activity activity;
    private boolean scrolled = false;

    public MessagesAdapter(TdApi.Chat chat, Activity homeActivity, RecyclerView gridView) {
        sharedpref = homeActivity.getSharedPreferences(CF.SHARED_PREFERENCES_TELEGRAM, MODE_PRIVATE);
        this.chat = chat;
        activity = homeActivity;
        this.gridView = gridView;
        client = TelegramClient.getInstance().getClient();
        long lastMessageShownId = sharedpref.getLong(chat.id + CF.LAST_MESSAGE_SHOWN_KEY, chat.lastMessage.id);
        if (adapterMessages.isEmpty()) {
            loadOlderMessages(chat.id, lastMessageShownId, true);
        }
    }

    private void loadOlderMessages(long chatid, long lastmessageid, boolean includethat) {
        Log.e(TAG, "loadOlderMessages: " + lastmessageid);
        TdApi.Function getChatHistory;
        if (includethat) {
            getChatHistory = new TdApi.GetChatHistory(chatid, lastmessageid, -1, 50, true);
            if (lastmessageid != chat.lastMessage.id) {
                getChatHistory = new TdApi.GetChatHistory(chatid, lastmessageid, -20, 50, true);
            }
        } else {
            getChatHistory = new TdApi.GetChatHistory(chatid, lastmessageid, 0, 50, false);
        }


        client.send(getChatHistory, chathistoryobj -> {
            if (chathistoryobj instanceof TdApi.Messages) {
                activity.runOnUiThread(() -> {
                    TdApi.Messages messages = (TdApi.Messages) chathistoryobj;
                    Log.e(TAG, "showMessagesOf: " + messages.messages.length);
                    Log.e(TAG, "showMessagesOf: " + messages.totalCount);
                    int lastpos = adapterMessages.size();
                    adapterMessages.addAll(Arrays.asList(messages.messages));
                    notifyItemRangeInserted(lastpos, messages.messages.length);
                    scrollGridViewToLastMessage(lastmessageid);
                });
            } else {
                Log.e(TAG, "showMessagesOf: " + chathistoryobj);
            }
        });
    }

    private void scrollGridViewToLastMessage(long targetMessageId) {
        if (!scrolled) {
            int targetPosition = -1;
            for (int i = 0; i < adapterMessages.size(); i++) {
                if (adapterMessages.get(i).id == targetMessageId) {
                    targetPosition = i;
                    break;
                }
            }
            if (targetPosition != -1) {
                gridView.scrollToPosition(targetPosition);
                Log.e(TAG, "scrollGridViewToLastMessage: " + targetPosition);
            }
            scrolled = true;
        }
    }

    private void loadNewerMessages(long chatId, long lastmessageid) {
        Log.e(TAG, "loadingNewMessages: " + lastmessageid);
        TdApi.Function getChatHistory;
        getChatHistory = new TdApi.GetChatHistory(chatId, lastmessageid, -10, 11, false);

        client.send(getChatHistory, chathistoryobj -> {
            if (chathistoryobj instanceof TdApi.Messages) {
                activity.runOnUiThread(() -> {
                    TdApi.Messages messages = (TdApi.Messages) chathistoryobj;
                    Log.e(TAG, "gotNewMessages: " + messages.messages.length);
                    List<TdApi.Message> newMessages = new ArrayList<>(Arrays.asList(messages.messages));
                    newMessages.remove(newMessages.size() - 1);
                    newMessages.remove(newMessages.size() - 1);
                    adapterMessages.addAll(0, newMessages);
                    notifyItemRangeInserted(0, messages.messages.length - 2);
                });
            } else {
                Log.e(TAG, "showMessagesOf: " + chathistoryobj);
            }
        });
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemMessageBinding binding = ItemMessageBinding.inflate(layoutInflater, parent, false);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        TdApi.Message message = adapterMessages.get(position);
        holder.bind(message);
        if (position == adapterMessages.size() - 1) {
            loadOlderMessages(message.chatId, message.id, false);
        }
        if (position == 0 && chat.lastMessage != null && scrolled) {
            if (message.id != chat.lastMessage.id) {
                loadNewerMessages(message.chatId, message.id);
            } else {
                Log.e(TAG, "onBindViewHolder: Latest Message Reached" + message.id);
            }
        }
    }


    @Override
    public int getItemCount() {
        return adapterMessages.size();
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        private ItemMessageBinding binding;
        private final CompositeDisposable disposables = new CompositeDisposable();

        public ChatViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(TdApi.Message message) {
            disposables.clear();
            ItemMetaBinding metaBinding = ItemMetaBinding.inflate(LayoutInflater.from(activity), null, false);
            metaBinding.datetxt.setText(TimeUtils.formatMessageTime(message.date));
            binding.mainframe.removeAllViews();
            binding.mainframe.addView(metaBinding.getRoot());
            binding.mainframe.setOnClickListener(v -> {
                ImageButton playbtn = v.findViewById(R.id.playbtn);
                if (playbtn != null) {
                    playbtn.requestFocus();
                }
            });
            ColorStateList colorDefault = binding.mainframe.getBackgroundTintList();
            binding.mainframe.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#335757")));
                } else {
                    v.setBackgroundTintList(colorDefault);
                }
            });

            if (message.content instanceof TdApi.MessageText) {
                TdApi.MessageText messageText = (TdApi.MessageText) message.content;
                String text = messageText.text.text;
                binding.mainframe.addView(getTextView(text), 0);
            }
            if (message.content instanceof TdApi.MessagePhoto) {
                TdApi.MessagePhoto messagePhoto = (TdApi.MessagePhoto) message.content;
                String text = messagePhoto.caption.text;
                binding.mainframe.addView(getTextView(text), 0);
                binding.mainframe.addView(getPhotoView(messagePhoto), 0);
            }
            if (message.content instanceof TdApi.MessageAudio) {
                TdApi.MessageAudio messageAudio = (TdApi.MessageAudio) message.content;
                String text = messageAudio.caption.text;
                binding.mainframe.addView(getTextView(text), 0);
                binding.mainframe.addView(getAudioView(messageAudio, message), 0);
            }
            if (message.content instanceof TdApi.MessageDocument) {
                TdApi.MessageDocument messageDocument = (TdApi.MessageDocument) message.content;
                String text = messageDocument.caption.text;
                binding.mainframe.addView(getTextView(text), 0);
                binding.mainframe.addView(getDocView(messageDocument, message), 0);
            }
            if (message.content instanceof TdApi.MessageVideo) {
                TdApi.MessageVideo messageVideo = (TdApi.MessageVideo) message.content;
                String text = messageVideo.caption.text;
                binding.mainframe.addView(getTextView(text), 0);

                binding.mainframe.addView(getVideoView(messageVideo, message), 0);

            }
            if (message.content instanceof TdApi.MessageAnimation) {
                TdApi.MessageAnimation messageAnimation = (TdApi.MessageAnimation) message.content;
                String text = messageAnimation.caption.text;
                binding.mainframe.addView(getTextView(text), 0);
            }
            if (message.content instanceof TdApi.MessageAnimatedEmoji) {

            }
            if (message.content instanceof TdApi.MessageContact) {
                TdApi.MessageContact messageContact = (TdApi.MessageContact) message.content;
                String text = messageContact.contact.firstName + " " + messageContact.contact.lastName;
                binding.mainframe.addView(getTextView(text));
            }
            if (message.content instanceof TdApi.MessageLocation) {
            }
            if (message.content instanceof TdApi.MessageVenue) {
            }
            if (message.content instanceof TdApi.MessageBasicGroupChatCreate) {
                TdApi.MessageBasicGroupChatCreate messageBasicGroupChatCreate = (TdApi.MessageBasicGroupChatCreate) message.content;
                String text = messageBasicGroupChatCreate.title;
                binding.mainframe.addView(getTextView(text));
            }
            if (message.content instanceof TdApi.MessageChatAddMembers) {
                TdApi.MessageChatAddMembers messageChatAddMembers = (TdApi.MessageChatAddMembers) message.content;
                String text = Arrays.toString(messageChatAddMembers.memberUserIds);
                binding.mainframe.addView(getTextView(text + " joined"));
            }
            if (message.content instanceof TdApi.MessageChatJoinByLink) {
                TdApi.MessageChatJoinByLink messageChatJoinByLink = (TdApi.MessageChatJoinByLink) message.content;
                String text = messageChatJoinByLink.toString();
                binding.mainframe.addView(getTextView(text));
            }
            if (message.content instanceof TdApi.MessageChatDeleteMember) {
                TdApi.MessageChatDeleteMember messageChatDeleteMember = (TdApi.MessageChatDeleteMember) message.content;
                String text = messageChatDeleteMember.toString();
                binding.mainframe.addView(getTextView(text));
            }
        }

        private View getVideoView(TdApi.MessageVideo messageVideo, TdApi.Message message) {
            ItemDocumentBinding lb = ItemDocumentBinding.inflate(LayoutInflater.from(activity), null, false);
            if (messageVideo.video.minithumbnail != null) {
                byte[] minithumbnailData = messageVideo.video.minithumbnail.data;
                Bitmap minithumbnailBitmap = BitmapFactory.decodeByteArray(minithumbnailData, 0, minithumbnailData.length);
                lb.thumbnail.setImageBitmap(minithumbnailBitmap);
            }

            if (messageVideo.video.thumbnail != null)
                disposables.add(ProfileUtils.download(messageVideo.video.thumbnail.file)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> lb.thumbnail.setImageURI(Uri.fromFile(file))));

            lb.title.setText(messageVideo.video.fileName);
            lb.sizetype.setText(String.format("%s • %s", formatSize(messageVideo.video.video.size), messageVideo.video.mimeType));
            lb.downloadbtn.setOnClickListener(v -> {
                saveLastMessageID(message);
                client.send(new TdApi.AddFileToDownloads(messageVideo.video.video.id, chat.id, message.id, 32), object -> {
                    if (object instanceof TdApi.File) {
                        TdApi.File file = (TdApi.File) object;
                        Toast.makeText(activity, "Added to Downloads", Toast.LENGTH_SHORT).show();
                    }
                }, e -> {
                });
            });
            lb.downloadbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });
            lb.playbtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(activity.getApplicationContext(), RockPlayer.class);
                intent.putExtra("fileId", messageVideo.video.video.id);
                intent.putExtra("messageId", message.id);
                intent.putExtra("chatId", chat.id);
                intent.putExtra("name", messageVideo.video.fileName);
                activity.startActivity(intent);
                saveLastMessageID(message);
            });
            lb.playbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.playbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.playbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });
            return lb.getRoot();
        }

        private View getPhotoView(TdApi.MessagePhoto messagePhoto) {
            ItemPhotoBinding lb = ItemPhotoBinding.inflate(LayoutInflater.from(activity), null, false);
            if (messagePhoto.photo != null)
                disposables.add(ProfileUtils.download(messagePhoto.photo.sizes[0].photo)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> lb.imgview.setImageURI(Uri.fromFile(file))));
            return lb.getRoot();
        }

        private View getAudioView(TdApi.MessageAudio messageAudio, TdApi.Message message) {
            ItemDocumentBinding lb = ItemDocumentBinding.inflate(LayoutInflater.from(activity), null, false);
            TdApi.Audio audio = messageAudio.audio;
            lb.title.setText(audio.fileName);
            lb.sizetype.setText(String.format("%s • %s • %s", formatSize(audio.audio.size), audio.mimeType, audio.duration + " sec"));
            if (audio.albumCoverMinithumbnail != null) {
                byte[] minithumbnailData = audio.albumCoverMinithumbnail.data;
                Bitmap minithumbnailBitmap = BitmapFactory.decodeByteArray(minithumbnailData, 0, minithumbnailData.length);
                lb.thumbnail.setImageBitmap(minithumbnailBitmap);
            }
            if (audio.albumCoverThumbnail != null) {
                disposables.add(ProfileUtils.download(audio.albumCoverThumbnail.file)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> lb.thumbnail.setImageURI(Uri.fromFile(file))));
            }
            return lb.getRoot();
        }

        private View getDocView(TdApi.MessageDocument messageDocument, TdApi.Message message) {
            ItemDocumentBinding lb = ItemDocumentBinding.inflate(LayoutInflater.from(activity), null, false);
            lb.title.setText(messageDocument.document.fileName);
            lb.sizetype.setText(String.format("%s • %s", formatSize(messageDocument.document.document.size), messageDocument.document.mimeType));
            if (messageDocument.document.minithumbnail != null) {
                byte[] minithumbnailData = messageDocument.document.minithumbnail.data;
                Bitmap minithumbnailBitmap = BitmapFactory.decodeByteArray(minithumbnailData, 0, minithumbnailData.length);
                lb.thumbnail.setImageBitmap(minithumbnailBitmap);
            }

            lb.downloadbtn.setOnClickListener(v -> {
                saveLastMessageID(message);
                Log.e(TAG, "getDocView D: " + messageDocument.document.document.id);
                client.send(new TdApi.AddFileToDownloads(messageDocument.document.document.id, chat.id, message.id, 32), object -> {
                    if (object instanceof TdApi.File) {
                        TdApi.File file = (TdApi.File) object;
                        activity.runOnUiThread(()->{
                            Toast.makeText(activity, "Added to Downloads", Toast.LENGTH_SHORT).show();
                        });
                    }
                }, e -> {
                    e.printStackTrace();
                });

            });
            lb.downloadbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });
            lb.playbtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(activity.getApplicationContext(), RockPlayer.class);
                intent.putExtra("fileId", messageDocument.document.document.id);
                intent.putExtra("messageId", message.id);
                intent.putExtra("chatId", chat.id);
                intent.putExtra("name", messageDocument.document.fileName);
                activity.startActivity(intent);
                saveLastMessageID(message);
            });
            lb.playbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.playbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.playbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });

            if (messageDocument.document.thumbnail != null) {
                disposables.add(ProfileUtils.download(messageDocument.document.thumbnail.file)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> lb.thumbnail.setImageURI(Uri.fromFile(file))));
            }
            return lb.getRoot();
        }
    }


    private void saveLastMessageID(TdApi.Message message) {
        sharedpref.edit().putLong(message.chatId + CF.LAST_MESSAGE_SHOWN_KEY, message.id).apply();
    }

    private String getFormatedDate(int date) {
        String dateString = "";
        Date dateObj = new Date(date);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("DD MMM yy HH:mm a");
        dateString = simpleDateFormat.format(dateObj);
        return dateString;
    }

    private View getTextView(String text) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(40, 10, 20, 10);
        tv.setTextSize(15);
        return tv;
    }

    public static String formatSize(long bytes) {
        final String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

}