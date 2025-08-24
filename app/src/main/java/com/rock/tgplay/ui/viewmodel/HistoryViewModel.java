package com.rock.tgplay.ui.viewmodel;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rock.tgplay.player.HistoryHelper;

import java.util.ArrayList;

public class HistoryViewModel extends ViewModel {
    MutableLiveData<ArrayList<HistoryHelper.PlayerHistoryItem>> history = new MutableLiveData<>();


    public MutableLiveData<ArrayList<HistoryHelper.PlayerHistoryItem>> getHistory(Context context) {
        ArrayList<HistoryHelper.PlayerHistoryItem> historyItemArrayList = new HistoryHelper(context).getPlayedHistory();
        history.postValue(historyItemArrayList);
        return history;
    }
}
