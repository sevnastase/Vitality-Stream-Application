package com.videostreamtest.ui.phone.downloads.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.helpers.PraxCallbacks;
import com.videostreamtest.helpers.WorkHelper;
import com.videostreamtest.ui.phone.downloads.DownloadsViewModel;
import com.videostreamtest.workers.download.DownloadAllMovieImagesServiceWorker;
import com.videostreamtest.workers.download.DownloadAssetsServiceWorker;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadAssetsFragment extends Fragment {
    private final static String TAG = DownloadAssetsFragment.class.getSimpleName();
    private DownloadsViewModel downloadsViewModel;

    private Button nextButton;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private SeekBar progressBar;

    private AtomicBoolean isNavigating = new AtomicBoolean(false);

    private String apikey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download_assets, container, false);

        Log.d(TAG, "gregment started");

        downloadsViewModel = new ViewModelProvider(requireActivity()).get(DownloadsViewModel.class);

        titleTextView = view.findViewById(R.id.download_assets_title_textview);
        descriptionTextView = view.findViewById(R.id.download_assets_description_textview);
        nextButton = view.findViewById(R.id.login_goto_synchronisation_view_button);
        progressBar = view.findViewById(R.id.download_assets_seekbar);

        apikey = view.getContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey", "");

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleTextView.setText(R.string.downloading_assets_title);
        showCurrentStepInTitleView(titleTextView);
        if (downloadAssets()) {
            progressBar.setVisibility(View.VISIBLE);
            listeningLiveData();
        } else {
            descriptionTextView.setText(R.string.downloaded_files_already_present);
            nextButton.setVisibility(View.VISIBLE);
            gotoNextFragment();
        }
    }

    private void gotoNextFragment() {
        if (!isNavigating.compareAndSet(false, true)) return;
        try {
            downloadsViewModel.addInstallationStep();
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_downloadAssetsFragment_to_downloadFlagsFragment, getArguments());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
            isNavigating.set(false);
        }
    }

    private boolean downloadAssets() {
        WorkHelper.enqueueWork(
                DownloadAssetsServiceWorker.class,
                new Data.Builder().putString("apikey", apikey).build(),
                (AppCompatActivity) getActivity(), // safe, we only use AppCompatActivity
                new PraxCallbacks.OnWorkerFinishedCallback() {
                    @Override
                    public void run(WorkInfo.State resultState, Data outputData) {
                        Activity activity = getActivity();
                        if (activity == null) return;

                        if (resultState.equals(WorkInfo.State.FAILED) || resultState.equals(WorkInfo.State.CANCELLED)) {
                            activity.runOnUiThread(() -> {
                                descriptionTextView.setText("Download failed. Please restart your device. If the issue persists, contact us at service@praxtour.nl");
                            });
                        }

                        if (resultState.equals(WorkInfo.State.SUCCEEDED)) {
                            activity.runOnUiThread(() -> {
                                gotoNextFragment();
                            });
                        }
                    }
                },
                null
        );

        return true;
    }

    private void listeningLiveData() {
        downloadsViewModel.getCurrentDownloadTypeInformation("assets").observe(getViewLifecycleOwner(), generalDownloadTracker -> {
            if (generalDownloadTracker != null) {
                descriptionTextView.setText(getString(R.string.download_general_message) + generalDownloadTracker.getDownloadCurrentFile());
                progressBar.setMax(Math.toIntExact(generalDownloadTracker.getDownloadTypeTotal()));
                progressBar.setProgress(generalDownloadTracker.getDownloadTypeCurrent());
                if (generalDownloadTracker.getDownloadCurrentFile().equalsIgnoreCase("done")) {
                    nextButton.setVisibility(View.VISIBLE);
                    gotoNextFragment();
                }
            }
        });
    }

    private void showCurrentStepInTitleView(final TextView titleView) {
        downloadsViewModel.getInstallationSteps().observe(getViewLifecycleOwner(), totalInstallationSteps -> {
            if (totalInstallationSteps != null) {
                downloadsViewModel.getCurrentInstallationStep().observe(getViewLifecycleOwner(), currentInstallationStep -> {
                    if (currentInstallationStep != null) {
                        titleView.setText(String.format(getString(R.string.login_proces_step_formatting), currentInstallationStep, totalInstallationSteps, titleView.getText()));
                    }
                });
            }
        });
    }
}
