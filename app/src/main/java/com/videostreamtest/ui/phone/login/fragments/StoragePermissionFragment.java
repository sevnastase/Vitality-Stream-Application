package com.videostreamtest.ui.phone.login.fragments;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.login.LoginViewModel;
import com.videostreamtest.ui.phone.splash.SplashActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StoragePermissionFragment extends Fragment {
    private final int STORAGE_PERMISSION_REQUEST_CODE = 1234;

    private LoginViewModel loginViewModel;

    private Button nextButton;
    private TextView storagepermissionText;
    private TextView storagepermissionTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_storage_permission, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        storagepermissionTitle = view.findViewById(R.id.login_storage_permission_title);
        storagepermissionText = view.findViewById(R.id.login_storage_permission_summary);
        nextButton = view.findViewById(R.id.login_goto_location_permission_button);

        List<String> requestPermissions = getStoragePermissionsForRequest();

        if(requestPermissions != null && requestPermissions.size()==0) {
//            NavHostFragment.findNavController(StoragePermissionFragment.this)
//                    .navigate(R.id.action_storagePermissionFragment_to_locationPermissionFragment, getArguments());
            gotoNextFragment();
        }

        nextButton.setOnClickListener((onClickedView) -> {
            if (requestPermissions.size() > 0) {
                String[] perms = requestPermissions.toArray(new String[0]);
                ActivityCompat.requestPermissions(
                        this.getActivity(),
                        perms,
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                gotoNextFragment();
//                if (getLocationPermissionsForRequest().size()>0) {
//                    NavHostFragment.findNavController(StoragePermissionFragment.this)
//                            .navigate(R.id.action_storagePermissionFragment_to_locationPermissionFragment, getArguments());
//                } else {
//                    Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
//                    startActivity(splashScreenActivity);
//                    getActivity().finish();
//                }
            }
        });

        storagepermissionText.setText(R.string.login_storage_permission_summary);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nextButton.requestFocus();

        showCurrentStepInTitleView(storagepermissionTitle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            int allGranted = 0;
            for (String permission : permissions) {
                Log.d(getClass().getSimpleName(), "Requested Permission: "+permission);
            }
            for (int grantResult : grantResults) {
                allGranted += grantResult;
                Log.d(getClass().getSimpleName(), "Requested GrantResult: "+grantResult);
            }
            if (allGranted == 0) {
//                if (getLocationPermissionsForRequest().size()>0) {
                    gotoNextFragment();
//                } else {
//                    Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
//                    startActivity(splashScreenActivity);
//                    getActivity().finish();
//                }
            } else {

            }
        }
    }

    private List<String> getStoragePermissionsForRequest() {
        List<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

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

    private List<String> getLocationPermissionsForRequest() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
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

    private void gotoNextFragment() {
        loginViewModel.addInstallationStep();
        NavHostFragment.findNavController(StoragePermissionFragment.this)
                .navigate(R.id.action_storagePermissionFragment_to_locationPermissionFragment, getArguments());
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
