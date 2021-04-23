package com.videostreamtest.ui.phone.videoplayer.fragments;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
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
    private Chronometer stopwatchCurrentRide;
    private RecyclerView statusbarRouteparts;
    private ImageButton speedUpButton;
    private ImageButton speedDownButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_praxspin_statusbar, container, false);

        statusbarMovieTitle = view.findViewById(R.id.statusbar_praxspin_movie_title);
        stopwatchCurrentRide = view.findViewById(R.id.statusbar_praxspin_stopwatch_current_ride);
        statusbarDistance = view.findViewById(R.id.statusbar_praxspin_movie_distance);
        statusbarTotalDistance = view.findViewById(R.id.statusbar_praxspin_movie_total_distance);
        statusbarRouteparts = view.findViewById(R.id.statusbar_praxspin_recyclerview_movieparts);

        //INIT VALUES
        stopwatchCurrentRide.setFormat(getString(R.string.videoplayer_chronometer_message));
        stopwatchCurrentRide.setBase(SystemClock.elapsedRealtime());

        speedIndicator = view.findViewById(R.id.statusbar_praxspin_speed_indicator);
        speedUpButton = view.findViewById(R.id.statusbar_praxspin_speed_button_up);
        speedDownButton = view.findViewById(R.id.statusbar_praxspin_speed_button_down);

        statusbarRouteparts.setHasFixedSize(true);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        statusbarRouteparts.setLayoutManager(layoutManager);

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

        videoPlayerViewModel.getKmhData().observe(getViewLifecycleOwner(), kmhData -> {
            if (kmhData != null) {
                speedIndicator.setText(""+kmhData+" kmh");
                speedUpButton.setOnClickListener(clickedView ->{
                    videoPlayerViewModel.setKmhData(kmhData+2);
                });
                speedDownButton.setOnClickListener(clickedView -> {
                    videoPlayerViewModel.setKmhData(kmhData-2);
                });
            }
        });
    }
}
