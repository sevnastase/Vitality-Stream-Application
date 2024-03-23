package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Update;

import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.videoplayer.MQTTService;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.BluetoothHelper;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsAdapter;
import com.videostreamtest.ui.phone.videoplayer.fragments.routeparts.RoutePartsViewHolder;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.DistanceLookupTable;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PraxSpinStatusBarFragment extends Fragment implements BluetoothHelper.BluetoothDeviceListener {

    private BluetoothHelper bluetoothHelper;
    private static final String TAG = PraxSpinStatusBarFragment.class.getSimpleName();
    private VideoPlayerViewModel videoPlayerViewModel;
    private RoutePartsAdapter routePartsAdapter;
    private RoutePartsViewHolder routePartsViewHolder;

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

    //FOR CHINESPORT
    private View chinesportStatsContainer;
    private Button chinesportToggle;
    private TextView chinesportSpeed;
    private TextView chinesportPower;
    private TextView chinesportMode;
    private TextView chinesportDirection;
    private TextView chinesportTime;

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

        //SEEKBAR BUTTONS
        seekBarT1 = view.findViewById(R.id.statusbar_praxspin_seekbar_t1);
        seekBarT2 = view.findViewById(R.id.statusbar_praxspin_seekbar_t2);
        seekBarT3 = view.findViewById(R.id.statusbar_praxspin_seekbar_t3);
        seekBarT4 = view.findViewById(R.id.statusbar_praxspin_seekbar_t4);
        seekBarT5 = view.findViewById(R.id.statusbar_praxspin_seekbar_t5);
        seekBarT6 = view.findViewById(R.id.statusbar_praxspin_seekbar_t6);

        //FILL SEEKBARBUTTONS ARRAY
        seekBarButtons = new ImageButton[6];
        seekBarButtons[0] = seekBarT1;
        seekBarButtons[1] = seekBarT2;
        seekBarButtons[2] = seekBarT3;
        seekBarButtons[3] = seekBarT4;
        seekBarButtons[4] = seekBarT5;
        seekBarButtons[5] = seekBarT6;

        // CHINESPORT DATA
        chinesportStatsContainer = view.findViewById(R.id.statusbar_information_display_blocks_chinesport);
        chinesportToggle = view.findViewById(R.id.chinesport_detailed_stats_button);
        chinesportSpeed = view.findViewById(R.id.chinesport_speed_box_value);
        chinesportPower = view.findViewById(R.id.chinesport_power_box_value);
        chinesportMode = view.findViewById(R.id.chinesport_mode_box_value);
        chinesportDirection = view.findViewById(R.id.chinesport_direction_box_value);
        chinesportTime = view.findViewById(R.id.chinesport_time_box_value);

        chinesportToggle.setOnClickListener(new View.OnClickListener() {

            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                if (chinesportStatsContainer.getVisibility() == View.VISIBLE) {
                    chinesportStatsContainer.setVisibility(View.GONE);
                } else {
                    chinesportStatsContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        Bundle arguments = getArguments();
        if (arguments!= null) {
            isLocalPlay = arguments.getBoolean("localPlay");
        }

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

                if (!ViewHelper.isTouchScreen(view.getContext())) {
                    //SET FOCUS ON BUTTON
                    toggleSwitchRoutepart.requestFocusFromTouch();
                    toggleSwitchRoutepart.requestFocus();
                }
            } else {
                view.setVisibility(View.GONE);
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
                        routePartsViewHolder = new RoutePartsViewHolder(view, videoPlayerViewModel, getViewLifecycleOwner());
                    }
                });
            }
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

    // FOR CHINESPORT
    private BroadcastReceiver motoLifeDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> motoLifeData = intent.getStringArrayListExtra("motoLifeData");

            if (!motoLifeData.get(0).equals("Speed")) {
                executeCommand(motoLifeData);
            } else {
                // Update UI here
                chinesportSpeed.setText(motoLifeData.get(0));
                chinesportPower.setText(motoLifeData.get(1));
                chinesportMode.setText(motoLifeData.get(2));
                chinesportDirection.setText(motoLifeData.get(3));
                chinesportTime.setText(motoLifeData.get(4));
            }

            chinesportSpeed.setText(motoLifeData.get(0));
            chinesportPower.setText(motoLifeData.get(1));
            chinesportMode.setText(motoLifeData.get(2));
            chinesportDirection.setText(motoLifeData.get(3));
            chinesportTime.setText(motoLifeData.get(4));
        }
    };

    private void executeCommand(ArrayList<String> motoLifeData) {
        if (motoLifeData.get(0) == "Stop") {
//            onStop();
        } else if (motoLifeData.get(0).matches("Jump[1-6]")) {

        } else if (motoLifeData.get(0) == "Pause") {

        } else if (motoLifeData.get(0) == "Spasm") {

        } else if (motoLifeData.get(0) == "Resume") {

        } else if (motoLifeData.get(0) == "End") {

        } else if (motoLifeData.get(0) == "StartLeg" || motoLifeData.get(0) == "StartArm") {

        }
    }

     // FOR CHINESPORT
    @Override
    public void onStart() {
        super.onStart();

        if (getContext() != null) {
            Intent serviceIntent = new Intent(getContext(), MQTTService.class);
            getContext().startService(serviceIntent);

            LocalBroadcastManager.getInstance(getContext()).registerReceiver(motoLifeDataReceiver,
                    new IntentFilter("com.videostreamtest.MQTT_DATA_UPDATE"));
        }
    }

     // FOR CHINESPORT
    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(motoLifeDataReceiver);
        super.onStop();

        if (getContext() != null) {
            Intent serviceIntent = new Intent(getContext(), MQTTService.class);
            getContext().stopService(serviceIntent);
        }
    }

//    // FOR CHINESPORT
//    private ServiceConnection serviceConnection = new ServiceConnection() {
//
//        /**
//         * Called when a connection to the Service has been established, with
//         * the {@link IBinder} of the communication channel to the
//         * Service.
//         *
//         * <p class="note"><b>Note:</b> If the system has started to bind your
//         * client app to a service, it's possible that your app will never receive
//         * this callback. Your app won't receive a callback if there's an issue with
//         * the service, such as the service crashing while being created.
//         *
//         * @param name    The concrete component name of the service that has
//         *                been connected.
//         * @param service The IBinder of the Service's communication channel,
//         *                which you can now make calls on.
//         */
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            MQTTService.LocalBinder binder = (MQTTService.LocalBinder) service;
//            mqttService = binder.getService();
//            isBound = true;
//
//            fetchData();
//        }
//
//        /**
//         * Called when a connection to the Service has been lost.  This typically
//         * happens when the process hosting the service has crashed or been killed.
//         * This does <em>not</em> remove the ServiceConnection itself -- this
//         * binding to the service will remain active, and you will receive a call
//         * to {@link #onServiceConnected} when the Service is next running.
//         *
//         * @param name The concrete component name of the service whose
//         *             connection has been lost.
//         */
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            isBound = false;
//        }
//    };
//
//    // FOR CHINESPORT
//    @Override
//    public void onStart() {
//        super.onStart();
//        Intent serviceIntent = new Intent(getActivity(), MQTTService.class);
//        getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//    }
//
//    // FOR CHINESPORT
//    @Override
//    public void onStop() {
//        handler.removeCallbacks(dataUpdateRunnable);
//
//        if (isBound) {
//            getActivity().unbindService(serviceConnection);
//            isBound = false;
//        }
//
//        super.onStop();
//    }
//
//    // FOR CHINESPORT
//    private Runnable dataUpdateRunnable = new Runnable() {
//
//        /**
//         * When an object implementing interface <code>Runnable</code> is used
//         * to create a thread, starting the thread causes the object's
//         * <code>run</code> method to be called in that separately executing
//         * thread.
//         * <p>
//         * The general contract of the method <code>run</code> is that it may
//         * take any action whatsoever.
//         *
//         * @see Thread#run()
//         */
//        @Override
//        public void run() {
//            if (isBound && mqttService != null) {
//                try {
//                    ArrayList<String> data;
//                    if (mqttService.getData() == null || mqttService.getData().isEmpty()) {
//                        data = new ArrayList<>(Collections.nCopies(5, "0"));
//                    } else {
//                        data = mqttService.getData();
//                    }
//                    updateUI(data);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            handler.postDelayed(this, 5000);
//        }
//    };
//
//    // FOR CHINESPORT
//    private void updateUI(@NonNull ArrayList<String> data) {
//        String speed = data.get(0);
//        String power = data.get(1);
//        String mode = data.get(2);
//        String direction = data.get(3);
//        String time = data.get(4);
//
//        // Update UI elements for each component
//    }
//
//    // FOR CHINESPORT
//    private void fetchData() {
//        handler.post(dataUpdateRunnable);
//    }



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

        builder.setPositiveButton("Pause", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle Pause action
                pauseBike();
                Log.d(TAG, "Pause selected.");
            }
        });

        builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Handle Stop action
                shutdownBike();
                Log.d(TAG, "Stop selected.");
            }
        });

    }

    private void pauseBike()
    {
        //TODO: steps to take to pause bike
    }

    private void shutdownBike()
    {
        //TODO: steps to take to shutdown bike
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the BluetoothHelper with the fragment's context and the listener implementation
        bluetoothHelper = new BluetoothHelper(getActivity(), this);
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // Handle a found Bluetooth device
    }

    @Override
    public void onDevicePaired(BluetoothDevice device) {
        // Handle a paired Bluetooth device
    }
}
