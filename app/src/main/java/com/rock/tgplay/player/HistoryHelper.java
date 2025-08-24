package com.rock.tgplay.player;

import android.content.Context;
import android.util.Log;

import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class HistoryHelper {
    private static final String HISTORY_PREFS_NAME = "history_prefs";
    private static final String HISTORY_KEY = "history";

    Context context;

    public HistoryHelper(Context context) {
        this.context = context;
    }

    public void addToHistory(PlayerHistoryItem item) {
        item.timestamp = System.currentTimeMillis();
        ArrayList<PlayerHistoryItem> history = getPlayedHistory();
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).messageId == item.messageId && history.get(i).chatId == item.chatId) {
                history.remove(i);
                break;
            }
        }
        history.add(0,item);
        Log.e(getClass().getSimpleName(), history.size()+"Adding history: "+item.played + item.messageId + item.chatId);
        saveHistory(history);
    }

    private void saveHistory(ArrayList<PlayerHistoryItem> history) {
        try {
            File file = new File(context.getFilesDir(), "history.ser");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(history);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<PlayerHistoryItem> getPlayedHistory() {
        File file = new File(context.getFilesDir(), "history.ser");
        if (!file.exists()) return new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<PlayerHistoryItem> history = (ArrayList<PlayerHistoryItem>) ois.readObject();
            ois.close();
            fis.close();
            return history;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public static class PlayerHistoryItem implements Serializable {
        public double played;
        public long timestamp;
        public long messageId;
        public long chatId;

        public int fileId;

        public PlayerHistoryItem(double played, long timestamp, long messageId, long chatId,int fileId) {
            this.played = played;
            this.timestamp = timestamp;
            this.messageId = messageId;
            this.chatId = chatId;
            this.fileId = fileId;
        }
    }
}
