package com.rock.tgplay.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
import com.rock.tgplay.adapter.HistoryAdapter;
import com.rock.tgplay.databinding.ActivityDownloadBinding;
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.ui.viewmodel.DownloadsViewModel;
import com.rock.tgplay.ui.viewmodel.HistoryViewModel;

public class HistoryActivity extends AppCompatActivity {

    HistoryViewModel vm;
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
        vm = new ViewModelProvider(this).get(HistoryViewModel.class);
        refreshHistory();
    }

    private void refreshHistory() {
        vm.getHistory(this).observe(this,historyList -> {
            if (historyList.isEmpty()){
                Toast.makeText(this, "No History Found", Toast.LENGTH_SHORT).show();
            }
            lb.downloads.setAdapter(new HistoryAdapter(historyList));
        });
    }


}