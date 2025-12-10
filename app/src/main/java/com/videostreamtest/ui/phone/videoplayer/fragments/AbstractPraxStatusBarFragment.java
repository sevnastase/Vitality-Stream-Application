package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.alerts.PauseFragment;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is the template for classes that are responsible for the proper functioning
 * of the bottom, black, semi-transparent bars that contain information and buttons.
 * <p>
 * This class contains (or aims to contain) all mutual behaviour between all current and future
 * products from Praxtour.
 */
public abstract class AbstractPraxStatusBarFragment extends Fragment {
    private static final String TAG = AbstractPraxStatusBarFragment.class.getSimpleName();

    //STATEFUL ELEMENTS
    protected boolean onDragProgressbar = false;
    protected boolean isLocalPlay = false;

    protected VideoPlayerViewModel videoPlayerViewModel;
    protected Movie selectedMovie;
    protected SeekBar movieProgressBar;
    private TextView statusbarMovieTitle;

    protected ImageButton toggleStatusbarButton;
    private View statusbar;

    protected Chronometer stopwatchCurrentRide;

    protected ImageButton toggleRoutePartsButton;

    //VOLUME
    private TextView volumeIndicator;
    protected ImageButton volumeUp;
    protected ImageButton volumeDown;
    private int volumeLevelBeforePause;

    // RPM (TODO: REWORK SO PRAXFIT/FILM HAS MOTOLIFE CONDITIONAL INTEGRATION)
    protected TextView rpmIndicator;

    //MOVIE PARTS
    protected RecyclerView statusbarRoutePartsView;
    protected IntentFilter localBroadcastIntentFilter = new IntentFilter();
    protected BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }

            switch(intent.getAction()) {
                case "hideRoutepartsLayout":
                    toggleMoviePartsVisibility(false);
                    break;
            }
        }
    };
    protected LinearLayout routePartsLayout; // TODO: WHEN ABSTRACTING SPIN AND FIT TOGETHER, MOVE THIS TO THEIR MUTUAL CLASS
    protected RoutePartsAdapter routePartsAdapter;
    protected List<Routepart> routeParts;
    protected Handler toggleRoutePartsLayoutTimer;

    private ArrayList<View> usedStatusBarBoxes;

    protected ImageButton stopButton;

    //FOR CHINESPORT
    protected TextView motolifePower;
    protected TextView motolifeMode;
    protected TextView motolifeDirection;
    protected TextView motolifeTime;
    protected boolean startedFromMotolife;
    protected IntentFilter mqttMessageFilter = new IntentFilter();

    protected BroadcastReceiver mqttMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onMqttMessageReceived(intent);
        }
    };

    protected void onMqttMessageReceived(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "MQTT broadcast received: " + intent.getAction());

        switch (intent.getAction()) {
            case "com.videostreamtest.ACTION_PAUSE_FILM":
                videoPlayerViewModel.setPlayerPaused(true);
                Log.d(TAG, "Film Paused");
                showPausedDialog();
                break;
            case "com.videostreamtest.ACTION_RESUME_FILM":
                Log.d(TAG, "Resume selected.");
                hidePausedDialog();
                videoPlayerViewModel.setPlayerPaused(false);
                break;
            case "com.videostreamtest.ACTION_FINISH_FILM":
                finishFilm();
                break;
            case "com.videostreamtest.MQTT_DATA_UPDATE":
                displayData(intent);
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_abstract_statusbar, container, false);

        Bundle arguments = getArguments();
        if (arguments != null) {
            isLocalPlay = arguments.getBoolean("localPlay");
            startedFromMotolife = arguments.getBoolean("startedFromMotolife");
        }

        initializeLayout(view);
        setupVisibilities(view);
        setupFunctionality(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        useVideoPlayerViewModel(view);
    }

    /**
     * All inheriting classes should override this method to initialize their custom
     * layout elements.
     * All views need to be declared in {@link R.layout#fragment_abstract_statusbar}.
     */
    protected void initializeLayout(View view) {
        statusbar = view.findViewById(R.id.statusbar);
        toggleStatusbarButton = view.findViewById(R.id.toggle_statusbar_button);

        toggleStatusbarButton.setOnFocusChangeListener((view2, hasFocus) -> repaintToggleStatusbarButton(hasFocus));

        toggleRoutePartsButton = view.findViewById(R.id.statusbar_toggle_movieparts_button);

        routePartsLayout = view.findViewById(R.id.statusbar_routeparts_layout);
        statusbarRoutePartsView = view.findViewById(R.id.statusbar_routeparts_recyclerview);

        routeParts = new ArrayList<>();
        routePartsAdapter = new RoutePartsAdapter(routeParts, isLocalPlay, videoPlayerViewModel, getViewLifecycleOwner());

        statusbarMovieTitle = view.findViewById(R.id.statusbar_movie_title);
        movieProgressBar = view.findViewById(R.id.statusbar_seekbar);

        stopButton = view.findViewById(R.id.statusbar_stop_button);

        //TIME
        stopwatchCurrentRide = view.findViewById(R.id.statusbar_time_value_chrono);

        // VOLUME
        volumeIndicator = view.findViewById(R.id.statusbar_volume_value);
        volumeUp = view.findViewById(R.id.statusbar_volume_up_button);
        volumeDown = view.findViewById(R.id.statusbar_volume_down_button);

        rpmIndicator = view.findViewById(R.id.statusbar_rpm_value);

        // CHINESPORT
        motolifePower = view.findViewById(R.id.statusbar_motolife_power_value);
        motolifeMode = view.findViewById(R.id.statusbar_motolife_mode_value);
        motolifeDirection = view.findViewById(R.id.statusbar_motolife_direction_value);
        motolifeTime = view.findViewById(R.id.statusbar_motolife_time_value);

        usedStatusBarBoxes = new ArrayList<>();
    }

    /**
     * Inheriting classes *must* add their used boxes through this method.
     * Be careful that the parent LinearLayout has to be added, not the
     * field holding the actual value. This is so that when the user pauses
     * and resumes the film, the correct boxes (dis)appear.
     */
    protected void addUsedViews(View[] statusBarBoxes) {
        if (statusBarBoxes == null) {
            return;
        }
        for (View box : statusBarBoxes) {
            usedStatusBarBoxes.add(box);
        }
    }

    /**
     * Any inheriting classes should declare all custom UI functionalities using this method.
     * <p>
     * For code quality purposes, all views should be declared before by
     * overriding {@link this#initializeLayout}.
     */
    protected void setupFunctionality(View view) {
        addRedBorderOnFocus(new View[]{volumeUp, volumeDown});

        stopwatchCurrentRide.setFormat(getString(R.string.videoplayer_chronometer_message));
        stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());

        volumeUp.setOnClickListener(clickedView -> {
            videoPlayerViewModel.changeVolumeLevelBy(10);
        });
        volumeDown.setOnClickListener(clickedView -> {
            videoPlayerViewModel.changeVolumeLevelBy(-10);
        });

        stopButton.setOnClickListener(clickedView -> {
            ((Activity) view.getContext()).finish();
        });

        toggleStatusbarButton.setOnClickListener(clickedView -> {
            toggleStatusbarVisibility();
        });

        toggleRoutePartsButton.setOnClickListener(clickedView -> {
            toggleMoviePartsVisibility();
        });

        toggleRoutePartsLayoutTimer = new Handler(Looper.getMainLooper());
    }

    protected void setupVisibilities(View view) {
        for (View box : usedStatusBarBoxes) {
            box.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "startedFromMotoLife: " + startedFromMotolife);
        if (startedFromMotolife) {
            motolifeTime.setVisibility(View.VISIBLE);
            stopwatchCurrentRide.setVisibility(View.GONE);
        } else {
            motolifeTime.setVisibility(View.GONE);
            stopwatchCurrentRide.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This method is responsible to handle any and all VideoPlayerViewModel related tasks.
     * <p>
     * Subclasses must use this method to customize {@link this#videoPlayerViewModel}.
     */
    protected void useVideoPlayerViewModel(View view) {
        videoPlayerViewModel = new ViewModelProvider(requireActivity()).get(VideoPlayerViewModel.class);

        selectedMovie = videoPlayerViewModel.getSelectedMovie().getValue();
        if (selectedMovie != null) {
            statusbarMovieTitle.setText(selectedMovie.getMovieTitle());
        } else {
            Toast.makeText(getContext(), "Contact Greg (Movie Title)", Toast.LENGTH_SHORT).show();
        }

        //PLAYER TIME AND DISTANCE related
        videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds ->{
            if (movieTotalDurationSeconds!=null) {
                videoPlayerViewModel.getMovieSpendDurationSeconds().observe(getViewLifecycleOwner(), movieSpendDurationSeconds -> {
                    if (movieSpendDurationSeconds!=null && !onDragProgressbar) {
                        movieProgressBar.setMax(movieTotalDurationSeconds.intValue());
                        movieProgressBar.setProgress(movieSpendDurationSeconds.intValue());
                    }
                });
                if (movieTotalDurationSeconds.intValue() > 6 && routePartsAdapter.getItemCount()<1) {
                    Long startPart = 0L;
                    long partLength = movieTotalDurationSeconds / 6;

                    for (int routepartindex = 0; routepartindex < 6; routepartindex++) {
                        Routepart routepart = new Routepart();
                        if (routepartindex == 0) {
                            routepart.setMovieId(selectedMovie.getId().intValue());
                            routepart.setRoutepartId(routepartindex);
                            routepart.setMoviePartName(""+routepartindex);
                            routepart.setMoviePartFrameNumber(0);
                            routepart.setMoviePartImagePath("nummer_"+(routepartindex+1)+"_wit.png");
                        } else {
                            routepart.setMovieId(selectedMovie.getId().intValue());
                            routepart.setRoutepartId(routepartindex);
                            routepart.setMoviePartName(""+routepartindex);
                            startPart += partLength;
                            routepart.setMoviePartFrameNumber((startPart.intValue()/1000)*selectedMovie.getRecordedFps().intValue());
                            routepart.setMoviePartImagePath("nummer_"+(routepartindex+1)+"_wit.png");
                        }
                        routeParts.add(routepart);
                    }
//                    routePartsAdapter.setRouteparts(routeParts);
                    routePartsAdapter.notifyDataSetChanged();
                    statusbarRoutePartsView.setAdapter(routePartsAdapter);
                }
            }
        });

        // CONSTANTLY UPDATE VOLUME
        videoPlayerViewModel.getVolumeLevel().observe(getViewLifecycleOwner(), volumeLevel -> {
            if (volumeLevel != null) {
                volumeIndicator.setText(String.valueOf(volumeLevel));
            }
        });
        final Boolean[] lastPausedState = {null};
        //ROUTE IS PAUSED STATUS BUT VIEW IS STILL VISIBLE
        videoPlayerViewModel.getPlayerPaused().observe(getViewLifecycleOwner(), isPaused -> {
            if (lastPausedState[0] == null || !lastPausedState[0].equals(isPaused)) {
                lastPausedState[0] = isPaused;
                if (isPaused) {
                    pauseFilm();
                } else {
                    resumeFilm();
                }
            }

            if (!startedFromMotolife) {
                if (isPaused) {
                    stopwatchCurrentRide.stop();
                } else {
                    stopwatchCurrentRide.start();
                }
            }
        });

        //RESET STOPWATCH TO ZERO
        videoPlayerViewModel.getResetChronometer().observe(getViewLifecycleOwner(), resetChronometer -> {
            if (!startedFromMotolife) {
                if (resetChronometer) {
                    stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());
                    videoPlayerViewModel.setResetChronometer(false);
                }
            }
        });
    }

    /**
     * Any Button or ImageButton passed to this method will become focusable and
     * will have a red border upon having focus.
     * If a View of any other class is passed, the method returns without modifying it.
     */
    protected void addRedBorderOnFocus(View view) {
        if (!(view instanceof Button || view instanceof ImageButton)) {
            return;
        }

        view.setOnFocusChangeListener((itemView, hasFocus) -> {
            if (hasFocus) {
                final Drawable border = AppCompatResources.getDrawable(getContext(), R.drawable.imagebutton_red_border);
                view.setBackground(border);
            } else {
                view.setBackground(null);
            }
        });

        view.setFocusable(true);
    }

    /**
     * The contract of {@link this#addRedBorderOnFocus(View)} applies to all View objects in
     * {@code views}.
     */
    protected void addRedBorderOnFocus(View[] views) {
        if (views == null) {
            return;
        }

        for (View view : views) {
            addRedBorderOnFocus(view);
        }
    }

    protected boolean isTouchScreen() {
        return getView().getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    protected void toggleMoviePartsVisibility() {
        if (routePartsLayout.getVisibility() == View.GONE) {
            Runnable closeMoviePartsLayout = () -> routePartsLayout.setVisibility(View.GONE);
            //Redirect to login activity if timer exceeds 5 seconds
            toggleRoutePartsLayoutTimer.postDelayed( closeMoviePartsLayout, 20*1000 );

            routePartsLayout.setVisibility(View.VISIBLE);
            if (routePartsLayout.getChildCount()>0) {
                routePartsLayout.getChildAt(0).requestFocus();
                toggleRoutePartsButton.setNextFocusUpId(routePartsLayout.getId());
            }
        } else {
            toggleRoutePartsLayoutTimer.removeCallbacksAndMessages(null);
            routePartsLayout.setVisibility(View.GONE);
            toggleRoutePartsButton.setNextFocusUpId(toggleStatusbarButton.getId());
        }
    }

    protected void toggleMoviePartsVisibility(boolean value) {
        toggleRoutePartsLayoutTimer.removeCallbacksAndMessages(null);
        if (value) {
            routePartsLayout.setVisibility(View.VISIBLE);
            if (routePartsLayout.getChildCount()>0) {
                routePartsLayout.getChildAt(0).requestFocus();
            }
        } else {
            routePartsLayout.setVisibility(View.GONE);
        }
    }

    private void displayData(Intent intent) {
        ArrayList<String> motoLifeData = intent.getStringArrayListExtra("motoLifeData");

        if (motoLifeData == null) {
            return;
        }

        try {
            // fetch before to avoid problems when
            // concatenating strings in setText() call
            String speed = motoLifeData.get(0);
            String power = motoLifeData.get(1) + " W";
            String mode = "Mode: " + motoLifeData.get(2);
            String direction = "Direction: " + motoLifeData.get(3);
            String time = motoLifeData.get(4);

            rpmIndicator.setText(speed);
            motolifePower.setText(power);
            motolifeMode.setText(mode);
            motolifeDirection.setText(direction);
            motolifeTime.setText(time);
        } catch (IndexOutOfBoundsException ignored) {}
    }

    private void finishFilm() {
        Intent intent = new Intent("videoplayer_finish_film");
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }
    @Override
    public void onResume() {
        super.onResume();
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_PAUSE_FILM");
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_RESUME_FILM");
        mqttMessageFilter.addAction("com.videostreamtest.MQTT_DATA_UPDATE");
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_FINISH_FILM");

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mqttMessageReceiver, mqttMessageFilter);

        localBroadcastIntentFilter.addAction("hideRoutepartsLayout");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(localBroadcastReceiver, localBroadcastIntentFilter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mqttMessageReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(localBroadcastReceiver);
        super.onPause();
    }


    private void pauseFilm() {
        if (VideoplayerActivity.getInstance() != null && VideoplayerActivity.getInstance().isRouteFinished()) {
            return;
        }
        if (VideoplayerExoActivity.getInstance() != null && VideoplayerExoActivity.getInstance().isRouteFinished()) {
            return;
        }
        videoPlayerViewModel = new ViewModelProvider(requireActivity()).get(VideoPlayerViewModel.class);
        Log.d(TAG, "Fragment Activity: " + requireActivity());
        Boolean currentState = videoPlayerViewModel.getPlayerPaused().getValue();
        Log.d(TAG, "Current state: " + currentState);
        Log.d(TAG, "Updated state: " + videoPlayerViewModel.getPlayerPaused().getValue());
        Log.d(TAG, "Pausing Film");

        if (routePartsLayout.getVisibility() == View.VISIBLE) {
            toggleMoviePartsVisibility();
        }
        try {
            volumeLevelBeforePause = videoPlayerViewModel.getVolumeLevel().getValue();
        } catch (NullPointerException e) {
            volumeLevelBeforePause = 10;
        }
        videoPlayerViewModel.setVolumeLevel(0);

        for (View statusBarBox : usedStatusBarBoxes) {
            if (statusBarBox.getId() != R.id.statusbar_time_box) {
                statusBarBox.setVisibility(View.GONE);
            }
        }
    }

    private void resumeFilm() {
        Log.d(TAG, "Resuming film");
        videoPlayerViewModel = new ViewModelProvider(requireActivity()).get(VideoPlayerViewModel.class);
        if (routePartsLayout.getVisibility() == View.GONE) {
            toggleMoviePartsVisibility();
        }

        for (View statusBarBox : usedStatusBarBoxes) {
            if (statusBarBox.getId() != R.id.statusbar_time_box) {
                statusBarBox.setVisibility(View.VISIBLE);
            }
        }

        if (volumeLevelBeforePause > 0) {
            videoPlayerViewModel.setVolumeLevel(volumeLevelBeforePause);
        }
    }

    private void showPausedDialog() {
        try {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.videoplayer_framelayout_pause_dialog, PauseFragment.class, null)
                    .setReorderingAllowed(true)
                    .commit();
        } catch (NullPointerException e) {
            try {
                getActivity().finish();
            } catch (NullPointerException ignored) {}
        }

        try {
            VideoplayerActivity.getInstance().togglePauseScreen();
        } catch (NullPointerException e) {
            VideoplayerExoActivity.getInstance().togglePauseScreen();
        }
    }

    private void hidePausedDialog() {
        try {
            VideoplayerActivity.getInstance().togglePauseScreen();
        } catch (NullPointerException e) {
            VideoplayerExoActivity.getInstance().togglePauseScreen();
        }

        Fragment fragment;
        FragmentManager fragmentManager;

        try {
            fragmentManager = getActivity().getSupportFragmentManager();
        } catch (NullPointerException e) {
            Toast.makeText(getContext(), "Something went wrong...", Toast.LENGTH_SHORT).show();
            return;
        }

        fragment = fragmentManager.findFragmentById(R.id.videoplayer_framelayout_pause_dialog);

        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    protected void toggleStatusbarVisibility() {
        if (statusbar.getVisibility() == View.VISIBLE) {
            statusbar.setVisibility(View.GONE);
        } else {
            statusbar.setVisibility(View.VISIBLE);
        }

//        new Handler().postDelayed(() -> repaintToggleStatusbarButton(true), 200);
        repaintToggleStatusbarButton(true);
    }

    private void repaintToggleStatusbarButton(boolean hasFocus) {
        if (hasFocus) {
            if (statusbar.getVisibility() == View.VISIBLE) {
                toggleStatusbarButton.setImageResource(R.drawable.double_arrow_down_rounded_rectangle_focused);
            } else {
                toggleStatusbarButton.setImageResource(R.drawable.double_arrow_up_rounded_rectangle_focused);
            }
        } else {
            if (statusbar.getVisibility() == View.VISIBLE) {
                toggleStatusbarButton.setImageResource(R.drawable.double_arrow_down_rounded_rectangle);
            } else {
                toggleStatusbarButton.setImageResource(R.drawable.double_arrow_up_rounded_rectangle);
            }
        }

    }
}
