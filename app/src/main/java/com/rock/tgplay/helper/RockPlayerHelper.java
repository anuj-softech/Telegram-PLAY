package com.rock.tgplay.helper;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.source.MediaSource;


import java.util.Collections;
import java.util.List;

public class RockPlayerHelper {
    private static final String TAG = "RockPlayerHelper";

    public RockPlayerHelper() {
    }

    public void addToHistory() {
    }

    public MediaSource getMediaItem(Object ob) {
        return null;
    }
    @OptIn(markerClass = UnstableApi.class)
    public RenderersFactory getRenderersFactory(Context context) {
        return new DefaultRenderersFactory(context).setMediaCodecSelector((mimeType, requiresSecureDecoder, requiresTunnelingDecoder) -> {
            List<MediaCodecInfo> codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
            if (codecs.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.singletonList(codecs.get(0));
        });
    }

    public String getTitle() {
        return "RockPlayer";
    }

    public String getEpisode() {
        return "";
    }

    public String getEpisodeID() {
        return "default";
    }
    public Object playNext(){
        return null;
    }

    public Intent startPlayNext(Object ob) {

        return null;
    }
}
