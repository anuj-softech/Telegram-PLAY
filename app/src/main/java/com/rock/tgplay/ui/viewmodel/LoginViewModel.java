package com.rock.tgplay.ui.viewmodel;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import com.rock.tgplay.tdlib.TelegramClient;
import com.rock.tgplay.tdlib.listener.LoginListener;

public class LoginViewModel extends ViewModel implements LoginListener {

    private static final String TAG = "LoginViewModel";
    public MutableLiveData<LoginUiState> uiState = new MutableLiveData<>(LoginUiState.IDLE);

    String errorMsg = "";
    private Client client;

    public void sendOtp(String phone) {
        uiState.postValue(LoginUiState.LOADING);
        client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), object -> {
            if (object instanceof TdApi.Ok) {
                Log.d(TAG, "sendOtp: to " + phone);
            } else if (object instanceof TdApi.Error) {
                errorMsg = ((TdApi.Error) object).message;
                uiState.postValue(LoginUiState.ERROR);
            }
        });
    }

    public void verifyOtp(String otp) {
        uiState.postValue(LoginUiState.LOADING);
        client.send(new TdApi.CheckAuthenticationCode(otp), object -> {
            if (object instanceof TdApi.Ok) {
                Log.d(TAG, "verifyOtp: " + otp);
            } else if (object instanceof TdApi.Error) {
                errorMsg = ((TdApi.Error) object).message;
                uiState.postValue(LoginUiState.ERROR);
            }
        });
    }

    public String getError() {
        return errorMsg;
    }

    public void initLogin() {
        TelegramClient telegramClient = TelegramClient.getInstance();
        telegramClient.setLoginListener(this);
        client = telegramClient.getClient();
    }

    @Override
    public void onWaitForPhoneNumber() {
        uiState.postValue(LoginUiState.PHONE_INPUT);
    }

    @Override
    public void onWaitForOTP() {
        uiState.postValue(LoginUiState.OTP_SENT);
    }

    @Override
    public void onLoginSuccess() {
        uiState.postValue(LoginUiState.VERIFIED);
    }

    public boolean verify(String phoneNo) {
        return phoneNo.length() >= 10;
    }

    public String sanitize(String phoneNo) {
        if (phoneNo.length() == 10) {
            phoneNo = "+91" + phoneNo;
        }
        if (!phoneNo.startsWith("+")) {
            phoneNo = "+" + phoneNo;
        }
        return phoneNo;
    }
}
