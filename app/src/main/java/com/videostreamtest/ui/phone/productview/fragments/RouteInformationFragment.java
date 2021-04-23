package com.videostreamtest.ui.phone.productview.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.workers.DownloadServiceWorker;

public class RouteInformationFragment extends Fragment {
    private static final String TAG = RouteInformationFragment.class.getSimpleName();
    private ProductViewModel productViewModel;
    private TextView routeInfoTitleText;
    private TextView routeInfoTitleProgressText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_routeinformation, container, false);

        routeInfoTitleText = view.findViewById(R.id.routeinfo_title);
        routeInfoTitleProgressText = view.findViewById(R.id.routeinfo_progress_message);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        /**
         * Description: retrieve Movie string JSON object and make it an object
         *  - check if download is already running based on movieId
         *  - if not then start new download
         *  - if so then show download progress
         */

        String movieJsonString = "";
        Bundle bundle = this.getArguments();
        if (bundle!= null) {
            movieJsonString = bundle.get("movieObject").toString();
        }

        final Movie selectedMovie = new GsonBuilder().create().fromJson(movieJsonString, Movie.class);

        routeInfoTitleText.setText(selectedMovie.getMovieTitle());

        productViewModel.getDownloadStatus(selectedMovie.getId()).observe(getViewLifecycleOwner(), standAloneDownloadStatus -> {
            if (standAloneDownloadStatus != null) {
                routeInfoTitleProgressText.setText("Download Progress: " + standAloneDownloadStatus.getDownloadStatus().intValue() + "%");
            }
        });

        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("movie-"+selectedMovie.getId())
                .observe(requireActivity(), workInfos -> {
                    Log.d(TAG, "OUTPUT WORKERINFO FOR MOVIE WITH ID "+selectedMovie.getId()+" : "+workInfos.size());

                    if (!(workInfos.size() > 0)) {
                        if (DownloadHelper.canFileBeCopied(requireContext(),
                                (selectedMovie.getMapFileSize()+selectedMovie.getSceneryFileSize()+selectedMovie.getMovieFileSize()))) {
                            Log.d(TAG, "OUTPUT CAN BE DOWNLOADED FOR MOVIE WITH ID "+selectedMovie.getId()+" : "+selectedMovie.getMovieTitle());
                            routeInfoTitleProgressText.setText("Enough space to download routefilm.");

                            //START DownloadServiceWorker
                            //Setup Constraint
                            Constraints constraint = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build();

                            //Setup DownloadServiceWorker
                            Data.Builder mediaDownloader = new Data.Builder();
                            mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING",  new GsonBuilder().create().toJson(selectedMovie, Movie.class));
                            OneTimeWorkRequest downloadMediaRequest = new OneTimeWorkRequest.Builder(DownloadServiceWorker.class)
                                    .setConstraints(constraint)
                                    .setInputData(mediaDownloader.build())
                                    .addTag("movie-"+selectedMovie.getId())
                                    .build();

                            //Start downloading
                            WorkManager.getInstance(requireContext())
                                    .enqueueUniqueWork("movie-"+selectedMovie.getId(), ExistingWorkPolicy.KEEP, downloadMediaRequest);
                        } else {
                            routeInfoTitleProgressText.setText("Not enough space to download routefilm.");
                        }
                    }
                });

    }
}
