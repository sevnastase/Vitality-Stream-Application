package com.videostreamtest.ui.phone.productpicker.fragments.ble;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;

import org.jetbrains.annotations.NotNull;

public class NoPermissionsMessageFragment extends Fragment {
    private static final String TAG = NoPermissionsMessageFragment.class.getSimpleName();

    private TextView errorMessage;

    private Button retryButton;
    private Button showSettingsButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permissions_failed, container, false);
        errorMessage = view.findViewById(R.id.permission_error_message);
        retryButton = view.findViewById(R.id.permission_error_message_retry_button);
        showSettingsButton = view.findViewById(R.id.permission_error_message_show_settings_button);

        initVisualsGeneral();
        initTextBasedOnNeededPermission();

        //VISUALS
        if (ViewHelper.isTouchScreen(getActivity())) {
            final Drawable border = getActivity().getDrawable(R.drawable.imagebutton_red_border);
            retryButton.setBackground(border);
            showSettingsButton.setBackground(border);
        } else {
            retryButton.setOnFocusChangeListener((onFocusedView, hasFocus) -> {
                if (hasFocus) {
                    final Drawable border = getActivity().getDrawable(R.drawable.imagebutton_red_border);
                    retryButton.setBackground(border);
                } else {
                    retryButton.setBackground(null);
                }
            });
            showSettingsButton.setOnFocusChangeListener((onFocusedView, hasFocus) -> {
                if (hasFocus) {
                    final Drawable border = getActivity().getDrawable(R.drawable.imagebutton_red_border);
                    showSettingsButton.setBackground(border);
                } else {
                    showSettingsButton.setBackground(null);
                }
            });
        }
        //ACTIONS
        retryButton.setOnClickListener((onClickView) -> {
            if (hasBluetoothPermissions()) {
                goToBleDeviceInformationFragment();
            } else {
                Toast.makeText(getActivity(), getString(R.string.ble_device_lack_permission_message), Toast.LENGTH_LONG).show();
            }
        });

        showSettingsButton.setOnClickListener((onClickView) -> {
            showSettingsVisual();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showSettingsButton.requestFocus();
    }

    private void initVisualsGeneral() {
        final Drawable border = getActivity().getDrawable(R.drawable.imagebutton_red_border);
        retryButton.setBackground(border);
        retryButton.setBackground(null);
        showSettingsButton.setBackground(border);
        showSettingsButton.setBackground(null);
    }

    private void initTextBasedOnNeededPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
            showSettingsButton.setText(getString(R.string.permission_settings_text_ble_permission));
            errorMessage.setText(toString().format(getString(R.string.permission_error_message), getString(R.string.permission_settings_text_ble_permission)));
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH permission.");
            showSettingsButton.setText(getString(R.string.permission_settings_text_ble_permission));
            errorMessage.setText(toString().format(getString(R.string.permission_error_message), getString(R.string.permission_settings_text_ble_permission)));
        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
            showSettingsButton.setText(getString(R.string.permission_settings_text_location_permission));
            errorMessage.setText(toString().format(getString(R.string.permission_error_message), getString(R.string.permission_settings_text_location_permission)));
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ) {
            Log.d(TAG, "No ACCESS_FINE_LOCATION permission.");
            showSettingsButton.setText(getString(R.string.permission_settings_text_location_permission));
        }
    }

    private void showSettingsVisual() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
//            startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS), 0);
            PermissionHelper.checkPermissions();
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH permission.");
//            startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS), 0);
            PermissionHelper.checkPermissions();
        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
//            startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS), 0);
            PermissionHelper.checkPermissions();
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ) {
            Log.d(TAG, "No ACCESS_FINE_LOCATION permission.");
            PermissionHelper.checkPermissions();
        }
    }

    private void goToBleDeviceInformationFragment() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        if (navController.getCurrentDestination().getId() == R.id.permissionErrorMessage) {
            navController.navigate(R.id.bleDeviceInformationBoxFragment);
        }
    }

    private boolean hasBluetoothPermissions() {
        boolean permissionsAcquired = true;
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
            Log.d(TAG, "No ACCES_COARSE_LOCATION permission.");
            permissionsAcquired = false;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH_ADMIN permission.");
            permissionsAcquired = false;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No BLUETOOTH permission.");
            permissionsAcquired = false;
        }
        return permissionsAcquired;
    }
}
