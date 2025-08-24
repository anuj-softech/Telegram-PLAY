package com.rock.tgplay.tdlib.listener;

public interface LoginListener {
    void onWaitForPhoneNumber();

    void onWaitForOTP();

    void onLoginSuccess();
}
