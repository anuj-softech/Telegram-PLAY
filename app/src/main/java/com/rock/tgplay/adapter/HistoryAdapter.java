package com.rock.tgplay.adapter;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.rock.tgplay.adapter.MessagesAdapter.formatSize;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rock.tgplay.R;
import com.rock.tgplay.databinding.ItemDownloadsBinding;
import com.rock.tgplay.helper.ProfileUtils;
import com.rock.tgplay.player.HistoryHelper;
import com.rock.tgplay.player.RockPlayer;
import com.rock.tgplay.tdlib.TelegramClient;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    Client client = TelegramClient.getInstance().getClient();
    private final ArrayList<HistoryHelper.PlayerHistoryItem> hItems;
    private boolean hasNext;
    CompositeDisposable disposable = new CompositeDisposable();

    public HistoryAdapter(ArrayList<HistoryHelper.PlayerHistoryItem> hItems) {
        this.hItems = hItems;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDownloadsBinding binding = ItemDownloadsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        int fileId = hItems.get(position).fileId;
        long messageId = hItems.get(position).messageId;
        long chatId = hItems.get(position).chatId;
        Log.e(getClass().getSimpleName(), "onBindViewHolder: " + fileId + " " + messageId + " " + chatId);
        disposable.clear();
        Disposable disposableItem = getFileFromId(fileId, messageId, chatId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(fileMessagePair -> {
                    if (fileMessagePair.second.content instanceof TdApi.MessageDocument || fileMessagePair.second.content instanceof TdApi.MessageVideo) {
                        holder.bind(fileMessagePair.first, fileMessagePair.second.content,hItems.get(position));
                    }
                });
        disposable.add(disposableItem);


    }

    private Observable<Pair<TdApi.File, TdApi.Message>> getFileFromId(int fileId, long messageId, long chatId) {
        return Observable.create(emitter -> {
            client.send(new TdApi.GetFile(fileId), object1 -> {
                if (object1 instanceof TdApi.File) {
                    TdApi.File file = (TdApi.File) object1;
                    {

                        client.send(new TdApi.GetMessage(chatId, messageId), object2 -> {
                            if (object2 instanceof TdApi.Message) {
                                TdApi.Message messege = (TdApi.Message) object2;
                                emitter.onNext(new Pair<>(file, messege));
                                emitter.onComplete();
                            }
                        });
                    }
                }
            });
        });
    }

    private Observable<TdApi.File> getDownloadProgress(int fileId) {
        return Observable.create(emitter -> {
            Handler handler = new Handler(Looper.getMainLooper());
            AtomicBoolean isDownloadingActive = new AtomicBoolean(true);

            Runnable[] runnableHolder = new Runnable[1];

            runnableHolder[0] = () -> client.send(new TdApi.GetFile(fileId), object1 -> {
                if (object1 instanceof TdApi.File) {
                    TdApi.File file = (TdApi.File) object1;
                    emitter.onNext(file);
                    isDownloadingActive.set(file.local.isDownloadingActive);

                    if (!isDownloadingActive.get()) {
                        emitter.onComplete();
                    } else {
                        handler.postDelayed(runnableHolder[0], 1000);
                    }
                }
            });

            handler.post(runnableHolder[0]);
        });
    }

    @Override
    public int getItemCount() {
        return hItems.size();
    }


    public class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemDownloadsBinding lb;
        private final CompositeDisposable downloadObDisposables = new CompositeDisposable();
        private final CompositeDisposable disposables = new CompositeDisposable();
        private final View.OnClickListener playClickListner = v -> {
            Toast.makeText(v.getContext(), "Not Downloaded Yet", Toast.LENGTH_SHORT).show();
        };

        HistoryViewHolder(ItemDownloadsBinding binding) {
            super(binding.getRoot());
            this.lb = binding;
        }

        @SuppressLint("SetTextI18n")
        void bind(TdApi.File file, TdApi.MessageContent content, HistoryHelper.PlayerHistoryItem playerHistoryItem) {
            lb.dnldprog.setProgress((int) (playerHistoryItem.played * 100));
            lb.localpath.setText("Last Played on " + SimpleDateFormat.getDateInstance().format(new Date(playerHistoryItem.timestamp)));
            if (content instanceof TdApi.MessageDocument) {
                TdApi.MessageDocument doc = (TdApi.MessageDocument) content;
                bindDoc(doc, playClickListner,playerHistoryItem);
            }
            if (content instanceof TdApi.MessageVideo) {
                TdApi.MessageVideo video = (TdApi.MessageVideo) content;
                bindVideo(video, playClickListner,playerHistoryItem);
            }

            lb.getRoot().setOnClickListener(v -> {
                ImageButton playbtn = v.findViewById(R.id.playbtn);
                if (playbtn != null) {
                    playbtn.requestFocus();
                }
            });
            ColorStateList colorDefault = lb.getRoot().getBackgroundTintList();
            lb.getRoot().setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#335757")));
                } else {
                    v.setBackgroundTintList(colorDefault);
                }
            });
        }

        private void bindVideo(TdApi.MessageVideo messageVideo, View.OnClickListener playClickListner, HistoryHelper.PlayerHistoryItem playerHistoryItem) {
            if (messageVideo.video.minithumbnail != null) {
                byte[] minithumbnailData = messageVideo.video.minithumbnail.data;
                Bitmap minithumbnailBitmap = BitmapFactory.decodeByteArray(minithumbnailData, 0, minithumbnailData.length);
                lb.maindoc.thumbnail.setImageBitmap(minithumbnailBitmap);
            }

            if (messageVideo.video.thumbnail != null)
                disposables.add(ProfileUtils.download(messageVideo.video.thumbnail.file)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> lb.maindoc.thumbnail.setImageURI(Uri.fromFile(file))));

            lb.maindoc.title.setText(messageVideo.video.fileName);
            lb.maindoc.sizetype.setText(String.format("%s • %s", formatSize(messageVideo.video.video.size), messageVideo.video.mimeType));
            lb.maindoc.playbtn.setOnClickListener(playClickListner);
            lb.maindoc.playbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.maindoc.playbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.maindoc.playbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });
            lb.maindoc.playbtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(lb.maindoc.getRoot().getContext().getApplicationContext(), RockPlayer.class);
                intent.putExtra("fileId", messageVideo.video.video.id);
                intent.putExtra("messageId", playerHistoryItem.messageId);
                intent.putExtra("chatId", playerHistoryItem.chatId);
                intent.putExtra("name", messageVideo.video.fileName);
                lb.maindoc.getRoot().getContext().startActivity(intent);
            });
        }

        private void bindDoc(TdApi.MessageDocument messageDoc, View.OnClickListener playClickListner, HistoryHelper.PlayerHistoryItem playerHistoryItem) {
            if (messageDoc.document.minithumbnail != null) {
                byte[] minithumbnailData = messageDoc.document.minithumbnail.data;
                Bitmap minithumbnailBitmap = BitmapFactory.decodeByteArray(minithumbnailData, 0, minithumbnailData.length);
                lb.maindoc.thumbnail.setImageBitmap(minithumbnailBitmap);
            }

            if (messageDoc.document.thumbnail != null)
                disposables.add(ProfileUtils.download(messageDoc.document.thumbnail.file)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(file -> lb.maindoc.thumbnail.setImageURI(Uri.fromFile(file))));

            lb.maindoc.title.setText(messageDoc.document.fileName);
            lb.maindoc.sizetype.setText(String.format("%s • %s", formatSize(messageDoc.document.document.size), messageDoc.document.mimeType));
            lb.maindoc.playbtn.setOnClickListener(playClickListner);
            lb.maindoc.playbtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(lb.maindoc.getRoot().getContext().getApplicationContext(), RockPlayer.class);
                intent.putExtra("fileId", messageDoc.document.document.id);
                intent.putExtra("messageId", playerHistoryItem.messageId);
                intent.putExtra("chatId", playerHistoryItem.chatId);
                intent.putExtra("name", messageDoc.document.fileName);
                lb.maindoc.getRoot().getContext().startActivity(intent);
            });

        }

    }
}
