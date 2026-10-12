package com.videostreamtest.ui.phone.downloads.fragments;

import android.Manifest;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.downloads.DownloadsViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BluetoothPermissionFragment extends Fragment {
    private static final String TAG = BluetoothPermissionFragment.class.getSimpleName();
    private final int STORAGE_PERMISSION_REQUEST_CODE = 1234;

    private DownloadsViewModel downloadsViewModel;

    private Button nextButton;
    private TextView bluetoothPermissionSummaryTextView;
    private TextView bluetoothPermissionTitleTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth_permission, container, false);
        downloadsViewModel = new ViewModelProvider(requireActivity()).get(DownloadsViewModel.class);

        bluetoothPermissionTitleTextView = view.findViewById(R.id.bluetooth_permission_title);
        bluetoothPermissionSummaryTextView = view.findViewById(R.id.bluetooth_permission_summary);
        nextButton = view.findViewById(R.id.goto_splash_button);

        List<String> requestPermissions = getBluetoothPermissionsToRequest();

        if (requestPermissions.isEmpty()) {
            goToNextFragment();
        }

        nextButton.setOnClickListener((onClickedView) -> {
            if (!requestPermissions.isEmpty()) {
                String[] perms = requestPermissions.toArray(new String[0]);
                requestPermissions(perms, STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                goToNextFragment();
            }
        });

        bluetoothPermissionSummaryTextView.setText(R.string.bluetooth_permission_summary);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nextButton.requestFocus();

        showCurrentStepInTitleView(bluetoothPermissionTitleTextView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            int allGranted = 0;
            for (String permission : permissions) {
                Log.d(getClass().getSimpleName(), "Requested Permission: "+permission);
            }
            for (int grantResult : grantResults) {
                allGranted -= grantResult; // minues because granted = 0; denied = -1
                Log.d(getClass().getSimpleName(), "Requested GrantResult: "+grantResult);
            }
            if (allGranted > 0) {
                goToNextFragment();
            }
        }
    }

    private List<String> getBluetoothPermissionsToRequest() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        ) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ) {
            Log.d(TAG, "No ACCESS_FINE_LOCATION permission then ASK, otherwise no popup is shown.");
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Log.d(TAG, "No BLUETOOTH permission.");
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            Log.d(TAG, "No BLUETOOTH_SCAN permission.");
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            Log.d(TAG, "No BLUETOOTH_CONNECT permission.");
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        return permissionsNeeded;
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

    private void goToNextFragment() {
        downloadsViewModel.addInstallationStep();
        NavHostFragment.findNavController(BluetoothPermissionFragment.this)
                .navigate(R.id.action_bluetoothPermissionFragment_to_downloadSoundFragment, getArguments());
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
