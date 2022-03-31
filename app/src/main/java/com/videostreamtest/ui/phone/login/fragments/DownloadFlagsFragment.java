package com.videostreamtest.ui.phone.login.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.login.LoginViewModel;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.workers.download.DownloadFlagsServiceWorker;

import org.jetbrains.annotations.NotNull;

public class DownloadFlagsFragment extends Fragment {
    private LoginViewModel loginViewModel;
    private String apikey;

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
        titleView.setText(R.string.download_flags_title);
        showCurrentStepInTitleView(titleView);
        if (downloadFlags()) {
            downloadProgressbar.setVisibility(View.VISIBLE);
            listeningLiveData();
        } else {
            descriptionView.setText(R.string.downloaded_files_already_present);
            nextButton.setVisibility(View.VISIBLE);
        }
    }

    private void initButtonClickListener() {
        nextButton.setOnClickListener((onClickedView) -> {
            gotoNextFragment();
        });
    }

    private void startMainActivity() {
        Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
        startActivity(splashScreenActivity);
        getActivity().finish();
    }

    private void gotoNextFragment() {
        loginViewModel.addInstallationStep();
        NavHostFragment.findNavController(DownloadFlagsFragment.this)
                .navigate(R.id.action_downloadFlagsFragment_to_downloadMovieSupportImagesFragment, getArguments());
    }

    private boolean downloadFlags() {
        if (!DownloadHelper.isFlagsLocalPresent(getActivity())) {
            Constraints constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest downloadFlagsWorker = new OneTimeWorkRequest.Builder(DownloadFlagsServiceWorker.class)
                    .setConstraints(constraint)
                    .setInputData(new Data.Builder().putString("apikey", apikey).build())
                    .build();

            WorkManager.getInstance(getActivity())
                    .beginUniqueWork("login-download-flags", ExistingWorkPolicy.KEEP, downloadFlagsWorker)
                    .enqueue();
            return true;
        }
        return false;
    }

    private void listeningLiveData() {
        loginViewModel.getCurrentDownloadTypeInformation("flags").observe(getViewLifecycleOwner(), generalDownloadTracker -> {
            if (generalDownloadTracker != null) {
                descriptionView.setText(getString(R.string.download_general_message)+generalDownloadTracker.getDownloadCurrentFile());
                downloadProgressbar.setMax(generalDownloadTracker.getDownloadTypeTotal());
                downloadProgressbar.setProgress(generalDownloadTracker.getDownloadTypeCurrent());
                if (generalDownloadTracker.getDownloadCurrentFile().equalsIgnoreCase("done")) {
                    nextButton.setVisibility(View.VISIBLE);
                    gotoNextFragment();
                }
            }
        });
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
