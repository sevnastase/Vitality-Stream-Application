package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.app.Activity;
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
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.DistanceLookupTable;
import com.videostreamtest.utils.RpmVectorLookupTable;

public class PraxSpinStatusBarFragment extends Fragment {
    private static final String TAG = PraxSpinStatusBarFragment.class.getSimpleName();
    private VideoPlayerViewModel videoPlayerViewModel;
    private RoutePartsAdapter routePartsAdapter;

    private TextView statusbarMovieTitle;
    private TextView statusbarDistance;
    private TextView statusbarTotalDistance;
    private TextView speedIndicator;

    //CLOCK
    private Handler loadTimer;
    private Chronometer stopwatchCurrentRide;

    //SPEED BUTTONS
    private ImageButton speedUpButton;
    private ImageButton speedDownButton;

    //STOP BUTTON
    private ImageButton stopButton;

    //TOGGLE SWITCH FOR ROUTEPARTS
    private LinearLayout moviePartsLayout;
    private RecyclerView statusbarRouteparts;
    private ImageButton toggleSwitchRoutepart;


    //VOLUME
    private TextView statusbarVolumeIndicator;
    private ImageButton volumeUp;
    private ImageButton volumeDown;

    //ROUTE PROGRESS
    private SeekBar progressBar;

    private boolean isLocalPlay = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_praxspin_statusbar, container, false);

        statusbarMovieTitle = view.findViewById(R.id.statusbar_title_box_title);
        stopwatchCurrentRide = view.findViewById(R.id.statusbar_time_box_value);
        statusbarDistance = view.findViewById(R.id.statusbar_distance_box_value);
        statusbarTotalDistance = view.findViewById(R.id.statusbar_distance_finish_box_value);
        statusbarRouteparts = view.findViewById(R.id.statusbar_praxspin_recyclerview_movieparts);
        moviePartsLayout = view.findViewById(R.id.statusbar_praxspin_movieparts);
        toggleSwitchRoutepart = view.findViewById(R.id.statusbar_switch_part_button);
        //VOLUME
        statusbarVolumeIndicator = view.findViewById(R.id.statusbar_praxspin_volume_indicator);
        volumeUp = view.findViewById(R.id.statusbar_praxspin_volume_button_up);
        volumeDown = view.findViewById(R.id.statusbar_praxspin_volume_button_down);
        //STOP BUTTON
        stopButton = view.findViewById(R.id.statusbar_praxspin_stop_button);

        //PROGRESSBAR
        progressBar = view.findViewById(R.id.statusbar_praxspin_progress_indicator);

        //INIT VALUES
        stopwatchCurrentRide.setFormat(getString(R.string.videoplayer_chronometer_message));
        stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());

        //SPEED BUTTONS
        speedIndicator = view.findViewById(R.id.statusbar_praxspin_speed_indicator);
        speedUpButton = view.findViewById(R.id.statusbar_praxspin_speed_button_up);
        speedDownButton = view.findViewById(R.id.statusbar_praxspin_speed_button_down);

        Bundle arguments = getArguments();
        if (arguments!= null) {
            isLocalPlay = arguments.getBoolean("localPlay");
        }

        statusbarRouteparts.setHasFixedSize(true);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRouteparts.setLayoutManager(layoutManager);

        toggleSwitchRoutepart.setOnClickListener(clickedView -> {
            toggleMoviePartsVisibility();
        });

        stopButton.setOnClickListener(clickedView -> {
            ((Activity) view.getContext()).finish();
        });

        //SET FOCUS LISTENERS
        toggleSwitchRoutepart.setOnFocusChangeListener((itemView, hasFocus)->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                toggleSwitchRoutepart.setBackground(border);
            } else {
                toggleSwitchRoutepart.setBackground(null);
            }
        });

        volumeUp.setOnFocusChangeListener((itemView,hasFocus) ->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                volumeUp.setBackground(border);

            } else {
                volumeUp.setBackground(null);
            }
        });

        volumeDown.setOnFocusChangeListener((itemView,hasFocus) ->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                volumeDown.setBackground(border);
            } else {
                volumeDown.setBackground(null);
            }
        });

        speedUpButton.setOnFocusChangeListener((itemView,hasFocus) ->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                speedUpButton.setBackground(border);
            } else {
                speedUpButton.setBackground(null);
            }
        });
        speedDownButton.setOnFocusChangeListener((itemView,hasFocus) ->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                speedDownButton.setBackground(border);
            } else {
                speedDownButton.setBackground(null);
            }
        });
        stopButton.setOnFocusChangeListener((itemView,hasFocus) ->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                stopButton.setBackground(border);
            } else {
                stopButton.setBackground(null);
            }
        });

        //SET BUTTONS FOCUSABLE
        toggleSwitchRoutepart.setFocusable(true);
        volumeUp.setFocusable(true);
        volumeDown.setFocusable(true);
        speedUpButton.setFocusable(true);
        speedDownButton.setFocusable(true);
        stopButton.setFocusable(true);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoPlayerViewModel = new ViewModelProvider(requireActivity()).get(VideoPlayerViewModel.class);

        //STATUSBAR VISIBILITY
        videoPlayerViewModel.getStatusbarVisible().observe(getViewLifecycleOwner(), statusBarVisible -> {
            if (statusBarVisible) {
                view.setVisibility(View.VISIBLE);
                stopwatchCurrentRide.start();

                if (!isTouchScreen()) {
                    //SET FOCUS ON BUTTON
                    toggleSwitchRoutepart.requestFocus();
                    toggleSwitchRoutepart.requestFocusFromTouch();
                }
            } else {
                view.setVisibility(View.GONE);
                stopwatchCurrentRide.stop();
            }
        });

        //ROUTE IS PAUSED STATUS BUT VIEW IS STILL VISIBLE
        videoPlayerViewModel.getPlayerPaused().observe(getViewLifecycleOwner(), isPaused -> {
            if (isPaused) {
                stopwatchCurrentRide.stop();
                if (toggleSwitchRoutepart.getVisibility() == View.VISIBLE) {
                    toggleMoviePartsVisibility();
                }
            } else {
                stopwatchCurrentRide.start();
            }
        });

        //Movie object related
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (selectedMovie!= null) {
                //Set movie title
                statusbarMovieTitle.setText(selectedMovie.getMovieTitle());

                //set distance text values
                //PLAYER TIME AND DISTANCE related
                videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds ->{
                    if (movieTotalDurationSeconds!=null) {
                        videoPlayerViewModel.getMovieSpendDurationSeconds().observe(getViewLifecycleOwner(), movieSpendDurationSeconds -> {
                            if (movieSpendDurationSeconds!=null) {
                                final float mps = DistanceLookupTable.getMeterPerSecond(selectedMovie.getMovieLength(), movieTotalDurationSeconds / 1000);
                                final int currentMetersDone = (int) (mps * (movieSpendDurationSeconds / 1000));
                                statusbarDistance.setText(toString().format(getString(R.string.video_screen_distance), currentMetersDone));

                                final int metersToGo = selectedMovie.getMovieLength() - currentMetersDone;
                                statusbarTotalDistance.setText(String.format(getString(R.string.video_screen_total_distance), metersToGo));

                                progressBar.setMax(movieTotalDurationSeconds.intValue());
                                progressBar.setProgress(movieSpendDurationSeconds.intValue());
                            }
                        });
                    }
                });

                //LOAD ROUTEPARTS IF AVAILABLE
                videoPlayerViewModel.getRoutePartsOfMovieId(selectedMovie.getId()).observe(getViewLifecycleOwner(), routeparts -> {
                    if (routeparts != null && routeparts.size()>0) {
                        routePartsAdapter = new RoutePartsAdapter(routeparts, isLocalPlay);
                        statusbarRouteparts.setAdapter(routePartsAdapter);
                    }
                });
            }
        });

        videoPlayerViewModel.getVolumeLevel().observe(getViewLifecycleOwner(), volumeLevel -> {
            if (volumeLevel!= null) {
                statusbarVolumeIndicator.setText(""+(int) (volumeLevel*100));
                volumeUp.setOnClickListener(clickedView -> {
                    if (volumeLevel < 1.0f) {
                        videoPlayerViewModel.setVolumeLevel(volumeLevel + 0.1f);
                    }
                });
                volumeDown.setOnClickListener(clickedView -> {
                    if (volumeLevel > 0.1f) {
                        videoPlayerViewModel.setVolumeLevel(volumeLevel - 0.1f);
                    }
                });
            }
        });

        videoPlayerViewModel.getKmhData().observe(getViewLifecycleOwner(), kmhData -> {
            if (kmhData != null) {
                speedIndicator.setText(""+kmhData+" kmh");
                speedUpButton.setOnClickListener(clickedView ->{
                    videoPlayerViewModel.setKmhData(kmhData+2);
                });
                speedDownButton.setOnClickListener(clickedView -> {
                    if (kmhData>2) {
                        videoPlayerViewModel.setKmhData(kmhData - 2);
                    }
                });
            }
        });

    }

    private void toggleMoviePartsVisibility() {
        if (moviePartsLayout.getVisibility() == View.GONE) {
            loadTimer = new Handler(Looper.getMainLooper());

            Runnable closeMoviePartsLayout = new Runnable() {
                public void run() {
                    toggleMoviePartsVisibility();
                }
            };
            //Redirect to login activity if timer exceeds 5 seconds
            loadTimer.postDelayed( closeMoviePartsLayout, 20*1000 );

            moviePartsLayout.setVisibility(View.VISIBLE);
            if (moviePartsLayout.getChildCount()>0) {
                moviePartsLayout.getChildAt(0).requestFocus();
            }
        } else {
            loadTimer.removeCallbacksAndMessages(null);
            moviePartsLayout.setVisibility(View.GONE);
        }
    }

    private boolean isTouchScreen() {
        return getView().getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }
}
