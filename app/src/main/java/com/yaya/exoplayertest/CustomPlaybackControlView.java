package com.yaya.exoplayertest;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import jp.wasabeef.glide.transformations.BlurTransformation;

/**
 * Created by Administrator on 2018/4/1.
 * 邮箱：jiaqipang@bupt.edu.cn
 * package_name com.yaya.exoplayertest
 * project_name ExoplayerTest
 * 功能：
 */
public class CustomPlaybackControlView extends FrameLayout {


    public interface EndedListener {
        void musicEnd();
    }

    /**
     * Listener to be notified about changes of the visibility of the UI control.
     */
    public interface VisibilityListener {

        /**
         * Called when the visibility changes.
         *
         * @param visibility The new visibility. Either {@link View#VISIBLE} or {@link View#GONE}.
         */
        void onVisibilityChange(int visibility);

    }

    /**
     * Dispatches operations to the player.
     * <p>
     * Implementations may choose to suppress (e.g. prevent playback from resuming if audio focus is
     * denied) or modify (e.g. change the seek position to prevent a user from seeking past a
     * non-skippable advert) operations.
     */
    public interface ControlDispatcher {

        /**
         * Dispatches a {@link ExoPlayer#setPlayWhenReady(boolean)} operation.
         *
         * @param player        The player to which the operation should be dispatched.
         * @param playWhenReady Whether playback should proceed when ready.
         * @return True if the operation was dispatched. False if suppressed.
         */
        boolean dispatchSetPlayWhenReady(ExoPlayer player, boolean playWhenReady);

        /**
         * Dispatches a {@link ExoPlayer#seekTo(int, long)} operation.
         *
         * @param player      The player to which the operation should be dispatched.
         * @param windowIndex The index of the window.
         * @param positionMs  The seek position in the specified window, or {@link C#TIME_UNSET} to seek
         *                    to the window's default position.
         * @return True if the operation was dispatched. False if suppressed.
         */
        boolean dispatchSeekTo(ExoPlayer player, int windowIndex, long positionMs);

    }

    /**
     * Default {@link ControlDispatcher} that dispatches operations to the player without
     * modification.
     */
    public static final ControlDispatcher DEFAULT_CONTROL_DISPATCHER = new ControlDispatcher() {

        @Override
        public boolean dispatchSetPlayWhenReady(ExoPlayer player, boolean playWhenReady) {
            player.setPlayWhenReady(playWhenReady);
            return true;
        }

        @Override
        public boolean dispatchSeekTo(ExoPlayer player, int windowIndex, long positionMs) {
            player.seekTo(windowIndex, positionMs);
            return true;
        }

    };

    public static final int DEFAULT_FAST_FORWARD_MS = 15000;
    public static final int DEFAULT_REWIND_MS = 5000;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;

    /**
     * The maximum number of windows that can be shown in a multi-window time bar.
     */
    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;

    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;

    private final ComponentListener componentListener;
    private final View previousButton;
    private final View nextButton;
    private final View playButton;
    private final View pauseButton;
    private final View fastForwardButton;
    private final View rewindButton;
    private final TextView durationView;
    private final TextView positionView;
    private final CustomTimeBar timeBar;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private final Timeline.Period period;
    private final Timeline.Window window;

    private ExoPlayer player;
    private ControlDispatcher controlDispatcher;
    private VisibilityListener visibilityListener;

    private EndedListener endedListener;

    long duration = 0;

    /**
     * new added views
     */
    private ImageView discStatic;
    private ImageView discMove;
    private ObjectAnimator animator;
    private boolean isStarted;
    private ImageView background;
    private TextView nameView;
    private ImageView customNext;
    private ImageView customPrev;


    private boolean isAttachedToWindow;
    private boolean showMultiWindowTimeBar;
    private boolean multiWindowTimeBar;
    private boolean scrubbing;
    private int rewindMs;
    private int fastForwardMs;
    private int showTimeoutMs;
    private long hideAtMs;
    private long[] adBreakTimesMs;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public CustomPlaybackControlView(Context context) {
        this(context, null);
    }

    public CustomPlaybackControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CustomPlaybackControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //int controllerLayoutId = R.layout.exo_playback_control_view;
        int controllerLayoutId = R.layout.playback_control_view2;
        rewindMs = DEFAULT_REWIND_MS;
        fastForwardMs = DEFAULT_FAST_FORWARD_MS;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.PlaybackControlView, 0, 0);
            try {
                rewindMs = a.getInt(R.styleable.PlaybackControlView_rewind_increment, rewindMs);
                fastForwardMs = a.getInt(R.styleable.PlaybackControlView_fastforward_increment,
                        fastForwardMs);
                showTimeoutMs = a.getInt(R.styleable.PlaybackControlView_show_timeout, showTimeoutMs);
//                controllerLayoutId = a.getResourceId(R.styleable.PlaybackControlView_controller_layout_id,
//                        controllerLayoutId);
            } finally {
                a.recycle();
            }
        }
        period = new Timeline.Period();
        window = new Timeline.Window();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        adBreakTimesMs = new long[0];
        componentListener = new ComponentListener();
        controlDispatcher = DEFAULT_CONTROL_DISPATCHER;

        LayoutInflater.from(context).inflate(controllerLayoutId, this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        durationView = findViewById(R.id.exo_duration);
        positionView = findViewById(R.id.exo_position);
        timeBar = findViewById(R.id.exo_progress);
        if (timeBar != null) {
            timeBar.setListener(componentListener);
            //timeBar.setEnabled(false);
        }
        playButton = findViewById(R.id.exo_play);
        if (playButton != null) {
            playButton.setOnClickListener(componentListener);
            //setButtonEnabled(false, playButton);
        }
        pauseButton = findViewById(R.id.exo_pause);
        if (pauseButton != null) {
            pauseButton.setOnClickListener(componentListener);
        }
        previousButton = findViewById(R.id.exo_prev);
        if (previousButton != null) {
            previousButton.setOnClickListener(componentListener);
        }
        nextButton = findViewById(R.id.exo_next);
        if (nextButton != null) {
            nextButton.setOnClickListener(componentListener);
        }
        rewindButton = findViewById(R.id.exo_rew);
        if (rewindButton != null) {
            rewindButton.setOnClickListener(componentListener);
        }
        fastForwardButton = findViewById(R.id.exo_ffwd);
        if (fastForwardButton != null) {
            fastForwardButton.setOnClickListener(componentListener);
        }

        initCustomUI();

    }

    private void initCustomUI() {
        discStatic = findViewById(R.id.disc_static);

        discMove = findViewById(R.id.disc_move);
        background = findViewById(R.id.music_background);

        nameView = findViewById(R.id.music_name_big);

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();

        ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) discStatic.getLayoutParams();
        params1.height = width - 104 * 2;
        params1.width = width - 104 * 2;
        discStatic.setLayoutParams(params1);

        ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) discMove.getLayoutParams();
        params2.height = width - 208 * 2;
        params2.width = width - 208 * 2;
        discMove.setLayoutParams(params2);

        discStatic.setImageDrawable(Utils.getRoundBitmap(getContext(), R.drawable.black));
        discMove.setImageDrawable(Utils.getRoundBitmap(getContext(), R.mipmap.ic_launcher));

        animator = ObjectAnimator.ofFloat(discMove, "rotation", 0f, 360.0f);
        animator.setDuration(20000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        isStarted = false;

        customNext = findViewById(R.id.custom_next);
        customPrev = findViewById(R.id.custom_prev);
    }

    public void setEndedListener(EndedListener endedListener) {
        this.endedListener = endedListener;
    }

    public void setPrevClickListener(OnClickListener clickListener) {
        customPrev.setOnClickListener(clickListener);
    }

    public void setNextClickListener(OnClickListener clickListener) {
        customNext.setOnClickListener(clickListener);
    }

    public void setPrevEnable(boolean enable) {
        customPrev.setEnabled(enable);
    }

    public void setNextEnable(boolean enable) {
        customNext.setEnabled(enable);
    }

    public void setMusicName(String name) {
        //nameView = findViewById(R.id.music_name_big);
        nameView.setText(name);
        Log.d("name", "name=" + name);
    }

    public void setMusicPic(int resId) {
        discMove.setImageResource(resId);
    }

    public void setDiscMove(int resId) {
        discMove.setImageDrawable(Utils.getRoundBitmap(getContext(), resId));
        Glide.with(getContext()).load(resId).bitmapTransform(new BlurTransformation(getContext(), 25), new CenterCrop(getContext())).into(background);
        isStarted = false;
    }

    /**
     * Returns the player currently being controlled by this view, or null if no player is set.
     */
    public ExoPlayer getPlayer() {
        return player;
    }

    /**
     * Sets the {@link ExoPlayer} to control.
     *
     * @param player The {@code ExoPlayer} to control.
     */
    public void setPlayer(ExoPlayer player) {
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removeListener(componentListener);
        }
        this.player = player;
        if (player != null) {
            player.addListener(componentListener);
        }
        updateAll();
    }

    public void startMusic() {
        controlDispatcher.dispatchSetPlayWhenReady(player, true);
        startDisc();
    }

    /**
     * Sets whether the time bar should show all windows, as opposed to just the current one. If the
     * timeline has a period with unknown duration or more than
     * {@link #MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR} windows the time bar will fall back to showing a
     * single window.
     *
     * @param showMultiWindowTimeBar Whether the time bar should show all windows.
     */
    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar;
        updateTimeBarMode();
    }

    /**
     * Sets the {@link VisibilityListener}.
     *
     * @param listener The listener to be notified about visibility changes.
     */
    public void setVisibilityListener(VisibilityListener listener) {
        this.visibilityListener = listener;
    }

    /**
     * Sets the {@link ControlDispatcher}.
     *
     * @param controlDispatcher The {@link ControlDispatcher}, or null to use
     *                          {@link #DEFAULT_CONTROL_DISPATCHER}.
     */
    public void setControlDispatcher(ControlDispatcher controlDispatcher) {
        this.controlDispatcher = controlDispatcher == null ? DEFAULT_CONTROL_DISPATCHER
                : controlDispatcher;
    }

    /**
     * Sets the rewind increment in milliseconds.
     *
     * @param rewindMs The rewind increment in milliseconds. A non-positive value will cause the
     *                 rewind button to be disabled.
     */
    public void setRewindIncrementMs(int rewindMs) {
        this.rewindMs = rewindMs;
        updateNavigation();
    }

    /**
     * Sets the fast forward increment in milliseconds.
     *
     * @param fastForwardMs The fast forward increment in milliseconds. A non-positive value will
     *                      cause the fast forward button to be disabled.
     */
    public void setFastForwardIncrementMs(int fastForwardMs) {
        this.fastForwardMs = fastForwardMs;
        updateNavigation();
    }

    /**
     * Returns the playback controls timeout. The playback controls are automatically hidden after
     * this duration of time has elapsed without user input.
     *
     * @return The duration in milliseconds. A non-positive value indicates that the controls will
     * remain visible indefinitely.
     */
    public int getShowTimeoutMs() {
        return showTimeoutMs;
    }

    /**
     * Sets the playback controls timeout. The playback controls are automatically hidden after this
     * duration of time has elapsed without user input.
     *
     * @param showTimeoutMs The duration in milliseconds. A non-positive value will cause the controls
     *                      to remain visible indefinitely.
     */
    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
    }

    /**
     * Shows the playback controls. If {@link #getShowTimeoutMs()} is positive then the controls will
     * be automatically hidden after this duration of time has elapsed without user input.
     */
    public void show() {
        if (!isVisible()) {
            setVisibility(VISIBLE);
            if (visibilityListener != null) {
                visibilityListener.onVisibilityChange(getVisibility());
            }
            updateAll();
            requestPlayPauseFocus();
        }
        // Call hideAfterTimeout even if already visible to reset the timeout.
        hideAfterTimeout();
    }

    /**
     * Hides the controller.
     */
    public void hide() {
//        if (isVisible()) {
//            setVisibility(GONE);
//            if (visibilityListener != null) {
//                visibilityListener.onVisibilityChange(getVisibility());
//            }
//            removeCallbacks(updateProgressAction);
//            removeCallbacks(hideAction);
//            hideAtMs = C.TIME_UNSET;
//        }
    }

    /**
     * Returns whether the controller is currently visible.
     */
    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    private void hideAfterTimeout() {
        removeCallbacks(hideAction);
        if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs;
            if (isAttachedToWindow) {
                postDelayed(hideAction, showTimeoutMs);
            }
        } else {
            hideAtMs = C.TIME_UNSET;
        }
    }

    public void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateProgress();
    }

    private void updatePlayPauseButton() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        boolean requestPlayPauseFocus = false;
        boolean playing = player != null && player.getPlayWhenReady();
        if (playButton != null) {
            requestPlayPauseFocus |= playing && playButton.isFocused();
            Log.d("tessst", "playButton==playing" + playing);
            playButton.setVisibility(playing ? View.GONE : View.VISIBLE);
//            if (duration == 0) {
//                setButtonEnabled(false, playButton);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    animator.pause();
//                }
//            } else if (duration > 0) {
//                setButtonEnabled(true, playButton);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    if (animator.isPaused()) {
//                        animator.resume();
//                    }
//                }
//            }
        }
        if (pauseButton != null) {
            requestPlayPauseFocus |= !playing && pauseButton.isFocused();
            Log.d("tessst", "pauseButton==playing" + !playing);
            pauseButton.setVisibility(!playing ? View.GONE : View.VISIBLE);
        }
        if (requestPlayPauseFocus) {
            requestPlayPauseFocus();
        }
    }

    private void updateNavigation() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        Timeline timeline = player != null ? player.getCurrentTimeline() : null;
        boolean haveNonEmptyTimeline = timeline != null && !timeline.isEmpty();
        boolean isSeekable = false;
        boolean enablePrevious = false;
        boolean enableNext = false;
        if (haveNonEmptyTimeline) {
            int windowIndex = player.getCurrentWindowIndex();
            timeline.getWindow(windowIndex, window);
            isSeekable = window.isSeekable;
            enablePrevious = windowIndex > 0 || isSeekable || !window.isDynamic;
            enableNext = (windowIndex < timeline.getWindowCount() - 1) || window.isDynamic;
            if (timeline.getPeriod(player.getCurrentPeriodIndex(), period).isAd) {
                // Always hide player controls during ads.
                hide();
            }
        }
        setButtonEnabled(enablePrevious, previousButton);
        setButtonEnabled(enableNext, nextButton);
        setButtonEnabled(fastForwardMs > 0 && isSeekable, fastForwardButton);
        setButtonEnabled(rewindMs > 0 && isSeekable, rewindButton);
        if (timeBar != null) {
            timeBar.setEnabled(isSeekable);
        }
    }

    private void updateTimeBarMode() {
        if (player == null) {
            return;
        }
        multiWindowTimeBar = showMultiWindowTimeBar
                && canShowMultiWindowTimeBar(player.getCurrentTimeline(), period);
    }

    private void updateProgress() {

        if (!isVisible() || !isAttachedToWindow) {
            //Log.d("update","updateProgress()"+"~~~~~");
            return;
        }
        //Log.d("update","updateProgress()走到了"+"~~~~~");

        long position = 0;
        long bufferedPosition = 0;
        duration = 0;
        if (player != null) {
            if (multiWindowTimeBar) {
                Timeline timeline = player.getCurrentTimeline();
                int windowCount = timeline.getWindowCount();
                int periodIndex = player.getCurrentPeriodIndex();
                long positionUs = 0;
                long bufferedPositionUs = 0;
                long durationUs = 0;
                boolean isInAdBreak = false;
                boolean isPlayingAd = false;
                int adBreakCount = 0;
                for (int i = 0; i < windowCount; i++) {
                    timeline.getWindow(i, window);
                    for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                        if (timeline.getPeriod(j, period).isAd) {
                            isPlayingAd |= j == periodIndex;
                            if (!isInAdBreak) {
                                isInAdBreak = true;
                                if (adBreakCount == adBreakTimesMs.length) {
                                    adBreakTimesMs = Arrays.copyOf(adBreakTimesMs,
                                            adBreakTimesMs.length == 0 ? 1 : adBreakTimesMs.length * 2);
                                }
                                adBreakTimesMs[adBreakCount++] = C.usToMs(durationUs);
                            }
                        } else {
                            isInAdBreak = false;
                            long periodDurationUs = period.getDurationUs();
                            Assertions.checkState(periodDurationUs != C.TIME_UNSET);
                            long periodDurationInWindowUs = periodDurationUs;
                            if (j == window.firstPeriodIndex) {
                                periodDurationInWindowUs -= window.positionInFirstPeriodUs;
                            }
                            if (i < periodIndex) {
                                positionUs += periodDurationInWindowUs;
                                bufferedPositionUs += periodDurationInWindowUs;
                            }
                            durationUs += periodDurationInWindowUs;
                        }
                    }
                }
                position = C.usToMs(positionUs);
                bufferedPosition = C.usToMs(bufferedPositionUs);
                duration = C.usToMs(durationUs);
                if (!isPlayingAd) {
                    position += player.getCurrentPosition();
                    bufferedPosition += player.getBufferedPosition();
                }
                if (timeBar != null) {
                    timeBar.setAdBreakTimesMs(adBreakTimesMs, adBreakCount);
                }
            } else {
                position = player.getCurrentPosition();
                bufferedPosition = player.getBufferedPosition();
                duration = player.getDuration();
            }
        }

        if (duration == 0) {
            setButtonEnabled(false, playButton);
            playButton.setVisibility(VISIBLE);
            pauseButton.setVisibility(GONE);
        }

        if (durationView != null) {
            durationView.setText(Util.getStringForTime(formatBuilder, formatter, duration));
        }
        if (positionView != null && !scrubbing) {
            positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
        }
        if (timeBar != null) {
            timeBar.setPosition(position);
            timeBar.setBufferedPosition(bufferedPosition);
            timeBar.setDuration(duration);
        }

        // Cancel any pending updates and schedule a new one if necessary.
        removeCallbacks(updateProgressAction);
        int playbackState = player == null ? ExoPlayer.STATE_IDLE : player.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            Log.d("state", " 不懒不结束 " + playbackState);
            setButtonEnabled(true, playButton);
            long delayMs;
            if (player.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            postDelayed(updateProgressAction, delayMs);
            //startDisc();
        } else {
            Log.d("state", "  " + playbackState);
            if (playbackState == ExoPlayer.STATE_IDLE) {
                stopDisc();
                Log.d("state", "懒懒懒" + playbackState);
                playButton.setVisibility(VISIBLE);
                pauseButton.setVisibility(GONE);
                setButtonEnabled(false, playButton);
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                Log.d("tessst", "歌曲播完了");
                if (endedListener != null) {
                    endedListener.musicEnd();
                }
//                stopDisc();
//                playButton.setVisibility(VISIBLE);
//                pauseButton.setVisibility(GONE);
//                setButtonEnabled(true, playButton);
                player.seekToDefaultPosition();
            }
        }
    }

    private void requestPlayPauseFocus() {
        boolean playing = player != null && player.getPlayWhenReady();
        if (!playing && playButton != null) {
            playButton.requestFocus();
        } else if (playing && pauseButton != null) {
            pauseButton.requestFocus();
        }
    }

    private void setButtonEnabled(boolean enabled, View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        if (Util.SDK_INT >= 11) {
            setViewAlphaV11(view, enabled ? 1f : 0.3f);
            view.setVisibility(VISIBLE);
        } else {
            view.setVisibility(enabled ? VISIBLE : INVISIBLE);
        }
    }

    @TargetApi(11)
    private void setViewAlphaV11(View view, float alpha) {
        view.setAlpha(alpha);
    }

    private void previous() {
        Timeline timeline = player.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return;
        }
        int windowIndex = player.getCurrentWindowIndex();
        timeline.getWindow(windowIndex, window);
        if (windowIndex > 0 && (player.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                || (window.isDynamic && !window.isSeekable))) {
            seekTo(windowIndex - 1, C.TIME_UNSET);
        } else {
            seekTo(0);
        }
    }

    private void next() {
        Timeline timeline = player.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return;
        }
        int windowIndex = player.getCurrentWindowIndex();
        if (windowIndex < timeline.getWindowCount() - 1) {
            seekTo(windowIndex + 1, C.TIME_UNSET);
        } else if (timeline.getWindow(windowIndex, window, false).isDynamic) {
            seekTo(windowIndex, C.TIME_UNSET);
        }
    }

    private void rewind() {
        if (rewindMs <= 0) {
            return;
        }
        seekTo(Math.max(player.getCurrentPosition() - rewindMs, 0));
    }

    private void fastForward() {
        if (fastForwardMs <= 0) {
            return;
        }
        seekTo(Math.min(player.getCurrentPosition() + fastForwardMs, player.getDuration()));
    }

    private void seekTo(long positionMs) {
        seekTo(player.getCurrentWindowIndex(), positionMs);
    }

    private void seekTo(int windowIndex, long positionMs) {
        boolean dispatched = controlDispatcher.dispatchSeekTo(player, windowIndex, positionMs);
        if (!dispatched) {
            // The seek wasn't dispatched. If the progress bar was dragged by the user to perform the
            // seek then it'll now be in the wrong position. Trigger a progress update to snap it back.
            updateProgress();
        }
    }

    private void seekToTimebarPosition(long timebarPositionMs) {
        if (multiWindowTimeBar) {
            Timeline timeline = player.getCurrentTimeline();
            int windowCount = timeline.getWindowCount();
            long remainingMs = timebarPositionMs;
            for (int i = 0; i < windowCount; i++) {
                timeline.getWindow(i, window);
                for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                    if (!timeline.getPeriod(j, period).isAd) {
                        long periodDurationMs = period.getDurationMs();
                        if (periodDurationMs == C.TIME_UNSET) {
                            // Should never happen as canShowMultiWindowTimeBar is true.
                            throw new IllegalStateException();
                        }
                        if (j == window.firstPeriodIndex) {
                            periodDurationMs -= window.getPositionInFirstPeriodMs();
                        }
                        if (i == windowCount - 1 && j == window.lastPeriodIndex
                                && remainingMs >= periodDurationMs) {
                            // Seeking past the end of the last window should seek to the end of the timeline.
                            seekTo(i, window.getDurationMs());
                            return;
                        }
                        if (remainingMs < periodDurationMs) {
                            seekTo(i, period.getPositionInWindowMs() + remainingMs);
                            return;
                        }
                        remainingMs -= periodDurationMs;
                    }
                }
            }
        } else {
            seekTo(timebarPositionMs);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
        if (hideAtMs != C.TIME_UNSET) {
            long delayMs = hideAtMs - SystemClock.uptimeMillis();
            if (delayMs <= 0) {
                hide();
            } else {
                postDelayed(hideAction, delayMs);
            }
        }
        updateAll();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event);
        if (handled) {
            show();
        }
        return handled;
    }

    /**
     * Called to process media key events. Any {@link KeyEvent} can be passed but only media key
     * events will be handled.
     *
     * @param event A key event.
     * @return Whether the key event was handled.
     */
    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (player == null || !isHandledMediaKey(keyCode)) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    fastForward();
                    break;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    rewind();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    controlDispatcher.dispatchSetPlayWhenReady(player, !player.getPlayWhenReady());
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    controlDispatcher.dispatchSetPlayWhenReady(player, true);
                    //Toast.makeText(getContext(), "开始了", Toast.LENGTH_SHORT).show();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    controlDispatcher.dispatchSetPlayWhenReady(player, false);
                    //Toast.makeText(getContext(), "暂停了", Toast.LENGTH_SHORT).show();
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    next();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    previous();
                    break;
                default:
                    break;
            }
        }
        show();
        return true;
    }

    @SuppressLint("InlinedApi")
    private static boolean isHandledMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }

    /**
     * Returns whether the specified {@code timeline} can be shown on a multi-window time bar.
     *
     * @param timeline The {@link Timeline} to check.
     * @param period   A scratch {@link Timeline.Period} instance.
     * @return Whether the specified timeline can be shown on a multi-window time bar.
     */
    private static boolean canShowMultiWindowTimeBar(Timeline timeline, Timeline.Period period) {
        if (timeline.getWindowCount() > MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR) {
            return false;
        }
        int periodCount = timeline.getPeriodCount();
        for (int i = 0; i < periodCount; i++) {
            timeline.getPeriod(i, period);
            if (!period.isAd && period.durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }

    private final class ComponentListener implements ExoPlayer.EventListener, TimeBar.OnScrubListener,
            View.OnClickListener {

        @Override
        public void onScrubStart(TimeBar timeBar) {
            removeCallbacks(hideAction);
            scrubbing = true;
        }

        @Override
        public void onScrubMove(TimeBar timeBar, long position) {
            if (positionView != null) {
                positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
            }
        }

        @Override
        public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
            scrubbing = false;
            if (!canceled && player != null) {
                seekToTimebarPosition(position);
            }
            hideAfterTimeout();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            updatePlayPauseButton();
            updateProgress();
            //Log.d("tessst", "状态改变" + playbackState);
            if (playbackState == ExoPlayer.STATE_ENDED) {
                Log.d("tessst", "状态改变" + playbackState);
            }
        }

        @Override
        public void onPositionDiscontinuity() {
            updateNavigation();
            updateProgress();
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Do nothing.
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            updateNavigation();
            updateTimeBarMode();
            updateProgress();
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Do nothing.
        }

        @Override
        public void onTracksChanged(TrackGroupArray tracks, TrackSelectionArray selections) {
            // Do nothing.
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            // Do nothing.
        }

        @Override
        public void onClick(View view) {
            if (player != null) {
                if (nextButton == view) {
                    next();
                } else if (previousButton == view) {
                    previous();
                } else if (fastForwardButton == view) {
                    fastForward();
                } else if (rewindButton == view) {
                    rewind();
                } else if (playButton == view) {
                    controlDispatcher.dispatchSetPlayWhenReady(player, true);
                    //Toast.makeText(getContext(), "点了播放键", Toast.LENGTH_LONG).show();
                    startDisc();
                } else if (pauseButton == view) {
                    controlDispatcher.dispatchSetPlayWhenReady(player, false);
                    //Toast.makeText(getContext(), "点了暂停键", Toast.LENGTH_LONG).show();
                    stopDisc();
                }
            }
            hideAfterTimeout();
        }
    }


    private void startDisc() {
        if (animator != null) {
            if (isStarted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    animator.resume();
                } else {
                    animator.start();
                }
            } else {
                animator.start();
                isStarted = true;
            }
        }
    }

    private void stopDisc() {
        if (animator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                animator.pause();
            }
        }
    }

}