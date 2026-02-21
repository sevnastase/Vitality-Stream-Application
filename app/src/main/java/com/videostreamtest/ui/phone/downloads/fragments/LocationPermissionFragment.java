package com.videostreamtest.ui.phone.downloads.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.videostreamtest.R;
import com.videostreamtest.ui.phone.downloads.DownloadsViewModel;
import com.videostreamtest.ui.phone.splash.SplashActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LocationPermissionFragment extends Fragment {
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private DownloadsViewModel downloadsViewModel;

    private Button nextButton;
    private TextView locationpermissionTitle;
    private TextView locationpermissionText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_permission, container, false);
        downloadsViewModel = new ViewModelProvider(requireActivity()).get(DownloadsViewModel.class);

        locationpermissionTitle = view.findViewById(R.id.login_location_permission_title);
        locationpermissionText = view.findViewById(R.id.login_location_permission_summary);
        nextButton = view.findViewById(R.id.login_goto_splashscreen_button);

        List<String> requestPermissions = getLocationPermissionsForRequest();

        if (requestPermissions!= null && requestPermissions.size()==0) {
            gotoNextFragment();
        }

        nextButton.setOnClickListener((onClickedView) -> {
            if (requestPermissions.size()>0) {
                String[] perms = requestPermissions.toArray(new String[0]);
                requestPermissions(perms,
                        LOCATION_PERMISSION_REQUEST_CODE);

//                if (ContextCompat.checkSelfPermission(
//                        getContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
//                        PackageManager.PERMISSION_GRANTED) {
//                    // You can use the API that requires the permission.
////                    performAction(...);
//                    Log.d(getClass().getSimpleName(), "PERM GRANTED");
//                } else if (shouldShowRequestPermissionRationale(perms[0])) {
//                    // In an educational UI, explain to the user why your app requires this
//                    // permission for a specific feature to behave as expected. In this UI,
//                    // include a "cancel" or "no thanks" button that allows the user to
//                    // continue using your app without granting the permission.
//                    Toast.makeText(getContext(), "WHAT HAPPEND", Toast.LENGTH_LONG).show();
//                } else {
//                    // You can directly ask for the permission.
//                    // The registered ActivityResultCallback gets the result of this request.
//                    requestPermissionLauncher.launch(
//                            Manifest.permission.ACCESS_FINE_LOCATION);
//                }
            } else {
                gotoNextFragment();
            }
        });

        locationpermissionText.setText(R.string.login_location_permission_summary);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nextButton.requestFocus();

        showCurrentStepInTitleView(locationpermissionTitle);
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Toast.makeText(getContext(), "Granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Snackbar.make(this.getView(), R.string.action_register_user,
                            Snackbar.LENGTH_INDEFINITE).setAction(R.string.cast_tracks_chooser_dialog_ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request the permission
                            ActivityCompat.requestPermissions(LocationPermissionFragment.this.requireActivity(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    }).show();
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        Log.d(getClass().getSimpleName(), "RequestCode: "+requestCode+ " , grantResults: "+grantResults.length);
        if (grantResults.length>0) {
            Log.d(getClass().getSimpleName(), "First GrantResult: "+grantResults[0]);
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            int allGranted = 0;
            for (String permission : permissions) {
                Log.d(getClass().getSimpleName(), "Requested Permission: "+permission);
            }
            for (int grantResult : grantResults) {
                allGranted += grantResult;
                Log.d(getClass().getSimpleName(), "Requested GrantResult: "+grantResult);
            }
            if (allGranted == 0) {
                gotoNextFragment();
            } else {
                //DO NOTHING
            }
        }
    }

    private List<String> getLocationPermissionsForRequest() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        List<String> permissionRequests = new ArrayList<>();
        for (final String permission : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(
                    getActivity(), permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissionRequests.add(permission);
            }
        }

        return permissionRequests;
    }

    private void startMainActivity() {
        Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
        startActivity(splashScreenActivity);
        getActivity().finish();
    }

    private void gotoNextFragment() {
        downloadsViewModel.addInstallationStep();
        NavHostFragment.findNavController(LocationPermissionFragment.this)
                .navigate(R.id.action_locationPermissionFragment_to_downloadSoundFragment, getArguments());
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
