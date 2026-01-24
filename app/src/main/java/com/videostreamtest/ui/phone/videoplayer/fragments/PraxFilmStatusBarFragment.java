package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.videostreamtest.R;
import com.videostreamtest.constants.TrainingConstants;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

import java.util.Locale;

public class PraxFilmStatusBarFragment extends AbstractPraxStatusBarFragment {
    private static final String TAG = PraxFilmStatusBarFragment.class.getSimpleName();

    private TextView statusbarRpmValue;
    private boolean jumpBlocked = false;
    private final int DEFAULT_JUMP_COOLDOWN = 1000; // milliseconds
    private final Handler blockJumpHandler = new Handler(Looper.getMainLooper());
    private final Runnable unblockJumpRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && getView() != null) {
                toggleStatusbarVisibility();
                movieProgressBar.requestFocus();
                toggleStatusbarButton.setVisibility(View.VISIBLE);
            }
            jumpBlocked = false;
        }
    };

    @Override
    protected void initializeLayout(View view) {
        super.initializeLayout(view);

        statusbarRpmValue = view.findViewById(R.id.statusbar_rpm_value);

        addUsedViews(new View[]{
                view.findViewById(R.id.statusbar_rpm_box),
                view.findViewById(R.id.statusbar_volume_buttons_box)
        });

        if (startedFromMotolife) {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_motolife_power_box),
                    view.findViewById(R.id.chinesport_logo_imageview),
                    view.findViewById(R.id.motolife_info_layout)
            });
        }

        movieProgressBar.setFocusable(true);
        movieProgressBar.setFocusableInTouchMode(true);
        view.findViewById(R.id.statusbar_volume_down_button).setNextFocusDownId(movieProgressBar.getId());
        movieProgressBar.setNextFocusUpId(R.id.statusbar_volume_down_button);
    }

    @Override
    protected void setupFunctionality(View view) {
        super.setupFunctionality(view);

        movieProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int newProgress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    newProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onDragProgressbar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onDragProgressbar = false;
                seek(newProgress);
            }
        });

        movieProgressBar.setOnKeyListener((v, keyCode, event) -> {
            if (movieProgressBar.hasFocus() && event.getAction() == KeyEvent.ACTION_DOWN) {
                int currentProgressMovie = movieProgressBar.getProgress();
                int movieLength = movieProgressBar.getMax();
                int stepSize = movieProgressBar.getKeyProgressIncrement();
                int newProgressMovie;

                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (jumpBlocked || currentProgressMovie + stepSize >= movieLength - stepSize) {
                            return false;
                        }
                        blockJump();

                        newProgressMovie = currentProgressMovie + stepSize;
                        movieProgressBar.setProgress(newProgressMovie);
                        seek(newProgressMovie);

                        return true;

                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (jumpBlocked) {
                            return false;
                        }
                        blockJump();

                        newProgressMovie = Math.max(currentProgressMovie - stepSize, 0);
                        movieProgressBar.setProgress(newProgressMovie);
                        seek(newProgressMovie);

                        return true;
                }
            }
            return false;
        });
    }

    private void blockJump() {
        jumpBlocked = true;
        toggleStatusbarVisibility();
        toggleStatusbarButton.requestFocus();
        toggleStatusbarButton.setVisibility(View.GONE);

        blockJumpHandler.postDelayed(unblockJumpRunnable, DEFAULT_JUMP_COOLDOWN);
    }

    @Override
    protected void useVideoPlayerViewModel(View view) {
        super.useVideoPlayerViewModel(view);

        //RPM data related
        videoPlayerViewModel.getRpmData().observe(getViewLifecycleOwner(), rpmData -> {
            statusbarRpmValue.setText(String.format(Locale.GERMANY, getString(R.string.video_screen_rpm), rpmData));

            if (rpmData == TrainingConstants.MAX_RPM) {
                statusbarRpmValue.setText(String.format(Locale.GERMANY, "%d+", rpmData));
            }

            Integer seconds = videoPlayerViewModel.getMovieElapsedSeconds().getValue();
            int secondsSpentInMovie = seconds == null ? 0 : seconds;
            if (secondsSpentInMovie <= TrainingConstants.Beginning.CLAMP_MIN_RPM_UNTIL_SECONDS
                    && rpmData == TrainingConstants.Beginning.MIN_RPM) {
                statusbarRpmValue.setTextColor(Color.GRAY);
            }

            if (secondsSpentInMovie > TrainingConstants.Beginning.CLAMP_MIN_RPM_UNTIL_SECONDS
                    || rpmData > TrainingConstants.Beginning.MIN_RPM) {
                statusbarRpmValue.setTextColor(Color.WHITE);
            }
        });
    }

    private void seek(final int newProgress) {
        int framesPerSecond = selectedMovie.getRecordedFps();
        int frameNumber = (newProgress/1000) * framesPerSecond;
        try {
            VideoplayerExoActivity.getInstance().goToFrameNumber(frameNumber);
        } catch (NullPointerException e) {
            VideoplayerActivity.getInstance().goToFrameNumber(frameNumber);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        blockJumpHandler.removeCallbacksAndMessages(null);
    }
}
