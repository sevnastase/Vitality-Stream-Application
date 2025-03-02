package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;

public class PraxFitStatusBarFragment extends AbstractPraxStatusBarFragment {
    private static final String TAG = PraxFitStatusBarFragment.class.getSimpleName();

    //Elements of the fragment to fill
    private TextView statusbarMovieRpm;
    private TextView statusbarDistance;
    private TextView statusbarTotalDistance;
//    private Chronometer stopwatchCurrentRide;

    //MOVIE PARTS
    private ImageButton toggleRoutePartsButton;

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
    private int distanceOffset;

    private AlertDialog resumeOrStopDialog;


    @Override
    protected void initializeLayout(View view) {
        super.initializeLayout(view);

        statusbarMovieRpm = view.findViewById(R.id.statusbar_rpm_value);
        statusbarDistance = view.findViewById(R.id.statusbar_distance_box_value);
        statusbarTotalDistance = view.findViewById(R.id.statusbar_distance_finish_box_value);

        toggleRoutePartsButton = view.findViewById(R.id.statusbar_toggle_movieparts_button);

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

        addUsedViews(new View[]{
                view.findViewById(R.id.statusbar_time_box),
                view.findViewById(R.id.statusbar_rpm_box),
                view.findViewById(R.id.statusbar_distance_box),
                view.findViewById(R.id.statusbar_distance_finish_box),
                view.findViewById(R.id.statusbar_toggle_movieparts_box),
                view.findViewById(R.id.statusbar_volume_buttons_box)
        });

        if (startedFromMotolife) {
            addUsedViews(new View[]{
                    view.findViewById(R.id.statusbar_motolife_power_box),
                    view.findViewById(R.id.chinesport_logo_imageview),
                    view.findViewById(R.id.motolife_info_layout)
            });
        }
    }

    @Override
    protected void setupFunctionality(View view) {
        super.setupFunctionality(view);

        routePartsLayout.setOnClickListener(v -> routePartsLayout.setVisibility(View.GONE));

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRoutePartsView.setLayoutManager(layoutManager);

        toggleRoutePartsButton.setOnClickListener(clickedView -> {
            toggleMoviePartsVisibility();
        });

        addRedBorderOnFocus(new View[]{toggleRoutePartsButton});
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
    protected void setupVisibilities(View view) {
        super.setupVisibilities(view);

        for (ImageButton tButton : seekBarButtons) {
            tButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void useVideoPlayerViewModel(View view) {
        super.useVideoPlayerViewModel(view);

        //STATUSBAR VISIBILITY
        videoPlayerViewModel.getStatusbarVisible().observe(getViewLifecycleOwner(), statusBarVisible -> {
            if (statusBarVisible) {
                view.setVisibility(View.VISIBLE);
//                stopwatchCurrentRide.start();

                if (!isTouchScreen()) {
                    //SET FOCUS ON BUTTON
                    toggleRoutePartsButton.requestFocus();
                    toggleRoutePartsButton.requestFocusFromTouch();
                }
            } else {
                view.setVisibility(View.GONE);
//                stopwatchCurrentRide.stop();
            }
        });

        //ROUTE IS PAUSED STATUS BUT VIEW IS STILL VISIBLE
        videoPlayerViewModel.getPlayerPaused().observe(getViewLifecycleOwner(), isPaused -> {
            if (isPaused) {
//                stopwatchCurrentRide.stop();
            } else {
//                stopwatchCurrentRide.start();
            }
        });

        //RESET STOPWATCH TO ZERO
        videoPlayerViewModel.getResetChronometer().observe(getViewLifecycleOwner(), resetChronometer -> {
            if (resetChronometer) {
//                stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());
                videoPlayerViewModel.setResetChronometer(false);
            }
        });

        //Movie object related
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (selectedMovie!= null) {

                setupSeekbarButtonsFunctionality();

                //set distance text values
                //PLAYER TIME AND DISTANCE related
                videoPlayerViewModel.getMovieTotalDurationSeconds().observe(getViewLifecycleOwner(), movieTotalDurationSeconds ->{
                    if (movieTotalDurationSeconds!=null) {
                        videoPlayerViewModel.getMovieSpendDurationSeconds().observe(getViewLifecycleOwner(), movieSpendDurationSeconds -> {
                            if (movieSpendDurationSeconds!=null) {

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "finalFrame = " + finalFrame);
                                        Log.d(TAG, "seekBarWidth = " + seekBarWidth);
                                        Log.d(TAG,"progressBar.getWidth() = " + movieProgressBar.getWidth());
                                        Log.d(TAG, "progressBar.getPaddingStart()" + movieProgressBar.getPaddingStart());
                                        Log.d(TAG, "progressBar.getPaddingEnd()" + movieProgressBar.getPaddingEnd());
                                        seekBarWidth = movieProgressBar.getWidth() - movieProgressBar.getPaddingStart() - movieProgressBar.getPaddingEnd();
                                        if (movieParts != null) {
                                            for (int i = 0; i < movieParts.length; i++) {
                                                frameNumber = movieParts[i].getFrameNumber().intValue();
                                                Log.d(TAG, "movieParts[" + i + "] frameNumber = " + frameNumber);
                                                position = ((float) frameNumber / finalFrame) * seekBarWidth;
                                                Log.d(TAG, "position of movieParts[" + i + "] = " + position);
                                                if (seekBarButtons[i] != null) {
                                                    seekBarButtons[i].setX(movieProgressBar.getX() + movieProgressBar.getPaddingStart() + position);
                                                    Log.d(TAG, "seekBarButtons[" + i + "] position = " +
                                                            movieProgressBar.getX() + movieProgressBar.getPaddingStart() + position);
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
                        statusbarRoutePartsView.setAdapter(routePartsAdapter);
                    }
                });
            }
        });

        //RPM data related
        videoPlayerViewModel.getRpmData().observe(getViewLifecycleOwner(), rpmData ->{
            statusbarMovieRpm.setText(toString().format(getString(R.string.video_screen_rpm), rpmData));
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

    @Override
    public void onResume() {
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_JUMP");
        mqttMessageFilter.addAction("com.videostreamtest.ACTION_TOGGLE_ROUTEPARTS");
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mqttMessageReceiver);
        super.onPause();
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

    private void jumpToRoutepart(int routepartNr) {
        videoPlayerViewModel.getSelectedMovie().observe(getViewLifecycleOwner(), selectedMovie -> {
            if (AccountHelper.getAccountType(getContext()).equalsIgnoreCase("standalone")) {
                // WAS VIDEOPLAYERACTIVITY.GETINSTANCE IN ALL 6 OF THEM
                VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[routepartNr].getFrameNumber().intValue());
            } else {
                VideoplayerExoActivity.getInstance().goToFrameNumber(movieParts[routepartNr].getFrameNumber().intValue());
            }
            Log.d(TAG, "movieParts[0] frame as int = " + movieParts[routepartNr].getFrameNumber().intValue());
            if (routePartsLayout.getVisibility() == View.VISIBLE) {
                routePartsLayout.setVisibility(View.GONE);
            }
            try {
                toggleRoutePartsLayoutTimer.removeCallbacksAndMessages(null);
            } catch (NullPointerException ignored) {
            }

            videoPlayerViewModel.resetDistance(movieParts[routepartNr], selectedMovie);
        });
    }


    private void pauseFilm() {
        videoPlayerViewModel = new ViewModelProvider(requireActivity()).get(VideoPlayerViewModel.class);
        Log.d(TAG, "Fragment Activity: " + requireActivity());
        Boolean currentState = videoPlayerViewModel.getPlayerPaused().getValue();
        Log.d(TAG, "Current state: " + currentState);
        videoPlayerViewModel.setPlayerPaused(true);
        Log.d(TAG, "Updated state: " + videoPlayerViewModel.getPlayerPaused().getValue());
        Log.d(TAG, "Pausing Film");
    }

    private void showResumeOrStopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Film Paused");
        builder.setMessage("Use Motolife interface to resume or stop the film");

        //builder.setPositiveButton("Resume", new DialogInterface.OnClickListener() {
        //    @Override
        //    public void onClick(DialogInterface dialogInterface, int i) {
        //        // Handle Resume action
        //        // TODO: Implement your resume logic here
        //        resumeOrStopDialog.dismiss();
        //        videoPlayerViewModel = new ViewModelProvider(requireActivity()).get(VideoPlayerViewModel.class);
        //        videoPlayerViewModel.setPlayerPaused(false);
        //        Log.d(TAG, "Resume selected.");
        //    }
        //});

        //builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
        //    @Override
        //    public void onClick(DialogInterface dialogInterface, int i) {
        //        // Handle Stop action
        //        // TODO: Implement your stop logic here
        //        resumeOrStopDialog.dismiss();
        //        ((Activity) Objects.requireNonNull(getView()).getContext()).finish();
        //        Log.d(TAG, "Stop selected.");
        //    }
        //});

        // Show the alert dialog
        resumeOrStopDialog = builder.create();
        resumeOrStopDialog.show();
        resumeOrStopDialog.setCancelable(false);

        if (routePartsLayout.getVisibility() == View.VISIBLE) {
            routePartsLayout.setVisibility(View.GONE);
        }
    }

//    public int resetDistance(MoviePart moviePart, Movie selectedMovie, float mps) {
//        int seekBarPartFrameNumber = moviePart.getFrameNumber().intValue();
//        int seekBarPartDurationSeconds = (1000 * seekBarPartFrameNumber) / selectedMovie.getRecordedFps().intValue();
//        distanceOffset = (int) (mps * (seekBarPartDurationSeconds / 1000));
//        return distanceOffset;
//    }
}
