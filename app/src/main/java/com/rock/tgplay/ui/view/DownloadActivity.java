package com.rock.tgplay.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rock.tgplay.R;
import com.rock.tgplay.adapter.DownloadsAdapter;
import com.rock.tgplay.databinding.ActivityDownloadBinding;
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.ui.viewmodel.DownloadsViewModel;

public class DownloadActivity extends AppCompatActivity {

    DownloadsViewModel vm;
    ActivityDownloadBinding lb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lb = ActivityDownloadBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(lb.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.secondary));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        lb.downloads.setLayoutManager(new LinearLayoutManager(this));
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
        vm = new ViewModelProvider(this).get(DownloadsViewModel.class);
        refreshDownloads();
    }

    private void refreshDownloads() {
        Log.e(getClass().getSimpleName(), "Showing downloads: ");
        vm.getDownloads(true).observe(this,downloadsListResponse -> {
            if (downloadsListResponse == null) return;
            if(!downloadsListResponse.isLoadMore){
                lb.downloads.setAdapter(new DownloadsAdapter(downloadsListResponse.files,downloadsListResponse.hasNext, new DownloadsAdapter.onLoadMoreListener() {
                    @Override
                    public void loadMoreDownloads() {
                        vm.getDownloads(false);
                    }
                }));
            }else{
                if(lb.downloads.getAdapter() instanceof DownloadsAdapter){
                    ((DownloadsAdapter) lb.downloads.getAdapter()).addMore(downloadsListResponse);
                }
            }
        });
    }


}