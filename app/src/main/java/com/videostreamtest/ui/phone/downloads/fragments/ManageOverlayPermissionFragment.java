package com.videostreamtest.ui.phone.downloads.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.downloads.DownloadsViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This permission is needed for start-on-boot functionality.
 */
public class ManageOverlayPermissionFragment extends Fragment {
    private final int STORAGE_PERMISSION_REQUEST_CODE = 1234;

    private DownloadsViewModel downloadsViewModel;

    private Button nextButton;
    private TextView permissionTitleTextView;

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (getContext() != null) {
                    if (Settings.canDrawOverlays(getContext())) {
                        gotoNextFragment();
                    } else {
                        // user did not grant permission
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_overlay_permission, container, false);
        downloadsViewModel = new ViewModelProvider(requireActivity()).get(DownloadsViewModel.class);

        permissionTitleTextView = view.findViewById(R.id.manage_overlay_permission_title);
        nextButton = view.findViewById(R.id.goto_location_permission_button);

        if (Settings.canDrawOverlays(view.getContext())) gotoNextFragment();

        nextButton.setOnClickListener((onClickedView) -> {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + view.getContext().getPackageName())
            );

            overlayPermissionLauncher.launch(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nextButton.requestFocus();

        showCurrentStepInTitleView(permissionTitleTextView);
    }

    private void gotoNextFragment() {
        downloadsViewModel.addInstallationStep();
        NavHostFragment.findNavController(ManageOverlayPermissionFragment.this)
                .navigate(R.id.action_manageOverlayPermissionFragment_to_storagePermissionFragment, getArguments());
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
