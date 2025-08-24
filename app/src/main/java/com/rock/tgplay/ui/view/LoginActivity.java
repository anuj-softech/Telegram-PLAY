package com.rock.tgplay.ui.view;

import static com.rock.tgplay.ui.viewmodel.LoginUiState.PHONE_INPUT;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rock.tgplay.R;
import com.rock.tgplay.databinding.ActivityLoginBinding;
import com.rock.tgplay.ui.viewmodel.LoginViewModel;
import com.rock.tgplay.widget.LoaderDialog;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    LoginViewModel vm;
    ActivityLoginBinding lb;
    LoaderDialog loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lb = ActivityLoginBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(lb.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.secondary));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        vm = new ViewModelProvider(this).get(LoginViewModel.class);
        observeUiState();
        vm.uiState.setValue(PHONE_INPUT);
        vm.initLogin();
    }

    private void observeUiState() {
        vm.uiState.observe(this, state -> {
            switch (state) {
                case PHONE_INPUT:
                    hideProgressBar();
                    showPhoneInput();
                    break;
                case IDLE:
                    showProgressBar();
                    break;
                case LOADING:
                    showProgressBar();
                    break;
                case OTP_SENT:
                    hideProgressBar();
                    showOtpInput();
                    break;
                case VERIFIED:
                    hideProgressBar();
                    goToNextScreen();
                    break;
                case ERROR:
                    hideProgressBar();
                    Toast.makeText(this, "Error: " + vm.getError() , Toast.LENGTH_SHORT).show();
                    break;
                default:
                    hideProgressBar();
            }
        });
    }

    private void goToNextScreen() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showPhoneInput() {
        setEditTextFocus(lb.phoneNumberEditText);
        lb.otpEditText.setVisibility(View.GONE);
        lb.phoneNumberEditText.setVisibility(View.VISIBLE);
        lb.loginButton.setText("Get OTP");
        lb.loginButton.setOnClickListener(v -> {
            String phoneNo = Objects.requireNonNull(lb.phoneNumberEditText.getText()).toString();
            if (vm.verify(phoneNo)) {
                vm.sendOtp(vm.sanitize(phoneNo));
            } else {
                lb.phoneNumberEditText.setError("Invalid Phone Number");
            }
        });
    }

    private void showOtpInput() {
        setEditTextFocus(lb.otpEditText);
        lb.otpEditText.setVisibility(View.VISIBLE);
        lb.phoneNumberEditText.setVisibility(View.GONE);
        lb.status.setText("OTP sent to "+lb.phoneNumberEditText.getText());
        lb.loginButton.setText("Verify OTP");
        lb.loginButton.setOnClickListener(v -> {
            if(lb.otpEditText.getText()!=null && !lb.otpEditText.getText().toString().isEmpty()){
                String otp = Objects.requireNonNull(lb.otpEditText.getText()).toString();
                vm.verifyOtp(otp);
            }else{
                lb.otpEditText.setError("Invalid OTP");
            }
        });
    }

    private void hideProgressBar() {
        if (loader != null) loader.stopLoading();
    }

    private void showProgressBar() {
        if(loader==null) loader = new LoaderDialog(this, R.drawable.loader);
        loader.startLoading();
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