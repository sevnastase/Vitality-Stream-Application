package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.os.Bundle;
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

    //MOVIE PARTS
    private LinearLayout moviePartsLayout;

    //VOLUME
    private TextView statusbarVolumeIndicator;
    private ImageButton volumeUp;
    private ImageButton volumeDown;

    private ImageButton toggleSwitchRoutepart;

    //ROUTE PROGRESS
    private SeekBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_praxfit_statusbar, container, false);

        //Link items to lay-out
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

        statusbarRouteparts.setHasFixedSize(true);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRouteparts.setLayoutManager(layoutManager);

        toggleSwitchRoutepart.setOnClickListener(clickedView -> {
            toggleMoviePartsVisibility();
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
                        routePartsAdapter = new RoutePartsAdapter(routeparts);
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

    }

    private void toggleMoviePartsVisibility() {
        if (moviePartsLayout.getVisibility() == View.GONE) {
            moviePartsLayout.setVisibility(View.VISIBLE);
            if (moviePartsLayout.getChildCount()>0) {
                moviePartsLayout.getChildAt(0).requestFocus();
            }
        } else {
            moviePartsLayout.setVisibility(View.GONE);
        }
    }
}
