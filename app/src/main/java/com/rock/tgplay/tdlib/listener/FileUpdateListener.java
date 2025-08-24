package com.rock.tgplay.tdlib.listener;

import org.drinkless.tdlib.TdApi;

public interface FileUpdateListener {

    void onUpdateFile(TdApi.UpdateFile updateFile);
}
