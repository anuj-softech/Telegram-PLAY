package com.rock.tgplay.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.rock.tgplay.R;
import com.rock.tgplay.databinding.ActivityAppapiBinding;
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.tdlib.manager.CredentialsManager;
import com.rock.tgplay.tdlib.model.TgAppCredentials;
import com.rock.tgplay.widget.LoaderDialog;

public class AppApiActivity extends AppCompatActivity {

    ActivityAppapiBinding lb;
    LoaderDialog loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lb = ActivityAppapiBinding.inflate(getLayoutInflater());
        setContentView(lb.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.secondary));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setOnClickListners();
    }

    private void setOnClickListners() {
        setEditTextFocus(lb.apiIdEditText);
        setEditTextFocus(lb.apiHashText);
        lb.saveCred.setOnClickListener(v -> {
            if(lb.apiHashText.getText()==null && lb.apiHashText.getText().toString().isEmpty()){
                lb.apiHashText.setError("Enter API Hash");
                return;

            }
            if(lb.apiIdEditText.getText()==null && lb.apiIdEditText.getText().toString().isEmpty()){
                lb.apiIdEditText.setError("Enter API ID");
                return;
            }
            TgAppCredentials tgAppCredentials = new TgAppCredentials(lb.apiIdEditText.getText().toString(),lb.apiHashText.getText().toString());
            CredentialsManager credentialsManager = new CredentialsManager(this);
            credentialsManager.saveCredentials(tgAppCredentials);
            TelegramClient telegramClient = TelegramClient.getInstance();
            telegramClient.init(tgAppCredentials, getApplicationContext());
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void setEditTextFocus(EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                editText.setBackground(AppCompatResources.getDrawable(this,R.drawable.round_bg_focused));
            }else{
                editText.setBackground(AppCompatResources.getDrawable(this,R.drawable.round_bg));
            }
        });
    }
}