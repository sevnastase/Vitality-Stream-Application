package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.content.DialogInterface;
import android.util.Log;
import android.app.AlertDialog;
import android.content.Context;
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
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;

import java.util.ArrayList;
import java.util.List;

public class PraxFilmStatusBarFragment extends Fragment {
    private static final String TAG = PraxFilmStatusBarFragment.class.getSimpleName();
    private VideoPlayerViewModel videoPlayerViewModel;

    //STATEFUL ELEMENTS
    private boolean onDragProgressbar = false;
    private boolean isLocalPlay = false;

    //Elements of the fragment to fill
    private TextView statusbarMovieTitle;
    private TextView statusbarMovieRpm;
    private TextView statusbarVolumeIndicator;
    private Chronometer stopwatchCurrentRide;
    private SeekBar progressBar;
    private ImageButton volumeUp;
    private ImageButton volumeDown;

    //MOVIE PARTS
    private RecyclerView statusbarRouteparts;
    private LinearLayout moviePartsLayout;
    private ImageButton toggleSwitchRoutepart;
    private RoutePartsAdapter routePartsAdapter;
    private List<Routepart> routeparts;
    private Handler loadTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_praxfilm_statusbar, container, false);

        statusbarMovieTitle = view.findViewById(R.id.statusbar_title_box_title);
        statusbarMovieRpm = view.findViewById(R.id.statusbar_speed_box_value);
        stopwatchCurrentRide = view.findViewById(R.id.statusbar_time_box_value);
        progressBar = view.findViewById(R.id.statusbar_praxfilm_seekbar_movie);
        statusbarVolumeIndicator = view.findViewById(R.id.statusbar_praxfilm_volume_indicator);
        volumeUp = view.findViewById(R.id.statusbar_praxfilm_volume_button_up);
        volumeDown = view.findViewById(R.id.statusbar_praxfilm_volume_button_down);

        moviePartsLayout = view.findViewById(R.id.statusbar_praxfilm_movieparts);
        toggleSwitchRoutepart = view.findViewById(R.id.statusbar_switch_part_button);
        statusbarRouteparts = view.findViewById(R.id.statusbar_praxfilm_recyclerview_movieparts);

        Bundle arguments = getArguments();
        if (arguments != null) {
            isLocalPlay = arguments.getBoolean("localPlay");
        }

        routeparts = new ArrayList<>();
        routePartsAdapter = new RoutePartsAdapter(routeparts, isLocalPlay, videoPlayerViewModel, getViewLifecycleOwner());
//        statusbarRouteparts.setAdapter(routePartsAdapter);

        //INIT VALUES
        stopwatchCurrentRide.setFormat(getString(R.string.videoplayer_chronometer_message));
        stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                goToSecond(newProgress);
            }
        });

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRouteparts.setLayoutManager(layoutManager);

        toggleSwitchRoutepart.setOnClickListener(clickedView -> {
            toggleMoviePartsVisibility();
        });

        //SET FOCUS LISTENERS
        volumeUp.setOnFocusChangeListener((itemView,hasFocus) -> {
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

        toggleSwitchRoutepart.setOnFocusChangeListener((itemView, hasFocus)->{
            if (hasFocus) {
                final Drawable border = view.getContext().getDrawable(R.drawable.imagebutton_red_border);
                toggleSwitchRoutepart.setBackground(border);
            } else {
                toggleSwitchRoutepart.setBackground(null);
            }
        });

        //SET BUTTONS FOCUSABLE
        volumeUp.setFocusable(true);
        volumeDown.setFocusable(true);
        toggleSwitchRoutepart.setFocusable(true);

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
                } else {
                    //HIDE MEDIA CONTROLS FOR TIME
                    LinearLayout routepartsBox = view.findViewById(R.id.statusbar_switch_part_box);
                    routepartsBox.setVisibility(View.GONE);
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
                if (!isTouchScreen() && toggleSwitchRoutepart.getVisibility() == View.VISIBLE) {
                    toggleMoviePartsVisibility();
                }
            } else {
                stopwatchCurrentRide.start();
            }
        });

        //RESET STOPWATCH TO ZERO
        videoPlayerViewModel.getResetChronometer().observe(getViewLifecycleOwner(), resetChronometer -> {
            if (resetChronometer) {
                stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());
                videoPlayerViewModel.setResetChronometer(false);
            }
        });

        //Movie object related
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (selectedMovie != null) {
                //Set movie title
                statusbarMovieTitle.setText(selectedMovie.getMovieTitle());

                //PLAYER TIME AND DISTANCE related
                videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds ->{
                    if (movieTotalDurationSeconds!=null) {
                        videoPlayerViewModel.getMovieSpendDurationSeconds().observe(getViewLifecycleOwner(), movieSpendDurationSeconds -> {
                            if (movieSpendDurationSeconds!=null && !onDragProgressbar) {
                                progressBar.setMax(movieTotalDurationSeconds.intValue());
                                progressBar.setProgress(movieSpendDurationSeconds.intValue());
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
                                routeparts.add(routepart);
                            }
                            routePartsAdapter.setRouteparts(routeparts);
                            routePartsAdapter.notifyDataSetChanged();
                            statusbarRouteparts.setAdapter(routePartsAdapter);
                        }
                    }
                });
            }
        });

        //RPM data related
        videoPlayerViewModel.getRpmData().observe(getViewLifecycleOwner(), rpmData ->{
            statusbarMovieRpm.setText(toString().format(getString(R.string.video_screen_rpm), rpmData));
        });

        videoPlayerViewModel.getVolumeLevel().observe(getViewLifecycleOwner(), volumeLevel -> {
            if (volumeLevel!= null) {
                statusbarVolumeIndicator.setText(""+(int) (volumeLevel));
                volumeUp.setOnClickListener(clickedView -> {
                    videoPlayerViewModel.setVolumeLevel(volumeLevel+10);
                });
                volumeDown.setOnClickListener(clickedView -> {
                    videoPlayerViewModel.setVolumeLevel(volumeLevel-10);
                });
            }
        });
    }

    private void goToSecond(final int newProgress) {
        Log.d(TAG, "newProgress: "+newProgress);
        int framesPerSecond = 30;
        int frameNumber = (newProgress/1000) * framesPerSecond;
        Log.d(TAG, "framenumber: "+frameNumber);
        VideoplayerActivity.getInstance().goToFrameNumber(frameNumber);
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

    private void showPauseOrStopDialog() {
        Context context = getActivity();
        if(context == null)
        {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pause or Stop");
        builder.setMessage("Do you want to pause or stop?");

        builder.setPositiveButton("Pause", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle Pause action
                // TODO: Implement your pause logic here
                Log.d(TAG, "Pause selected.");
            }
        });

        builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle Stop action
                // TODO: Implement your stop logic here
                Log.d(TAG, "Stop selected.");
            }
        });

        builder.setNeutralButton("Cancel", null); // just dismisses the dialog

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
