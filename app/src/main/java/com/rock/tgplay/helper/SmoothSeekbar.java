package com.rock.tgplay.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;

public class SmoothSeekbar extends androidx.appcompat.widget.AppCompatSeekBar {
    private ExoPlayer exoPlayer;
    private int STEP = 1;
    private int InitStep = 1;
    Handler proghandler;
    Runnable stopTaskRun;
    private OnSeekBarChangeListener seebarList;

    public SmoothSeekbar(@NonNull Context context) {
        super(context);
    }

    public SmoothSeekbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothSeekbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                int progress = getProgress() - STEP * InitStep;
                //Log.e("TAG", "left: " + progress);
                if (progress <= getMax() && progress >= 0) {
                    setProgress(progress);
                    STEP = STEP + 1;
                    stopHandler();
                }
                return true;
            }
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                int progress = getProgress() + STEP * InitStep;
                //Log.e("TAG", "right: " + progress);
                if (progress <= getMax() && progress >= 0) {
                    setProgress(progress);
                    STEP = STEP + 1;
                    stopHandler();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    private void stopHandler() {
        if (proghandler != null && stopTaskRun != null) {
            proghandler.removeCallbacks(stopTaskRun);
            seebarList.onProgressChanged(this, getProgress(), true);
            proghandler.postDelayed(stopTaskRun, 600);
        }
    }

    public void updateSeekBar(long currentPosition, long duration) {

        int progress = (int) (currentPosition * getMax() / duration);
        //Log.e("TAG", "updateSeekBar: " + progress);
        if (STEP == 1) {
            setProgress(progress);
        } else {
            //Log.e("TAG", "userchanging: ");
        }

    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        proghandler = new Handler(Looper.getMainLooper());
        stopTaskRun = () -> {
            STEP = 1;
            if (seebarList != null) {
                seebarList.onStopTrackingTouch(SmoothSeekbar.this);
                //Log.e("TAG", "run: " + getProgress());
            }
        };
        this.seebarList = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            stopHandler();
        }else if(event.getAction() == MotionEvent.ACTION_DOWN){
            seebarList.onStartTrackingTouch(this);
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            seebarList.onProgressChanged(this, getProgress(), true);
        }
        return super.onTouchEvent(event);
    }
}
