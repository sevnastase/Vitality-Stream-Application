package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;

public class PraxSpinStatusBarFragment extends AbstractPraxStatusBarFragment {
    private static final String TAG = PraxSpinStatusBarFragment.class.getSimpleName();

    private TextView statusbarDistance;
    private TextView statusbarTotalDistance;

    //SPEED
    private TextView speedIndicator;
    private ImageButton speedUpButton;
    private ImageButton speedDownButton;


    //SEEK BAR
    private ImageButton seekBarT1;
    private ImageButton seekBarT2;
    private ImageButton seekBarT3;
    private ImageButton seekBarT4;
    private ImageButton seekBarT5;
    private ImageButton seekBarT6;
    private ImageButton[] seekBarButtons;
    private MoviePart[] movieParts;
    private int finalFrame;
    private float position;
    private final Handler setupHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void initializeLayout(View view) {
        super.initializeLayout(view);

        statusbarDistance = view.findViewById(R.id.statusbar_distance_box_value);
        statusbarTotalDistance = view.findViewById(R.id.statusbar_distance_finish_box_value);

        //SPEED BUTTONS
        speedIndicator = view.findViewById(R.id.statusbar_speed_value);
        speedUpButton = view.findViewById(R.id.statusbar_speed_up_button);
        speedDownButton = view.findViewById(R.id.statusbar_speed_down_button);

        //SEEKBAR BUTTONS
        seekBarT1 = view.findViewById(R.id.statusbar_seekbar_t1);
        seekBarT2 = view.findViewById(R.id.statusbar_seekbar_t2);
        seekBarT3 = view.findViewById(R.id.statusbar_seekbar_t3);
        seekBarT4 = view.findViewById(R.id.statusbar_seekbar_t4);
        seekBarT5 = view.findViewById(R.id.statusbar_seekbar_t5);
        seekBarT6 = view.findViewById(R.id.statusbar_seekbar_t6);

        seekBarButtons = new ImageButton[6];
        seekBarButtons[0] = seekBarT1;
        seekBarButtons[1] = seekBarT2;
        seekBarButtons[2] = seekBarT3;
        seekBarButtons[3] = seekBarT4;
        seekBarButtons[4] = seekBarT5;
        seekBarButtons[5] = seekBarT6;

        // This is to let parent class know which boxes to hide/show when
        // movie is paused/resumed
        addUsedViews(new View[]{
                view.findViewById(R.id.statusbar_time_box),
                view.findViewById(R.id.statusbar_distance_box),
                view.findViewById(R.id.statusbar_distance_finish_box),
                view.findViewById(R.id.statusbar_toggle_movieparts_box),
                view.findViewById(R.id.statusbar_volume_buttons_box),
                view.findViewById(R.id.statusbar_speed_box)
        });

        if (startedFromMotolife) {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_rpm_box),
                    view.findViewById(R.id.statusbar_motolife_power_box),
                    view.findViewById(R.id.chinesport_logo_imageview),
                    view.findViewById(R.id.motolife_info_layout)
            });
        } else {
            addUsedViews(new LinearLayout[]{
                    view.findViewById(R.id.statusbar_stop_box)
            });
        }
    }

    @Override
    protected void setupFunctionality(View view) {
        super.setupFunctionality(view);

        //ROUTEPARTS
        routePartsLayout.setOnClickListener(v -> routePartsLayout.setVisibility(View.GONE));

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRoutePartsView.setLayoutManager(layoutManager);

        //SPEED
        speedUpButton.setOnClickListener(clickedView ->{
            videoPlayerViewModel.changeKmhBy(2);
        });
        speedDownButton.setOnClickListener(clickedView -> {
            videoPlayerViewModel.changeKmhBy(-2);
        });

        setupFocus();
    }

    @Override
    protected void onMqttMessageReceived(Intent intent) {
        super.onMqttMessageReceived(intent);

        if ("com.videostreamtest.ACTION_JUMP".equals(intent.getAction())) {
            String jumpCommand = intent.getStringExtra("routepartNr");
            Log.d(TAG, "MQTT broadcast was jump: " + jumpCommand);
            if (jumpCommand == null || jumpCommand.length() != 1) {
                return;
            }

            int routepartNr = Integer.parseInt(jumpCommand);
            if (routepartNr >= 1 && routepartNr <= 6) {
                jumpToRoutepart(--routepartNr);
            }
        } else if ("com.videostreamtest.ACTION_TOGGLE_ROUTEPARTS".equals(intent.getAction())) {
            // Explanation: if the MQTT command was "ToggleRouteparts1", value is true
            // See MQTTService#handleDataUpdate for clarification
            boolean value = intent.getBooleanExtra("toggleValue", false);
            Log.d(TAG, "show? " + value);
            toggleMoviePartsVisibility(value);
        }
    }

    @Override
    public void onResume() {
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_JUMP");
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_TOGGLE_ROUTEPARTS");
        super.onResume();
    }

    @Override
    public void onDestroy() {
        setupHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void setupFocus() {
        addRedBorderOnFocus(new View[]{toggleRoutePartsButton, speedUpButton, speedDownButton, stopButton});

        movieProgressBar.setFocusable(false);
        movieProgressBar.setFocusableInTouchMode(false);
        for (ImageButton tButton : seekBarButtons) {
            tButton.setFocusable(false);
            tButton.setFocusableInTouchMode(false);
        }

        if (startedFromMotolife) {

        } else {
            stopButton.setNextFocusRightId(R.id.statusbar_toggle_movieparts_button);
            toggleRoutePartsButton.setNextFocusLeftId(R.id.statusbar_stop_button);
        }
    }

    @Override
    protected void setupVisibilities(View view) {
        super.setupVisibilities(view);

        for (ImageButton tButton : seekBarButtons) {
            tButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void useVideoPlayerViewModel(View view) {
        super.useVideoPlayerViewModel(view);

        //Movie object related
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (selectedMovie!= null) {
                setupSeekbarButtonsFunctionality();
                setupSeekbarButtonsLayout();

                //LOAD ROUTEPARTS IF AVAILABLE
                videoPlayerViewModel.getRoutePartsOfMovieId(selectedMovie.getId()).observe(getViewLifecycleOwner(), routeparts -> {
                    if (routeparts != null && routeparts.size()>0) {
                        routePartsAdapter = new RoutePartsAdapter(routeparts, isLocalPlay, videoPlayerViewModel, getViewLifecycleOwner());
                        statusbarRoutePartsView.setAdapter(routePartsAdapter);
                    }
                });
            }
        });

        videoPlayerViewModel.getKmhData().observe(getViewLifecycleOwner(), kmhData -> {
            if (kmhData != null) {
                speedIndicator.setText(""+kmhData+" kmh");
            }
        });

        videoPlayerViewModel.getCurrentMetersDone().observe(getViewLifecycleOwner(), updatedCurrentMetersDone -> {
            if (updatedCurrentMetersDone != null) {
                statusbarDistance.setText(String.format(getString(R.string.video_screen_distance), updatedCurrentMetersDone));
            }
        });

        videoPlayerViewModel.getMetersToGo().observe(getViewLifecycleOwner(), updatedMetersToGo -> {
            if (updatedMetersToGo != null) {
                statusbarTotalDistance.setText(String.format(getString(R.string.video_screen_total_distance), updatedMetersToGo));
            }
        });

        getMovieParts(videoPlayerViewModel);
        getFinalFrame(videoPlayerViewModel);
    }

    private void setupSeekbarButtonsFunctionality() {
        int h = 0;
        for (ImageButton tButton : seekBarButtons) {
            final int i = h; // variable used in lambda expressions needs to be final
            tButton.setOnClickListener(v -> {
                tButton.requestFocus();
                jumpToRoutepart(i);
            });

            h++;
        }
    }

    private void jumpToRoutepart(int routepartNr) {
        Log.d(TAG, "Jumping to routepart " + routepartNr);

        if (AccountHelper.isLocalPlay(getContext())) {
            // WAS VIDEOPLAYERACTIVITY.GETINSTANCE IN ALL 6 OF THEM
            VideoplayerActivity.getInstance().goToFrameNumber(movieParts[routepartNr].getFrameNumber().intValue());
        } else {
            VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[routepartNr].getFrameNumber().intValue());
        }
        // Log.d(TAG, "movieParts[0] frame as int = " + movieParts[0].getFrameNumber().intValue());
        if (routePartsLayout.getVisibility() == View.VISIBLE) {
            routePartsLayout.setVisibility(View.GONE);
        }
        try {
            toggleRoutePartsLayoutTimer.removeCallbacksAndMessages(null);
        } catch (NullPointerException ignored) {}

        videoPlayerViewModel.resetDistance(movieParts[routepartNr], selectedMovie);
    }

    private void setupSeekbarButtonsLayout() {
        setupHandler.postDelayed(() -> {
                    /*Log.d(TAG, "finalFrame = " + finalFrame);
                    Log.d(TAG, "seekBarWidth = " + seekBarWidth);
                    Log.d(TAG,"progressBar.getWidth() = " + movieProgressBar.getWidth());
                    Log.d(TAG, "progressBar.getPaddingStart()" + movieProgressBar.getPaddingStart());
                    Log.d(TAG, "progressBar.getPaddingEnd()" + movieProgressBar.getPaddingEnd());*/
            int seekBarWidth = movieProgressBar.getWidth() - movieProgressBar.getPaddingStart() - movieProgressBar.getPaddingEnd();
            if (movieParts != null) {
                for (int i = 0; i < movieParts.length; i++) {
                    int frameNumber = movieParts[i].getFrameNumber().intValue();
                    // Log.d(TAG, "movieParts[" + i + "] frameNumber = " + frameNumber);
                    position = ((float) frameNumber / finalFrame) * seekBarWidth;
                    // Log.d(TAG, "position of movieParts[" + i + "] = " + position);
                    if (seekBarButtons[i] != null) {
                        seekBarButtons[i].setX(movieProgressBar.getX() + movieProgressBar.getPaddingStart() + position);
                        // Log.d(TAG, "seekBarButtons[" + i + "] position = " + progressBar.getX() + progressBar.getPaddingStart() + position);
                    }
                }
            }
        }, 5500);
    }

    private MoviePart[] getMovieParts(VideoPlayerViewModel videoPlayerViewModel) {
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (selectedMovie!= null) {
                videoPlayerViewModel.getRoutePartsOfMovieId(selectedMovie.getId()).observe(getViewLifecycleOwner(), routeparts -> {
                    if (routeparts.size()>0) {
                        movieParts = new MoviePart[routeparts.size()];
                        for (int partIndex = 0; partIndex<routeparts.size();partIndex++) {
                            movieParts[partIndex] = MoviePart.fromRoutepartEntity(routeparts.get(partIndex));
                        }
                    }
                });
            }
        });
        return movieParts;
    }

    private int getFinalFrame(VideoPlayerViewModel videoPlayerViewModel) {
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds -> {
                finalFrame = (int) ((movieTotalDurationSeconds / 1000) * selectedMovie.getRecordedFps().intValue());
            });
        });
        return finalFrame;
    }
}
