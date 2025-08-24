package com.rock.tgplay.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class ChunkProgressView extends LinearLayout {
    private final List<Pair<Float, Float>> downloadedChunks = new ArrayList<>();
    private Pair<Float, Float> downloadingChunk = null;
    private long fileSize = 1; // prevent divide-by-zero

    public ChunkProgressView(Context context) {
        super(context);
        init();
    }

    public ChunkProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);
    }

    public void setFileSize(long fileSize) {
        this.fileSize = Math.max(fileSize, 1);
        invalidate();
    }

    public void downloadedChunk(long offset, long length) {
        float startPercent = (offset * 100f) / fileSize;
        float endPercent = ((offset + length) * 100f) / fileSize;
        downloadedChunks.add(new Pair<>(startPercent, endPercent));
        invalidate();
    }

    public void downloadingChunk(long offset, long length) {
        float startPercent = (offset * 100f) / fileSize;
        float endPercent = ((offset + length) * 100f) / fileSize;
        downloadingChunk = new Pair<>(startPercent, endPercent);
        invalidate();
    }

    public void clearDownloadingChunk() {
        downloadingChunk = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        Paint greenPaint = new Paint();
        greenPaint.setColor(Color.GREEN);

        Paint redPaint = new Paint();
        redPaint.setColor(Color.RED);

        for (Pair<Float, Float> chunk : downloadedChunks) {
            float left = chunk.first / 100f * width;
            float right = chunk.second / 100f * width;
            canvas.drawRect(left, 0, right, height, greenPaint);
        }

        if (downloadingChunk != null) {
            float left = downloadingChunk.first / 100f * width;
            float right = downloadingChunk.second / 100f * width;
            canvas.drawRect(left, 0, right, height, redPaint);
        }
    }
}
