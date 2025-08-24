package com.rock.tgplay.ui.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.rock.tgplay.R;
import com.rock.tgplay.tdlib.listener.LoginListener;
import com.rock.tgplay.tdlib.manager.CredentialsManager;
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.tdlib.model.TgAppCredentials;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {
    private CredentialsManager credentialsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.secondary));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        Log.d("MAIN", "onCreate: ");
        try {
            createNoMedia();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private void createNoMedia() throws FileNotFoundException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                return;
            }
        }
        initTelegram();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    createNoMedia(); // now run it
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initTelegram() {
        if (credentialsManager == null) credentialsManager = new CredentialsManager(this);
        TgAppCredentials savedCredentials = credentialsManager.getSavedCredentials();
        if (savedCredentials == null || savedCredentials.getAPI_HASH().isEmpty()) {
            Intent intent = new Intent(this, AppApiActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }else
        {
            if (TelegramClient.isClientActive()) {
                if (TelegramClient.isLoggedIn()) {
                    openHomeActivity();
                } else {
                    openLoginActivity();
                }
            } else {
                initTDClient(savedCredentials);
            }
        }
    }

    private void initTDClient(TgAppCredentials savedCredentials) {
        Log.d("MAIN", "initTDClient: ");
        TelegramClient telegramClient = TelegramClient.getInstance();
        LoginListener loginListener = new LoginListener() {
            @Override
            public void onWaitForPhoneNumber() {
                openLoginActivity();
            }

            @Override
            public void onWaitForOTP() {

            }

            @Override
            public void onLoginSuccess() {
                openHomeActivity();
            }
        };
        telegramClient.setLoginListener(loginListener);
        telegramClient.init(savedCredentials, getApplicationContext());
    }

    private void openHomeActivity() {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void openLoginActivity() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}