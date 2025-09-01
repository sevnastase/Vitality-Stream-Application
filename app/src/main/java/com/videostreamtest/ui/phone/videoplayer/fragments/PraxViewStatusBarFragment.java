package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

public class PraxViewStatusBarFragment extends AbstractPraxStatusBarFragment {
    private static final String TAG = PraxViewStatusBarFragment.class.getSimpleName();

    private boolean jumpBlocked = false;
    private final int DEFAULT_JUMP_COOLDOWN = 1000; // milliseconds
    private Handler blockJumpHandler = new Handler();

    @Override
    protected void initializeLayout(View view) {
        super.initializeLayout(view);

        addUsedViews(new View[]{
                view.findViewById(R.id.statusbar_volume_buttons_box)
        });

        if (startedFromMotolife) {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_motolife_power_box),
                    view.findViewById(R.id.chinesport_logo_imageview),
                    view.findViewById(R.id.motolife_info_layout),
                    view.findViewById(R.id.statusbar_rpm_box)
            });
        } else {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_stop_box)
            });
        }

        movieProgressBar.setFocusable(true);
        movieProgressBar.setFocusableInTouchMode(true);
        view.findViewById(R.id.statusbar_volume_down_button).setNextFocusDownId(movieProgressBar.getId());
        movieProgressBar.setNextFocusUpId(R.id.statusbar_volume_down_button);
    }

    private void setupFocus() {
        addRedBorderOnFocus(new View[]{stopButton});
        if (startedFromMotolife) {

        } else {
            stopButton.setNextFocusRightId(stopButton.getId());
            stopButton.setNextFocusLeftId(volumeUp.getId());
            volumeUp.setNextFocusRightId(stopButton.getId());
            volumeDown.setNextFocusRightId(stopButton.getId());
        }
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

        setupFocus();
    }

    private void blockJump() {
        jumpBlocked = true;
        toggleStatusbarVisibility();
        toggleStatusbarButton.requestFocus();
        toggleStatusbarButton.setVisibility(View.GONE);

        blockJumpHandler.postDelayed(() -> {
            toggleStatusbarVisibility();
            movieProgressBar.requestFocus();
            toggleStatusbarButton.setVisibility(View.VISIBLE);
            jumpBlocked = false;
        }, DEFAULT_JUMP_COOLDOWN);
    }

    @Override
    protected void useVideoPlayerViewModel(View view) {
        super.useVideoPlayerViewModel(view);
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
    public void onDestroy() {
        super.onDestroy();

        blockJumpHandler.removeCallbacksAndMessages(null);
    }
}
