package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.content.Context;
import android.content.Intent;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.DistanceLookupTable;

public class PraxFitStatusBarFragment extends Fragment {
    private static final String TAG = PraxFitStatusBarFragment.class.getSimpleName();
    private VideoPlayerViewModel videoPlayerViewModel;
    private RoutePartsAdapter routePartsAdapter;

    //Elements of the fragment to fill
    private TextView statusbarMovieTitle;
    private TextView statusbarMovieRpm;
    private TextView statusbarDistance;
    private TextView statusbarTotalDistance;
    private Chronometer stopwatchCurrentRide;
    private RecyclerView statusbarRouteparts;

    private ConstraintLayout titlelayout;

    //MOVIE PARTS
    private LinearLayout moviePartsLayout;
    private ImageButton toggleSwitchRoutepart;
    private Handler loadTimer;

    //VOLUME
    private TextView statusbarVolumeIndicator;
    private ImageButton volumeUp;
    private ImageButton volumeDown;

    //SEEK BAR
    private ImageButton seekBarT1;
    private ImageButton seekBarT2;
    private ImageButton seekBarT3;
    private ImageButton seekBarT4;
    private ImageButton seekBarT5;
    private ImageButton seekBarT6;
    private int seekBarWidth;
    private ImageButton[] seekBarButtons;
    private MoviePart[] movieParts;
    private int frameNumber;
    private int finalFrame;
    private float position;

    //ROUTE PROGRESS
    private SeekBar progressBar;
    private boolean isLocalPlay = false;
    private int distanceOffset;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_praxfit_statusbar, container, false);

        //Link items to lay-out
        titlelayout = view.findViewById(R.id.statusbar_title_box);
        statusbarMovieTitle = view.findViewById(R.id.statusbar_title_box_title);
        statusbarMovieRpm = view.findViewById(R.id.statusbar_speed_box_value);
        statusbarDistance = view.findViewById(R.id.statusbar_distance_box_value);
        statusbarTotalDistance = view.findViewById(R.id.statusbar_distance_finish_box_value);
        stopwatchCurrentRide = view.findViewById(R.id.statusbar_time_box_value);
        statusbarRouteparts = view.findViewById(R.id.statusbar_praxfit_recyclerview_movieparts);
        statusbarVolumeIndicator = view.findViewById(R.id.statusbar_praxfit_volume_indicator);
        volumeUp = view.findViewById(R.id.statusbar_praxfit_volume_button_up);
        volumeDown = view.findViewById(R.id.statusbar_praxfit_volume_button_down);
        progressBar = view.findViewById(R.id.statusbar_praxfit_progress_indicator);
        moviePartsLayout = view.findViewById(R.id.statusbar_praxfit_movieparts);
        toggleSwitchRoutepart = view.findViewById(R.id.statusbar_switch_part_button);

        //INIT VALUES
        stopwatchCurrentRide.setFormat(getString(R.string.videoplayer_chronometer_message));
        stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());

        //SEEKBAR BUTTONS
        seekBarT1 = view.findViewById(R.id.statusbar_praxfit_seekbar_t1);
        seekBarT2 = view.findViewById(R.id.statusbar_praxfit_seekbar_t2);
        seekBarT3 = view.findViewById(R.id.statusbar_praxfit_seekbar_t3);
        seekBarT4 = view.findViewById(R.id.statusbar_praxfit_seekbar_t4);
        seekBarT5 = view.findViewById(R.id.statusbar_praxfit_seekbar_t5);
        seekBarT6 = view.findViewById(R.id.statusbar_praxfit_seekbar_t6);

        //FILL SEEKBARBUTTONS ARRAY
        seekBarButtons = new ImageButton[6];
        seekBarButtons[0] = seekBarT1;
        seekBarButtons[1] = seekBarT2;
        seekBarButtons[2] = seekBarT3;
        seekBarButtons[3] = seekBarT4;
        seekBarButtons[4] = seekBarT5;
        seekBarButtons[5] = seekBarT6;

        Bundle arguments = getArguments();
        if (arguments != null) {
            isLocalPlay = arguments.getBoolean("localPlay");
        }

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRouteparts.setLayoutManager(layoutManager);

        toggleSwitchRoutepart.setOnClickListener(clickedView -> {
            toggleMoviePartsVisibility();
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

        //SET BUTTONS FOCUSABLE
        toggleSwitchRoutepart.setFocusable(true);
        volumeUp.setFocusable(true);
        volumeDown.setFocusable(true);

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

        //RESET STOPWATCH TO ZERO
        videoPlayerViewModel.getResetChronometer().observe(getViewLifecycleOwner(), resetChronometer -> {
            if (resetChronometer) {
                stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());
                videoPlayerViewModel.setResetChronometer(false);
            }
        });

        //Movie object related
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (selectedMovie!= null) {
                //Set movie title
                statusbarMovieTitle.setText(selectedMovie.getMovieTitle());

                seekBarT1.setOnClickListener(v -> {
                    seekBarT1.requestFocus();
                    if (AccountHelper.getAccountType(v.getContext()).equalsIgnoreCase("standalone")) {
                        VideoplayerActivity.getInstance().goToFrameNumber(movieParts[0].getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[0].getFrameNumber().intValue());
                    }
                    Log.d(TAG, "movieParts[0] frame as int = " + movieParts[0].getFrameNumber().intValue());
                    toggleMoviePartsVisibility();
                    videoPlayerViewModel.resetDistance(movieParts[0], selectedMovie);
                });
                seekBarT2.setOnClickListener(v -> {
                    seekBarT2.requestFocus();
                    if (AccountHelper.getAccountType(v.getContext()).equalsIgnoreCase("standalone")) {
                        VideoplayerActivity.getInstance().goToFrameNumber(movieParts[1].getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[1].getFrameNumber().intValue());
                    }
                    Log.d(TAG, "movieParts[1] frame as int = " + movieParts[1].getFrameNumber().intValue());
                    toggleMoviePartsVisibility();
                    videoPlayerViewModel.resetDistance(movieParts[1], selectedMovie);
                });
                seekBarT3.setOnClickListener(v -> {
                    seekBarT3.requestFocus();
                    if (AccountHelper.getAccountType(v.getContext()).equalsIgnoreCase("standalone")) {
                        VideoplayerActivity.getInstance().goToFrameNumber(movieParts[2].getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[2].getFrameNumber().intValue());
                    }
                    Log.d(TAG, "movieParts[2] frame as int = " + movieParts[2].getFrameNumber().intValue());
                    toggleMoviePartsVisibility();
                    videoPlayerViewModel.resetDistance(movieParts[2], selectedMovie);
                });
                seekBarT4.setOnClickListener(v -> {
                    seekBarT4.requestFocus();
                    if (AccountHelper.getAccountType(v.getContext()).equalsIgnoreCase("standalone")) {
                        VideoplayerActivity.getInstance().goToFrameNumber(movieParts[3].getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[3].getFrameNumber().intValue());
                    }
                    Log.d(TAG, "movieParts[3] frame as int = " + movieParts[3].getFrameNumber().intValue());
                    toggleMoviePartsVisibility();
                    videoPlayerViewModel.resetDistance(movieParts[3], selectedMovie);
                });
                seekBarT5.setOnClickListener(v -> {
                    seekBarT5.requestFocus();
                    if (AccountHelper.getAccountType(v.getContext()).equalsIgnoreCase("standalone")) {
                        VideoplayerActivity.getInstance().goToFrameNumber(movieParts[4].getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[4].getFrameNumber().intValue());
                    }
                    Log.d(TAG, "movieParts[4] frame as int = " + movieParts[4].getFrameNumber().intValue());
                    toggleMoviePartsVisibility();
                    videoPlayerViewModel.resetDistance(movieParts[4], selectedMovie);
                });
                seekBarT6.setOnClickListener(v -> {
                    seekBarT6.requestFocus();
                    if (AccountHelper.getAccountType(v.getContext()).equalsIgnoreCase("standalone")) {
                        VideoplayerActivity.getInstance().goToFrameNumber(movieParts[5].getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[5].getFrameNumber().intValue());
                    }
                    Log.d(TAG, "movieParts[5] frame as int = " + movieParts[5].getFrameNumber().intValue());
                    toggleMoviePartsVisibility();
                    videoPlayerViewModel.resetDistance(movieParts[5], selectedMovie);
                });

                //set distance text values
                //PLAYER TIME AND DISTANCE related
                videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds ->{
                    if (movieTotalDurationSeconds!=null) {
                        videoPlayerViewModel.getMovieSpendDurationSeconds().observe(getViewLifecycleOwner(), movieSpendDurationSeconds -> {
                            if (movieSpendDurationSeconds!=null) {
//                                final float mps = DistanceLookupTable.getMeterPerSecond(selectedMovie.getMovieLength(), movieTotalDurationSeconds / 1000);
//                                int currentMetersDone = (int) (mps * (movieSpendDurationSeconds / 1000)) - distanceOffset;
//                                if (currentMetersDone < 0) currentMetersDone = 0;
//
//                                videoPlayerViewModel.setCurrentMetersDone(currentMetersDone);
//                                videoPlayerViewModel.getCurrentMetersDone().observe(getViewLifecycleOwner(), updatedCurrentMetersDone -> {
//                                    if (updatedCurrentMetersDone != null) {
//                                        statusbarDistance.setText(String.format(getString(R.string.video_screen_distance), updatedCurrentMetersDone));
//                                    }
//                                });
//
//                                final int metersToGo = selectedMovie.getMovieLength() - currentMetersDone - distanceOffset;
//
//                                videoPlayerViewModel.setMetersToGo(metersToGo);
//                                videoPlayerViewModel.getMetersToGo().observe(getViewLifecycleOwner(), updatedMetersToGo -> {
//                                    if (updatedMetersToGo != null) {
//                                        statusbarTotalDistance.setText(String.format(getString(R.string.video_screen_total_distance), updatedMetersToGo));
//                                    }
//                                });

                                progressBar.setMax(movieTotalDurationSeconds.intValue());
                                progressBar.setProgress(movieSpendDurationSeconds.intValue());

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "finalFrame = " + finalFrame);
                                        Log.d(TAG, "seekBarWidth = " + seekBarWidth);
                                        Log.d(TAG,"progressBar.getWidth() = " + progressBar.getWidth());
                                        Log.d(TAG, "progressBar.getPaddingStart()" + progressBar.getPaddingStart());
                                        Log.d(TAG, "progressBar.getPaddingEnd()" + progressBar.getPaddingEnd());
                                        seekBarWidth = progressBar.getWidth() - progressBar.getPaddingStart() - progressBar.getPaddingEnd();
                                        if (movieParts != null) {
                                            for (int i = 0; i < movieParts.length; i++) {
                                                frameNumber = movieParts[i].getFrameNumber().intValue();
                                                Log.d(TAG, "movieParts[" + i + "] frameNumber = " + frameNumber);
                                                position = ((float) frameNumber / finalFrame) * seekBarWidth;
                                                Log.d(TAG, "position of movieParts[" + i + "] = " + position);
                                                if (seekBarButtons[i] != null) {
                                                    seekBarButtons[i].setX(progressBar.getX() + progressBar.getPaddingStart() + position);
                                                    Log.d(TAG, "seekBarButtons[" + i + "] position = " +
                                                            progressBar.getX() + progressBar.getPaddingStart() + position);
                                                }
                                            }
                                        }
                                    }
                                }, 5500);
                            }
                        });
                    }
                });

                //LOAD ROUTEPARTS IF AVAILABLE
                videoPlayerViewModel.getRoutePartsOfMovieId(selectedMovie.getId()).observe(getViewLifecycleOwner(), routeparts -> {
                    if (routeparts != null && routeparts.size()>0) {
                        routePartsAdapter = new RoutePartsAdapter(routeparts, isLocalPlay, videoPlayerViewModel, getViewLifecycleOwner());
                        statusbarRouteparts.setAdapter(routePartsAdapter);
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
                    videoPlayerViewModel.setVolumeLevel(volumeLevel + 10);
                });
                volumeDown.setOnClickListener(clickedView -> {
                    videoPlayerViewModel.setVolumeLevel(volumeLevel - 10);
                });
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

    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
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

    public MoviePart[] getMovieParts(VideoPlayerViewModel videoPlayerViewModel) {
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

    public int getFinalFrame(VideoPlayerViewModel videoPlayerViewModel) {
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds -> {
                finalFrame = (int) ((movieTotalDurationSeconds / 1000) * selectedMovie.getRecordedFps().intValue());
            });
        });
        return finalFrame;
    }

//    public int resetDistance(MoviePart moviePart, Movie selectedMovie, float mps) {
//        int seekBarPartFrameNumber = moviePart.getFrameNumber().intValue();
//        int seekBarPartDurationSeconds = (1000 * seekBarPartFrameNumber) / selectedMovie.getRecordedFps().intValue();
//        distanceOffset = (int) (mps * (seekBarPartDurationSeconds / 1000));
//        return distanceOffset;
//    }
}
