package com.videostreamtest.ui.phone.productview.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

public class RouteDownloadInformationFragment extends Fragment {
    private static final String TAG = RouteDownloadInformationFragment.class.getSimpleName();
    private ProductViewModel productViewModel;

    private TextView routeInfoTitleText;
    private TextView routeInfoTitleProgressText;
    private ImageButton backButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_routeinformation, container, false);

        routeInfoTitleText = view.findViewById(R.id.routeinfo_title);
        routeInfoTitleProgressText = view.findViewById(R.id.routeinfo_progress_message);
        backButton = view.findViewById(R.id.routeinfo_back_button);

        backButton.requestFocus();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().popBackStackImmediate();//popBackStack() is also possible. with a tag name also! See doc reference.
            }
        });

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
        routeInfoTitleProgressText.setText("Download is pending.");
        productViewModel.getDownloadStatus(selectedMovie.getId()).observe(getViewLifecycleOwner(), standAloneDownloadStatus -> {
            if (standAloneDownloadStatus != null) {
                String status = "Download is pending.";
                if (standAloneDownloadStatus.getDownloadStatus() == -1) {
                    status = "Download is in queue";
                }
                if (standAloneDownloadStatus.getDownloadStatus() == -2) {
                    status = "Download impossible, not enough space to download.";
                }
                if (standAloneDownloadStatus.getDownloadStatus() >0) {
                    status = "Download Progress: " + standAloneDownloadStatus.getDownloadStatus().intValue() + "%";
                }

                routeInfoTitleProgressText.setText(status);
                if (standAloneDownloadStatus.getDownloadStatus() == 100) {
                    getParentFragmentManager().popBackStackImmediate();
                }
            }
        });
    }
}
