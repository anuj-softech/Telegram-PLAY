package com.rock.tgplay.tdlib.model;

import java.io.Serializable;

public class TgAppCredentials implements Serializable {
    String API_ID;
    String API_HASH;

    public TgAppCredentials(String api_id, String api_hash) {
        API_ID = api_id;
        API_HASH = api_hash;
    }

    //getters and setters
    public String getAPI_ID() {
        return API_ID;
    }

    public void setAPI_ID(String API_ID) {
        this.API_ID = API_ID;
    }

    public String getAPI_HASH() {
        return API_HASH;
    }

    public void setAPI_HASH(String API_HASH) {
        this.API_HASH = API_HASH;
    }
}