package com.rock.tgplay.player;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.google.common.collect.ImmutableList;
import com.rock.tgplay.CF;
import com.rock.tgplay.R;
import com.rock.tgplay.databinding.MediatrackBinding;
import com.rock.tgplay.databinding.RockPlayerBinding;
import com.rock.tgplay.databinding.SelectordialogBinding;
import com.rock.tgplay.helper.RockRendersFactory;
import com.rock.tgplay.tdlib.TelegramClient;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RockPlayer extends Activity {

    public RockPlayerBinding lb;
    String TAG = "player";


    public static final String BOOST_S = "boostS";
    public static final String ENHANCED_C = "enhancedC";
    Handler chandler;
    Runnable hideContp;
    ExoPlayer player;
    Handler time;
    Handler progHoverHandler;
    Runnable progHoverRunnable;
    private Runnable updateProgressAction;
    private SharedPreferences sharedPreferences;
    private boolean userIsInHurry = false;
    private boolean isAppStopped = false;
    private boolean speedX = false;
    private boolean progTracking = false;
    private String artwork;
    boolean sOpen = false;
    private boolean playNextEnable = true;
    int initialY = 0;
    private boolean boostSound = false;

    private boolean languageMode = false;
    LoudnessEnhancer enhancer;
    private Client client;
    private int fileId;

    private HistoryHelper.PlayerHistoryItem playerHistoryItem;


    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lb = RockPlayerBinding.inflate(getLayoutInflater());
        setContentView(lb.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        initVariables();
        loadPlayer(fileId);
        initHandlers();
        setOnclicker();
        try {
            showMeta();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setHeightOfWindow();
    }

    private void setHeightOfWindow() {
        lb.playerFrame.postDelayed(() -> {
            ViewGroup.LayoutParams lp = lb.playerFrame.getLayoutParams();
            lp.height = lb.playerView.getHeight();
            lb.playerFrame.setLayoutParams(lp);
        }, 300);
    }

    private void loadPlayer(int fileId) {
        if (client == null) {
            Toast.makeText(this, "File is null", Toast.LENGTH_SHORT).show();
            finish();
        }
        TdApi.GetFile getFile = new TdApi.GetFile(fileId);
        client.send(getFile, object -> {
            if (object instanceof TdApi.File) {
                TdApi.File file = (TdApi.File) object;
                lb.p.videoTitle.setText(getIntent().getStringExtra("name") + " ");
                runOnUiThread(() -> initPlayer(file));
            }
        });
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setHeightOfWindow();
        super.onConfigurationChanged(newConfig);
    }

    private void showMeta() {
        addToHistory();
        if (getIntent().hasExtra("artwork")) {
            if (artwork != null) {
                //TODO add artwork
                lb.p.artworkd.animate().alpha(0.3f).setDuration(24000).setInterpolator(new CycleInterpolator(12)).start();
            }
        }
    }

    private void addToHistory() {
        if (playerHistoryItem != null) {
            if (player != null && player.getDuration() > 0) {
                playerHistoryItem.played = (double) player.getCurrentPosition() / player.getDuration();
            }
            HistoryHelper historyHelper = new HistoryHelper(this);
            historyHelper.addToHistory(playerHistoryItem);
        }
    }

    private void passiveProg() {
        lb.p.prog.setProgress(0);
        lb.p.prog.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    userIsInHurry = true;
                    showCont(false);
                    if (player != null) {
                        long newPosition = player.getDuration() * lb.p.prog.getProgress() / 500;
                        lb.p.time.setText(stringForTime(player.getDuration() * lb.p.prog.getProgress() / 500));
                    }
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initVariables() {
        sharedPreferences = getSharedPreferences(CF.SHARED_PREFERENCES_ROCK_PLAYER, MODE_PRIVATE);
        if (getIntent().hasExtra("fileId")) {
            this.client = TelegramClient.getInstance().getClient();
            int fileId = getIntent().getIntExtra("fileId", 0);
            this.fileId = fileId;
        }
        if (getIntent().hasExtra("chatId") && getIntent().hasExtra("messageId")) {
            long chatId = getIntent().getLongExtra("chatId", 0);
            long messageId = getIntent().getLongExtra("messageId", 0);
            playerHistoryItem = new HistoryHelper.PlayerHistoryItem((double) sharedPreferences.getLong(fileId + "%", 0) / 100, System.currentTimeMillis(), chatId, messageId, fileId);
            addToHistory();
        }
    }

    private void initHandlers() {
        chandler = new Handler(getMainLooper());
        hideContp = () -> {
            hideContpF();
        };

        progHoverHandler = new Handler(getMainLooper());
        progHoverRunnable = () -> {
            ((ViewGroup) lb.p.progcont.getParent()).removeView(lb.p.progcont);
            lb.p.controller.post(() -> lb.p.controller.addView(lb.p.progcont, 0));
            lb.p.prog.setFocusable(true);
        };

        time = new Handler(getMainLooper());
        updateProgressAction = new Runnable() {
            @Override
            public void run() {
                updateProgressViews();
                time.postDelayed(this, 1000); // Update every 1 second
            }
        };


    }

    @SuppressLint("ClickableViewAccessibility")
    private void setOnclicker() {
        time.postDelayed(updateProgressAction, 1000);
        _focusButton(lb.p.playB);
        _focusButton(lb.p.prevB);
        _focusButton(lb.p.nextB);
        _focusButton(lb.p.subtitleD);
        _focusButton(lb.p.audioB);
        _focusButton(lb.p.qualityB);
        _focusButton(lb.p.stretch);
        _focusButton(lb.p.comment);
        _focusButton(lb.p.home);
        _focusButton(lb.p.boostS);
        _focusButton(lb.p.enchanceC);
        _focusButton(lb.p.speed);
        _focusButton(lb.p.pnext);
        _focusButton(lb.p.languageMode);
        lb.p.channel.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setScaleX(1.05F);
                v.setScaleY(1.05F);
            } else {
                v.setScaleX(1F);
                v.setScaleY(1F);
            }

        });
        lb.p.playB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (player.isPlaying()) {
                    player.pause();
                    lb.p.playB.setImageDrawable(getDrawable(R.drawable.play));

                } else {
                    player.play();
                    lb.p.playB.setImageDrawable(getDrawable(R.drawable.pause_24dp));
                    hideContpF();

                }
            }
        });
        addGestures();
        //prog-logic
        lb.p.qualityB.setOnClickListener(v -> {
            queryTrack(C.TRACK_TYPE_VIDEO);
            hideContpF();
        });
        lb.p.audioB.setOnClickListener(v -> {
            queryTrack(C.TRACK_TYPE_AUDIO);
            hideContpF();
        });
        lb.p.subtitleD.setOnClickListener(v -> {
            queryTrack(C.TRACK_TYPE_TEXT);
            hideContpF();

        });
        lb.p.subtitleD.setOnLongClickListener(v -> {
            queryTrack(C.TRACK_TYPE_TEXT);
            return true;
        });
        lb.p.nextB.setOnClickListener(v -> {
            player.seekTo(player.getCurrentPosition() + 10000);
        });
        lb.p.prevB.setOnClickListener(v -> {
            player.seekTo(player.getCurrentPosition() - 10000);
        });
        lb.p.pnext.setOnClickListener(v -> {
            playNextVideo();
        });
        lb.p.boostS.setOnClickListener(v -> {
            boostSound = !boostSound;
            if (player != null) boostSoundInPlayer(player);
        });
        lb.p.languageMode.setOnClickListener(v -> {
            languageMode = !languageMode;
            if (player != null) changeLanguage(player);
        });

        lb.p.enchanceC.setOnClickListener(v -> {
            boolean enchancedCB = sharedPreferences.getBoolean(ENHANCED_C, false);
            enchancedCB = !enchancedCB;
            sharedPreferences.edit().putBoolean(ENHANCED_C, enchancedCB).apply();
            if (player != null) {
                enhancedC(enchancedCB);
            }
        });

        lb.p.home.setOnClickListener(v -> {
            if (player != null) {
                if (player.getPlaybackParameters().speed == 1F) {
                    player.setPlaybackParameters(new PlaybackParameters(1.5f));
                    showPlayerToast("1.5x speed", false);
                } else {
                    showPlayerToast("1x speed", true);
                    player.setPlaybackParameters(new PlaybackParameters(1f));
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addGestures() {

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                showCont(true); // Show controls on single tap
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (e.getRawX() > lb.playerView.getWidth() / 2) {
                    if (player != null) {
                        player.seekTo(player.getCurrentPosition() + 10000);
                        showPlayerToast("❯❯❯ 10 seconds", true);
                    }
                } else {
                    if (player != null) {
                        player.seekTo(player.getCurrentPosition() - 10000);
                        showPlayerToast("❮❮❮ 10 seconds", true);
                    }
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (player != null) {
                    player.setPlaybackParameters(new PlaybackParameters(2f));
                    showPlayerToast("2x speed", false);
                    speedX = true;
                }
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        AtomicBoolean zoomEnabled = new AtomicBoolean(false);
        AtomicReference<MotionEvent.PointerCoords> oldZoom1 = new AtomicReference<>(new MotionEvent.PointerCoords());
        AtomicReference<MotionEvent.PointerCoords> oldZoom2 = new AtomicReference<>(new MotionEvent.PointerCoords());
        AtomicReference<Float> initialScale = new AtomicReference<>(lb.playerView.getScaleX());
        lb.p.contpBack.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (player != null && speedX) {
                    speedX = false;
                    player.setPlaybackParameters(new PlaybackParameters(1f));
                    hidePlayerToast();
                }
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                initialY = (int) event.getRawY();

            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                initialY = (int) event.getRawY();
                if (event.getPointerCount() > 1) {
                    MotionEvent.PointerCoords po1 = new MotionEvent.PointerCoords();
                    MotionEvent.PointerCoords po2 = new MotionEvent.PointerCoords();
                    event.getPointerCoords(0, po1);
                    event.getPointerCoords(1, po2);
                    if(zoomEnabled.get()){
                        float scaleFactor = initialScale.get() - (float) (distance(oldZoom1.get(), oldZoom2.get()) - distance(po1, po2)) * 0.8F / lb.playerView.getWidth();
                        scaleFactor = Math.max(0.2f, Math.min(scaleFactor, 2.0f));
                        showPlayerToast("Zoom " + scaleFactor,true);
                        lb.playerView.setScaleX(scaleFactor);
                        lb.playerView.setScaleY(scaleFactor);
                          } else {
                        zoomEnabled.set(true);
                        initialScale.set(lb.playerView.getScaleX());
                        oldZoom1.set(po1);
                        oldZoom2.set(po2);
                    }



                }else{
                    zoomEnabled.set(false);
                }
          }
            return gestureDetector.onTouchEvent(event);
        });
        lb.p.contp.setOnClickListener(v -> {
            hideContpF();
        });

    }

    private int distance(MotionEvent.PointerCoords oldZoom1, MotionEvent.PointerCoords oldZoom2) {
        float dx = oldZoom1.x - oldZoom2.x;
        float dy = oldZoom1.y - oldZoom2.y;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void enhancedC(boolean enhancedCB) {
        if (enhancedCB) {
            TextureView textureView = (TextureView) lb.playerView.getVideoSurfaceView();
            Paint paint = getEnhancedPaint();
            if (textureView != null) textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } else {
            TextureView textureView = (TextureView) lb.playerView.getVideoSurfaceView();
            if (textureView != null)
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, new Paint());
        }
    }

    @NonNull
    private static Paint getEnhancedPaint() {
        ColorMatrix finalMatrix = new ColorMatrix();

        // 💡 1. Saturation (1 = normal, >1 = more color, <1 = less)
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(1.5f); // Enhance saturation
        finalMatrix.postConcat(saturationMatrix);

        // 💡 2. Contrast (1 = normal, >1 = more contrast, <1 = less)
        float contrast = 1.2f;
        float translate = (-0.5f * contrast + 0.5f) * 255f;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        });
        finalMatrix.postConcat(contrastMatrix);

        // 💡 3. Brightness (0 = dark, 255 = white; add value)
        float brightness = 20f; // Slightly brighter
        ColorMatrix brightnessMatrix = new ColorMatrix(new float[]{
                1, 0, 0, 0, brightness,
                0, 1, 0, 0, brightness,
                0, 0, 1, 0, brightness,
                0, 0, 0, 1, 0
        });
        finalMatrix.postConcat(brightnessMatrix);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(finalMatrix));
        return paint;
    }

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint("ResourceAsColor")
    private void boostSoundInPlayer(ExoPlayer player) {
        if (boostSound) {
            lb.p.boostS.setBackgroundTintList(ColorStateList.valueOf(R.color.primary));
            enhancer = new LoudnessEnhancer(player.getAudioSessionId());
            enhancer.setTargetGain(1500);
            enhancer.setEnabled(true);
        } else {
            lb.p.boostS.setBackgroundTintList(ColorStateList.valueOf(R.color.white));
            if (enhancer != null) enhancer.setEnabled(false);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void changeLanguage(ExoPlayer player) {
        if (languageMode) {
            lb.p.languageMode.setBackgroundTintList(ColorStateList.valueOf(R.color.primary));
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredTextLanguage("en-US").setPreferredAudioLanguages("hi", "hin", "en", "en-US").build());
        } else {
            lb.p.languageMode.setBackgroundTintList(ColorStateList.valueOf(R.color.white));
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredTextLanguage("en-US").setPreferredAudioLanguages("kor", "ko", "en", "en-US").build());
        }
    }

    private void openS() {
        /*ValueAnimator va = new ValueAnimator();
        va.setDuration(300);
        va.setInterpolator(new AccelerateInterpolator());
        va.setFloatValues(lb.scrollView.getScrollY(), lb.s.getRoot().getHeight());
        va.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            lb.scrollView.scrollTo(0, (int) animatedValue);
        });
        Log.e(TAG, "openS: " + "openS" + lb.s.getRoot().getHeight());
        va.start();
        lb.s.getRoot().setVisibility(View.VISIBLE);
        sOpen = true;*/
    }

    private void hidePlayerToast() {
        lb.p.playerToast.animate().alpha(0).setDuration(200).start();
    }

    private void showPlayerToast(String s, boolean hideAfterSomeTime) {
        lb.p.playerToast.setText(s);
        lb.p.playerToast.animate().alpha(1).setDuration(200).start();
        if (hideAfterSomeTime) {
            lb.p.playerToast.postDelayed(() -> {
                hidePlayerToast();
            }, 300);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppStopped = false;
    }

    @UnstableApi
    private AnalyticsListener analyticsListener = new AnalyticsListener() {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onIsPlayingChanged(@NonNull EventTime eventTime, boolean isPlaying) {
            runOnUiThread(() -> {
                if (isPlaying) {
                    lb.p.playB.setImageDrawable(getDrawable(R.drawable.pause_24dp));
                } else {
                    lb.p.playB.setImageDrawable(getDrawable(R.drawable.play));
                }
            });

            if (player.getCurrentPosition() > (player.getDuration() - 100)) {
                Log.e(TAG, "updateProgressViews: " + "completed");
            }
        }

        @Override
        public void onRenderedFirstFrame(@NonNull EventTime eventTime, @NonNull Object output, long renderTimeMs) {
            runOnUiThread(() -> lb.p.artworkd.setVisibility(View.GONE));
        }

        @Override
        public void onPlayerError(@NonNull EventTime eventTime, @NonNull PlaybackException error) {
            Log.e(TAG, "Player Error:", error);
        }

        @Override
        public void onAudioSessionIdChanged(@NonNull EventTime eventTime, int audioSessionId) {
            if (boostSound) {
                if (enhancer != null) enhancer.release();
                enhancer = new LoudnessEnhancer(audioSessionId);
                enhancer.setTargetGain(1500);
                enhancer.setEnabled(true);
                Log.e(getClass().getSimpleName(), "Audio Session id changed and Enhancing audio : " + audioSessionId);
            }
            AnalyticsListener.super.onAudioSessionIdChanged(eventTime, audioSessionId);
        }
    };

    @OptIn(markerClass = UnstableApi.class)
    private void initPlayer(TdApi.File file) {
        //player initialisation
        RockRendersFactory renderersFactory = new RockRendersFactory(getApplicationContext()).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        player = new ExoPlayer.Builder(this).setRenderersFactory(renderersFactory).build();
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredTextLanguage("en-US").setPreferredAudioLanguages("kor", "ko", "en", "en-US").build());
        lb.playerView.setPlayer(player);
        player.addAnalyticsListener(analyticsListener);
        //source initialisation
        File localFile = new File(file.local.path);
        Log.d("helllo", "playInUi: " + localFile.getAbsolutePath());
        MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(localFile));
        TelegramFileDataSource.Factory factory = new TelegramFileDataSource.Factory(client, fileId, file.size);
        factory.setChunkProgressView(lb.p.chunkProgressView);
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        long startOffset = getStartOffset();
        player.setMediaSource(mediaSource, startOffset);
        if (!isAppStopped) {
            player.prepare();
            player.play();
            hideContpF();
            if (userIsInHurry) {
                player.seekTo(player.getDuration() * lb.p.prog.getProgress() / 500);
                userIsInHurry = false;
            }
            progLogic();

        }

    }

    private long getStartOffset() {
        long startOffset = 0;
        if (sharedPreferences.getLong(fileId + "%", 0) < 90) {
            startOffset = sharedPreferences.getLong(fileId + "ms", 0);
        }
        return startOffset;
    }

    private void stateChanged(int state) {
        boostSoundInPlayer(player);
        Log.e(TAG, "stateChanged: " + state);
    }


    private void progLogic() {


        lb.p.prog.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    showCont(false);
                    long newPosition = player.getDuration() * lb.p.prog.getProgress() / 500;
                    lb.p.time.setText(stringForTime(player.getDuration() * lb.p.prog.getProgress() / 500));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(player.getDuration() * lb.p.prog.getProgress() / 500);
                progTracking = false;
            }
        });
        AtomicBoolean progActive = new AtomicBoolean(false);
        final long[] oldpos = {0};
        lb.p.prog.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                progActive.set(true);
                oldpos[0] = lb.p.prog.getProgress();
                lb.p.prog.setThumb(getDrawable(R.drawable.thumb_pressed));
                try {
                    time.removeCallbacks(updateProgressAction);
                } catch (Exception e) {
                }

            } else {
                if (lb.p.prog.getProgress() != oldpos[0] && progActive.get()) {
                    Log.e(TAG, "onStopTrackingTouch: " + "progrssChanged");
                    player.seekTo(player.getDuration() * lb.p.prog.getProgress() / 500);
                    progActive.set(false);
                }
                lb.p.prog.setThumb(getDrawable(R.drawable.trans));
                time.postDelayed(updateProgressAction, 1000);
            }
        });
        lb.p.prog.setOnClickListener(v -> {
            Log.e(TAG, "onStopTrackingTouch: " + "clicked");
            if (progActive.get()) {
                Log.e(TAG, "onStopTrackingTouch: " + "progrssChanged");
                player.seekTo(player.getDuration() * lb.p.prog.getProgress() / 500);
            }
        });

    }

    @Override
    protected void onStop() {
        isAppStopped = true;
        try {
            player.stop();
            player.release();
            time.removeCallbacks(updateProgressAction);
            Log.e("t", "" + player.getCurrentPosition() / 1000);
            long currnt = player.getCurrentPosition();
            long total = player.getDuration();
            String episodeID = String.valueOf(fileId);
            sharedPreferences.edit().putLong(episodeID + "ms", currnt).apply();
            sharedPreferences.edit().putLong(episodeID + "%", currnt * 100 / total).apply();
            addToHistory();
            TdApi.DeleteFile deleteFile = new TdApi.DeleteFile(fileId);
            client.send(new TdApi.CancelDownloadFile(fileId, false), object -> {
                Log.e(getClass().getSimpleName(), "Download Paused: ");
            });
            client.send(deleteFile, object -> {
                if (object instanceof TdApi.Ok) {
                    Log.e("TAG", "File deleted successfully");
                }
            });
        } catch (Exception e) {
            finish();
        }
        super.onStop();
    }

    public void _focusButton(final View _view) {
        _view.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                showCont(false);
                view.setBackground(getDrawable(R.drawable.selected_circle));
                ((ImageButton) view).setColorFilter(Color.parseColor("#555555"));

            } else {
                view.setBackground(getDrawable(R.drawable.notselected));
                ((ImageButton) view).setColorFilter(Color.parseColor("#ffffff"));
            }
        });

    }

    public void showCont(boolean animate) {
        try {
            chandler.removeCallbacks(hideContp);
        } catch (Exception e) {
        }
        lb.scrollView.setScrollY(0);
        lb.p.contp.setAlpha(1F);
        lb.p.contp.setVisibility(View.VISIBLE);
        if (animate) {
            lb.p.controller.setTranslationY(lb.p.controller.getHeight());
            lb.p.controller.animate().translationY(0).setDuration(300).start();
            lb.p.titleBox.setTranslationY(-lb.p.titleBox.getHeight());
            lb.p.titleBox.animate().translationY(0).setDuration(300).start();
        }
        chandler.postDelayed(hideContp, 5500);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (sOpen) {
                if (keyEvent.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
                    closeS();
                    return true;
                }
                return false;
            }
            if (lb.p.contp.getVisibility() == View.INVISIBLE) {

                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    showCont(true);
                    lb.p.playB.requestFocus();
                }
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    openS();
                    return true;
                }

                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (player != null) {
                        player.seekTo(player.getCurrentPosition() - 10000);
                        lb.p.time.setText(stringForTime(player.getDuration() * lb.p.prog.getProgress() / 500));
                        lb.p.prog.setProgress((int) (player.getCurrentPosition() * 500 / player.getDuration()));
                        showProgHover();
                    }
                }
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (player != null) {
                        player.seekTo(player.getCurrentPosition() + 10000);
                        lb.p.prog.setProgress((int) (player.getCurrentPosition() * 500 / player.getDuration()));
                        lb.p.time.setText(stringForTime(player.getDuration() * lb.p.prog.getProgress() / 500));
                        showProgHover();
                    }
                }
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                    showCont(true);
                    if (player.isPlaying()) {
                        player.pause();
                        lb.p.playB.setImageDrawable(getDrawable(R.drawable.play));
                    } else {
                        player.play();
                        lb.p.playB.setImageDrawable(getDrawable(R.drawable.pause_24dp));
                    }
                }
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    try {
                        if (player.isPlaying()) {
                            player.pause();
                            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
                        } else {
                            RockPlayer.this.finish();
                        }
                    } catch (Exception e) {
                        RockPlayer.this.finish();
                    }
                }
                return false;
            } else if (lb.p.contp.getVisibility() == View.VISIBLE) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    hideContpF();
                }
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    if (lb.p.playB.hasFocus()) hideContpF();
                }
                return false;
            }
        } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
        }

        return super.onKeyDown(keyCode, keyEvent);
    }

    private void closeS() {
        lb.scrollView.setScrollY(0);
        lb.s.getRoot().setVisibility(View.INVISIBLE);
        sOpen = false;
    }

    private void hideContpF() {
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.addUpdateListener(animation -> {
            lb.p.contp.setAlpha((Float) animation.getAnimatedValue());
            lb.p.controller.animate().translationY(lb.p.controller.getHeight()).setDuration(300).start();
            lb.p.titleBox.animate().translationY(-lb.p.titleBox.getHeight()).setDuration(300).start();
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                lb.p.contp.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration(200);
        animator.start();
        lb.playerView.requestFocus();
    }

    private void showProgHover() {
        progHoverHandler.removeCallbacks(progHoverRunnable);
        ((ViewGroup) lb.p.progcont.getParent()).removeView(lb.p.progcont);
        lb.p.hoverprogholder.addView(lb.p.progcont);
        lb.p.prog.setFocusable(false);
        progHoverHandler.postDelayed(progHoverRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    private void updateProgressViews() {
        if (player != null) {
            if (!progTracking) {
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();
                lb.p.time.setText(stringForTime(currentPosition));
                if (player.getDuration() > 0) {
                    lb.p.time2.setText(stringForTime(duration));
                }
                long remainingTime = duration - currentPosition;
                if (remainingTime < 20000 && remainingTime > 5000) {
                    if (playNextEnable) {
                        player.stop();
                        playNextEnable = false;
                        playNextVideo();
                        return;
                    }
                }
                long estimatedEndTimeMillis = System.currentTimeMillis() + remainingTime;
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a"); // e.g., 2:34 PM
                String endTimeString = timeFormat.format(new Date(estimatedEndTimeMillis));
                lb.p.time3e.setText("End at " + endTimeString);
                if (player.isPlaying()) {
                    lb.p.prog.setProgress((int) (currentPosition * 500 / duration));
                    lb.p.prog.setSecondaryProgress((int) (player.getBufferedPosition() * 500 / duration));
                }
            }
            if (isAppStopped) {
                player.stop();
                player.release();
            }
        }
    }

    private void playNextVideo() {

    }

    private String stringForTime(long timeMs) {
        int seconds = (int) (timeMs / 1000);
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;

        Formatter timeFormatter = new Formatter(new StringBuilder(), Locale.getDefault());

        timeFormatter.format("%02d:%02d:%02d", h, m, s);
        return timeFormatter.toString();
    }

    @OptIn(markerClass = UnstableApi.class)
    public void queryTrack(int trackTypeVideo) {

        SelectordialogBinding sb = SelectordialogBinding.inflate(getLayoutInflater());
        AlertDialog.Builder tbuilder = new AlertDialog.Builder(this, R.style.PlayerDialogStyle);
        tbuilder.setView(sb.getRoot());
        Tracks tracks = player.getCurrentTracks();
        ImmutableList<Tracks.Group> groups = tracks.getGroups();

        AlertDialog alertDialog = tbuilder.create();

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.END; // Position the dialog to the right
            params.width = WindowManager.LayoutParams.WRAP_CONTENT; // Adjust width
            params.height = WindowManager.LayoutParams.MATCH_PARENT; // Full height
            window.setAttributes(params);
        }
        for (int j = 0; j < groups.size(); j++) {
            Tracks.Group tg = groups.get(j);

            if (tg.getType() == trackTypeVideo) {
                ArrayList<Format> formats = new ArrayList<>();
                for (int i = 0; i < tg.length; i++) {
                    formats.add(tg.getTrackFormat(i));
                }

                switch (tg.getType()) {
                    case C.TRACK_TYPE_VIDEO:
                        Collections.sort(formats, Comparator.comparingInt((Format format) -> format.height));
                        break;
                    case C.TRACK_TYPE_AUDIO:
                        Collections.sort(formats, Comparator.comparingInt((Format format) -> format.bitrate));
                        break;
                    case C.TRACK_TYPE_TEXT:
                        break;
                    default:
                        break;
                }

                addTracks(formats, sb.epglist, tg, alertDialog);
            }
        }

        alertDialog.show();
        setFullScreenPlayer(false);
        alertDialog.setOnDismissListener(dialog -> {
            setFullScreenPlayer(true);
        });

    }

    private void setFullScreenPlayer(boolean b) {
        if (b) {
            ViewGroup.LayoutParams lp = lb.playerView.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            lb.playerView.setLayoutParams(lp);
        } else {
            ViewGroup.LayoutParams lp = lb.playerView.getLayoutParams();

            lp.width = lb.playerView.getWidth() - dpToPx(this, 500);
            lp.height = (int) (lp.width * (9.0 / 16.0));

            Log.e(TAG, "setFullScreenPlayer: " + lp);
            lb.playerView.setLayoutParams(lp);
        }
    }

    private int dpToPx(RockPlayer rockPlayer, int i) {
        //return dp to px
        return (int) (i * rockPlayer.getResources().getDisplayMetrics().density);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void addTracks(ArrayList<Format> listFormat, LinearLayout epglist, Tracks.Group tg, AlertDialog alertDialog) {

        for (int i = 0; i < listFormat.size(); i++) {

            MediatrackBinding tbb = MediatrackBinding.inflate(getLayoutInflater(), epglist, true);
            tbb.titletxt.setText(tg.getTrackFormat(i).label);
            tbb.selectb.setChecked(tg.isTrackSelected(i));
            switch (tg.getType()) {
                case C.TRACK_TYPE_VIDEO:
                    tbb.titletxt.setText(tg.getTrackFormat(i).height + "p");
                    tbb.trackdesc.setText(tg.getTrackFormat(i).width + "x" + tg.getTrackFormat(i).height + " · " + tg.getTrackFormat(i).codecs + " · " + tg.getTrackFormat(i).frameRate);
                    break;
                case C.TRACK_TYPE_AUDIO:
                    tbb.trackdesc.setText(tg.getTrackFormat(i).language + " · " + tg.getTrackFormat(i).codecs + " · " + tg.getTrackFormat(i).bitrate);
                    break;
                case C.TRACK_TYPE_TEXT:
                    tbb.trackdesc.setText(tg.getTrackFormat(i).language + " · " + tg.getTrackFormat(i).frameRate);
                    break;
                default:
                    tbb.trackdesc.setText("unkonwn");
                    break;
            }
            int finalI = i;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tbb.selectb.setFocusable(View.NOT_FOCUSABLE);
            }
            tbb.linear1.setOnClickListener(v -> {
                tbb.selectb.setChecked(!tbb.selectb.isChecked());
                alertDialog.dismiss();
            });
            tbb.selectb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(tg.getType(), false).build());
                    player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(tg.getMediaTrackGroup(), finalI)).build());
                } else {
                    player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(tg.getType(), true).build());
                }
            });
            tbb.linear1.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    tbb.titletxt.setTextColor(getColor(R.color.black));
                    tbb.trackdesc.setTextColor(getColor(R.color.black));
                    tbb.linear1.setBackgroundResource(R.drawable.selected);
                    tbb.selectb.setBackgroundDrawable(getDrawable(R.drawable.notselected));
                } else {
                    tbb.titletxt.setTextColor(getColor(R.color.white));
                    tbb.trackdesc.setTextColor(getColor(R.color.white));
                    tbb.linear1.setBackgroundResource(R.drawable.epgback);
                    tbb.selectb.setBackgroundDrawable(getDrawable(R.drawable.trans));
                }
            });
            if (tg.isTrackSelected(i)) {
                new Handler().postDelayed(() -> {
                    tbb.linear1.requestFocus();
                }, 200);
            }

        }

    }

    public boolean isTvDevice(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}