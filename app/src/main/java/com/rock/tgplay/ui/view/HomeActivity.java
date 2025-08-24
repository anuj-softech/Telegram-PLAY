package com.rock.tgplay.ui.view;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rock.tgplay.R;
import com.rock.tgplay.adapter.ChatsAdapter;
import com.rock.tgplay.adapter.MessagesAdapter;
import com.rock.tgplay.databinding.ActivityHomeBinding;
import com.rock.tgplay.helper.ProfileUtils;
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.tdlib.chat.OrderedChat;
import com.rock.tgplay.ui.viewmodel.HomeViewModel;
import com.rock.tgplay.widget.LoaderDialog;

import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity {

    HomeViewModel vm;
    ActivityHomeBinding lb;
    LoaderDialog loader;
    ChatsAdapter chatsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lb = ActivityHomeBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(lb.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.tertiary));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        lb.chatsGridView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        lb.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TelegramClient.isClientActive()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        vm = new ViewModelProvider(this).get(HomeViewModel.class);
        observeUiState();
        setTabFocusAndLogic();
        if(getVisibleFragment() == 0){
            showChatList();
        }
        deleteTemp();
        ListenForChatMessages();
    }

    CompositeDisposable disposables = new CompositeDisposable();

    private void ListenForChatMessages() {

        vm.getChatMessagesData().observe(this, chat -> {
            if (chat != null) {
                disposables.clear();
                showFragment(1);
                lb.chatName.setText(String.format("%s", chat.title));
                ProfileUtils.loadProfile(lb.chatProfileImg, chat.title);
                if (chat.photo != null) {
                    disposables.add(ProfileUtils.download(chat.photo.small)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(file -> lb.chatProfileImg.setImageURI(Uri.fromFile(file))));
                }
                lb.messagesRecyclerView.setAdapter(new MessagesAdapter(chat, this, lb.messagesRecyclerView));
            }
        });
    }

    private void showFragment(int index) {
        int childCount = lb.homeFragment.getChildCount();
        for (int i = 0; i < childCount; i++) {
            lb.homeFragment.getChildAt(i).setVisibility(GONE);
        }
        lb.homeFragment.getChildAt(index).setVisibility(VISIBLE);
    }
    private int getVisibleFragment() {
        int childCount = lb.homeFragment.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if(lb.homeFragment.getChildAt(i).getVisibility() == VISIBLE){
                return i;
            }
        }
        return 0;
    }

    private void showChatList() {

        vm.showChatList();
        vm.getChatListLiveData().observe(this, pair -> {
            NavigableSet<OrderedChat> mainChatList = pair.first;
            ConcurrentMap<Long, TdApi.Chat> chats = pair.second;
            showFragment(0);
            Log.d("HOME", "showChatList: " + chats.size() + " mainchatlistsize " + mainChatList.size());
            if (chatsAdapter == null) {
                chatsAdapter = new ChatsAdapter(chats, mainChatList, chat -> {
                    Log.d("HOME", "Clicked: " + chat);
                    vm.openMessages(chat.id);
                });
                lb.chatsGridView.setAdapter(chatsAdapter);
                lb.chatsGridView.postDelayed(() -> TelegramClient.getInstance().getMainChatList(10), 2000);
            } else {
                lb.chatsGridView.post(() -> chatsAdapter.submitList(chats, mainChatList));
            }
        });
    }

    private void setTabFocusAndLogic() {
        setTabFocus(lb.sidebar.chatTab);
        setTabFocus(lb.sidebar.dlndTab);
        setTabFocus(lb.sidebar.historyTab);
        setTabFocus(lb.sidebar.deleteTab);
        lb.sidebar.chatTab.setOnClickListener(v -> {

        });
        lb.sidebar.dlndTab.setOnClickListener(v -> {
            Intent intent = new Intent(this, DownloadActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        lb.sidebar.historyTab.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        lb.sidebar.deleteTab.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Cached and Downloads")
                    .setMessage("Are you sure you want to delete all files?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        reallyDeleteAll();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        });
    }

    private void reallyDeleteAll() {
        String databasepath = null;
        databasepath = Objects.requireNonNull(getExternalFilesDir("rock-tg")).getAbsolutePath();
        if(databasepath.contains("rock-tg")){
            deleteFolder(new File(databasepath + "/temp"));
            deleteFolder(new File(databasepath + "/documents"));
            deleteFolder(new File(databasepath + "/thumbnails"));
            deleteFolder(new File(databasepath + "/photos"));
            deleteFolder(new File(databasepath + "/videos"));
        }
    }
    private void deleteTemp() {
        String databasepath = null;
        databasepath = Objects.requireNonNull(getExternalFilesDir("rock-tg")).getAbsolutePath();
        if(databasepath.contains("rock-tg")){
            deleteFolder(new File(databasepath + "/temp"));
        }
    }
    public static void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }
    private void setTabFocus(LinearLayout tabView) {
        ImageView imageView = (ImageView) tabView.getChildAt(0);
        TextView textView = (TextView) tabView.getChildAt(1);
        tabView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tabView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ffffff")));
                imageView.setImageTintList(ColorStateList.valueOf(getColor(R.color.light_gray)));
                textView.setTextColor(ColorStateList.valueOf(getColor(R.color.light_gray)));

            } else {
                imageView.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary)));
                textView.setTextColor(ColorStateList.valueOf(getColor(R.color.primary)));
                tabView.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            }
        });
    }

    private void observeUiState() {
        vm.uiState.observe(this, state -> {
            switch (state) {
                case IDLE:
                    break;
                case ERROR:
                    Toast.makeText(this, "Error: ", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    hideProgressBar();
            }
        });
    }

    private void goToNextScreen() {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            if(getVisibleFragment() == 1){
                showFragment(0);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void hideProgressBar() {
        if (loader != null) loader.stopLoading();
    }

    private void showProgressBar() {
        if (loader == null) loader = new LoaderDialog(this, R.drawable.loader);
        loader.startLoading();
    }
}