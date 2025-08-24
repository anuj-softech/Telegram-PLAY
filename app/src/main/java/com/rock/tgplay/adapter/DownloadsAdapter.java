package com.rock.tgplay.adapter;

import static com.rock.tgplay.adapter.MessagesAdapter.formatSize;

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
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.ui.viewmodel.DownloadsViewModel;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.DownloadsViewHolder> {

    Client client = TelegramClient.getInstance().getClient();
    private final ArrayList<TdApi.FileDownload> files;
    private boolean hasNext;
    private final onLoadMoreListener onLoadMoreListener;
    CompositeDisposable disposable = new CompositeDisposable();

    public interface onLoadMoreListener {
        void loadMoreDownloads();
    }

    public DownloadsAdapter(ArrayList<TdApi.FileDownload> files, boolean hasNext, onLoadMoreListener onLoadMoreListener) {
        this.files = files;
        this.hasNext = hasNext;
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public void addMore(DownloadsViewModel.DownloadsListResponse downloadsListResponse) {
        int oldSize = files.size();
        files.addAll(downloadsListResponse.files);
        hasNext = downloadsListResponse.hasNext;
        notifyItemRangeInserted(oldSize, downloadsListResponse.files.size());
    }

    @NonNull
    @Override
    public DownloadsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDownloadsBinding binding = ItemDownloadsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DownloadsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadsViewHolder holder, int position) {
        int fileId = files.get(position).fileId;
        long messageId = files.get(position).message.id;
        long chatId = files.get(position).message.chatId;
        Log.e(getClass().getSimpleName(), "onBindViewHolder: " + fileId + " " + messageId + " " + chatId);
        disposable.clear();
        Disposable disposableItem = getFileFromId(fileId, messageId, chatId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(fileMessagePair -> {
                    if (fileMessagePair.second.content instanceof TdApi.MessageDocument || fileMessagePair.second.content instanceof TdApi.MessageVideo) {
                        holder.bind(fileMessagePair.first, fileMessagePair.second.content);
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
        return files.size();
    }


    public class DownloadsViewHolder extends RecyclerView.ViewHolder {
        private final ItemDownloadsBinding lb;
        private final CompositeDisposable downloadObDisposables = new CompositeDisposable();
        private final CompositeDisposable disposables = new CompositeDisposable();
        private final View.OnClickListener playClickListner = v -> {
            Toast.makeText(v.getContext(), "Not Downloaded Yet", Toast.LENGTH_SHORT).show();
        };

        DownloadsViewHolder(ItemDownloadsBinding binding) {
            super(binding.getRoot());
            this.lb = binding;
        }

        void bind(TdApi.File file, TdApi.MessageContent content) {
            if (file.local.isDownloadingCompleted) {
                lb.dnldstatus.setText("Downloaded SuccessFully");
            }
            if (file.local.isDownloadingActive) {
                lb.dnldstatus.setText("Downloading...");
                downloadObDisposables.clear();
                Disposable disposableItem = getDownloadProgress(file.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(dFile -> {
                            if (dFile != null) {
                                lb.dnldprog.setProgress((int) (dFile.local.downloadedSize * 100 / file.size));
                                lb.localpath.setText(dFile.local.path);
                                if (dFile.local.isDownloadingCompleted) {
                                    lb.dnldstatus.setText("Downloaded SuccessFully");
                                    downloadObDisposables.clear();
                                }
                            }
                        });
                downloadObDisposables.add(disposableItem);
            } else {
                lb.dnldstatus.setText("Not Downloaded");
            }
            lb.dnldprog.setProgress((int) (file.local.downloadedSize * 100 / file.size));
            lb.localpath.setText(file.local.path);
            if (content instanceof TdApi.MessageDocument) {
                TdApi.MessageDocument doc = (TdApi.MessageDocument) content;
                bindDoc(doc, playClickListner);
            }
            if (content instanceof TdApi.MessageVideo) {
                TdApi.MessageVideo video = (TdApi.MessageVideo) content;
                bindVideo(video, playClickListner);
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

        private void bindVideo(TdApi.MessageVideo messageVideo, View.OnClickListener playClickListner) {
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
            lb.maindoc.downloadbtn.setOnClickListener(v -> {
                client.send(new TdApi.DownloadFile(messageVideo.video.video.id, 32, 0, 0, false), object -> {
                    if (object instanceof TdApi.File)
                        bind((TdApi.File) object, messageVideo);
                }, e -> {
                });
            });
            lb.maindoc.downloadbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.maindoc.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.maindoc.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });
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
        }

        private void bindDoc(TdApi.MessageDocument messageDoc, View.OnClickListener playClickListner) {
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
            lb.maindoc.downloadbtn.setOnClickListener(v -> {
                client.send(new TdApi.DownloadFile(messageDoc.document.document.id, 32, 0, 0, false), object -> {
                    if (object instanceof TdApi.File)
                        bind((TdApi.File) object, messageDoc);
                }, e -> {
                });
            });
            lb.maindoc.downloadbtn.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                    lb.maindoc.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.light_gray)));
                } else {
                    lb.maindoc.downloadbtn.setImageTintList(ColorStateList.valueOf(v.getContext().getColor(R.color.primary)));
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                }
            });
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
        }

    }
}
