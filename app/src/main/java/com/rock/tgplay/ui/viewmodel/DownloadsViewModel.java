package com.rock.tgplay.ui.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rock.tgplay.tdlib.TelegramClient;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.Arrays;

public class DownloadsViewModel extends ViewModel {
    MutableLiveData<DownloadsListResponse> downloads = new MutableLiveData<>();
    String offset = "";


    public MutableLiveData<DownloadsListResponse> getDownloads(boolean fromStart) {
        if(offset == null) return null;
        if (fromStart) {
            offset = "";
        }
        Client client = TelegramClient.getInstance().getClient();
        client.send(new TdApi.SearchFileDownloads("",false,false,offset,10),object -> {
            if (object instanceof TdApi.FoundFileDownloads) {
                DownloadsListResponse response = new DownloadsListResponse();
                TdApi.FoundFileDownloads ffd = (TdApi.FoundFileDownloads) object;
                response.files.addAll(Arrays.asList(ffd.files));
                if (ffd.nextOffset.isEmpty()) {
                    offset = null;
                    response.hasNext = false;
                }else{
                    offset = ffd.nextOffset;
                    response.hasNext = true;
                }
                response.isLoadMore = !fromStart;
                downloads.postValue(response);
            }
        }, Throwable::printStackTrace);
        return downloads;
    }

    public static class DownloadsListResponse {
        public ArrayList<TdApi.FileDownload> files = new ArrayList<>();
        public boolean hasNext = true;
        public boolean isLoadMore = false;
    }
}
