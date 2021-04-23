package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;
import com.videostreamtest.utils.DistanceLookupTable;

public class PraxFilmStatusBarFragment extends Fragment {
    private static final String TAG = PraxFilmStatusBarFragment.class.getSimpleName();
    private VideoPlayerViewModel videoPlayerViewModel;

    //STATEFUL ELEMENTS
    private boolean onDragProgressbar = false;

    //Elements of the fragment to fill
    private TextView statusbarMovieTitle;
    private TextView statusbarMovieRpm;
    private TextView statusbarVolumeIndicator;
    private Chronometer stopwatchCurrentRide;
    private SeekBar progressBar;
    private ImageButton volumeUp;
    private ImageButton volumeDown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_praxfilm_statusbar, container, false);

        statusbarMovieTitle = view.findViewById(R.id.statusbar_praxfilm_movie_title);
        statusbarMovieRpm = view.findViewById(R.id.statusbar_praxfilm_movie_speed);
        stopwatchCurrentRide = view.findViewById(R.id.statusbar_praxfilm_stopwatch_current_ride);
        progressBar = view.findViewById(R.id.statusbar_praxfilm_seekbar_movie);
        statusbarVolumeIndicator = view.findViewById(R.id.statusbar_praxfilm_volume_indicator);
        volumeUp = view.findViewById(R.id.statusbar_praxfilm_volume_button_up);
        volumeDown = view.findViewById(R.id.statusbar_praxfilm_volume_button_down);

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
                int framesPerSecond = 30;
                int frameNumber = (newProgress/1000) * framesPerSecond;
                VideoplayerActivity.getInstance().goToFrameNumber(frameNumber);
            }
        });

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
            } else {
                view.setVisibility(View.GONE);
                stopwatchCurrentRide.stop();
            }
        });

        //ROUTE IS PAUSED STATUS BUT VIEW IS STILL VISIBLE
        videoPlayerViewModel.getPlayerPaused().observe(getViewLifecycleOwner(), isPaused -> {
            if (isPaused) {
                stopwatchCurrentRide.stop();
            } else {
                stopwatchCurrentRide.start();
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
                statusbarVolumeIndicator.setText(""+(int) (volumeLevel*100));
                volumeUp.setOnClickListener(clickedView -> {
                    videoPlayerViewModel.setVolumeLevel(volumeLevel+0.1f);
                });
                volumeDown.setOnClickListener(clickedView -> {
                    videoPlayerViewModel.setVolumeLevel(volumeLevel-0.1f);
                });
            }
        });
    }
}
