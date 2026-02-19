package com.videostreamtest.ui.phone.login.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.ui.phone.login.LoginViewModel;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.workers.SoundInformationServiceWorker;
import com.videostreamtest.workers.UpdateRegisteredMovieServiceWorker;
import com.videostreamtest.workers.UpdateRoutePartsServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductMovieLinksServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductsServiceWorker;
import com.videostreamtest.workers.synchronisation.SyncFlagsServiceWorker;
import com.videostreamtest.workers.synchronisation.SyncMovieFlagsServiceWorker;

import org.jetbrains.annotations.NotNull;

public class SyncDatabaseFragment extends Fragment {
    private LoginViewModel loginViewModel;
    private String apikey;
    private Bundle arguments;

    private TextView titleView;
    private TextView descriptionView;
    private SeekBar downloadProgressbar;
    private Button nextButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download_sound, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        apikey = view.getContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");

        titleView = view.findViewById(R.id.download_sound_status_title);
        descriptionView = view.findViewById(R.id.download_sound_status_summary);
        downloadProgressbar = view.findViewById(R.id.download_sound_status_progressbar);
        nextButton = view.findViewById(R.id.login_goto_synchronisation_view_button);

        initButtonClickListener();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        arguments = getArguments();
        titleView.setText(String.format(getString(R.string.login_sync_db_title)));
        descriptionView.setText(String.format(getString(R.string.login_sync_db_description)));
        showCurrentStepInTitleView(titleView);
        synchroniseDatabase();
        listeningLiveData();
    }

    private void listeningLiveData() {
        loginViewModel.getRoutefilms().observe(getViewLifecycleOwner(), routefilms -> {
            if (routefilms != null && routefilms.size()>0) {
                descriptionView.setText(String.format(getString(R.string.login_sync_db_successfull_message), routefilms.size()));
                determineNextStep();
            }
        });
    }

    private void synchroniseDatabase() {
        Data.Builder syncData = new Data.Builder();
        syncData.putString("apikey",  apikey);
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest productRequest = new OneTimeWorkRequest.Builder(ActiveProductsServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("products")
                .build();

        OneTimeWorkRequest productMovieRequest = new OneTimeWorkRequest.Builder(ActiveProductMovieLinksServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("productmovie-link")
                .build();

        OneTimeWorkRequest syncDatabaseWorkRequest = new OneTimeWorkRequest.Builder(UpdateRegisteredMovieServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("sync-database")
                .build();

        OneTimeWorkRequest productMoviePartsRequest = new OneTimeWorkRequest.Builder(UpdateRoutePartsServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("movieparts-link")
                .build();

        OneTimeWorkRequest productMovieSoundsRequest = new OneTimeWorkRequest.Builder(SoundInformationServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("moviesounds-sync")
                .build();

        OneTimeWorkRequest flagRequest = new OneTimeWorkRequest.Builder(SyncFlagsServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("flags-sync")
                .build();

        OneTimeWorkRequest movieflagRequest = new OneTimeWorkRequest.Builder(SyncMovieFlagsServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("movieflags-sync")
                .build();

        WorkManager.getInstance(getActivity())
                .beginWith(flagRequest)
                .then(movieflagRequest)
                .then(productMovieSoundsRequest)
                .then(productMoviePartsRequest)
                .then(productMovieRequest)
                .then(productRequest)
                .then(syncDatabaseWorkRequest)
                .enqueue();
    }

    private void initButtonClickListener() {
        nextButton.setOnClickListener((onClickedView) -> {
            determineNextStep();
        });
    }

    private void startMainActivity() {
        Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
        startActivity(splashScreenActivity);
        getActivity().finish();
    }

    private void gotoNextFragment() {
        loginViewModel.addInstallationStep();
        NavHostFragment.findNavController(SyncDatabaseFragment.this)
                .navigate(R.id.action_syncDatabaseFragment_to_downloadFlagsFragment, getArguments());
    }

    private void determineNextStep() {
        try {
            if (AccountHelper.isLocalPlay(getContext())) {
                gotoNextFragment();
            } else {
                startMainActivity();
            }
        } catch (Exception exception) {
            Log.e(getClass().getSimpleName(), exception.getLocalizedMessage());
        }

    }

    private void showCurrentStepInTitleView(final TextView titleView) {
        loginViewModel.getInstallationSteps().observe(getViewLifecycleOwner(), totalInstallationSteps -> {
            if (totalInstallationSteps != null) {
                loginViewModel.getCurrentInstallationStep().observe(getViewLifecycleOwner(), currentInstallationStep -> {
                    if (currentInstallationStep != null) {
                        titleView.setText(String.format(getString(R.string.login_proces_step_formatting), currentInstallationStep, totalInstallationSteps, titleView.getText()));
                    }
                });
            }
        });
    }
}
