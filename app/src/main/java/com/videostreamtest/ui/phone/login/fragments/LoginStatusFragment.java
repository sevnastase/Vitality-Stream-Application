package com.videostreamtest.ui.phone.login.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
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
import com.videostreamtest.ui.phone.login.LoginViewModel;
import com.videostreamtest.ui.phone.splash.SplashActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LoginStatusFragment extends Fragment {
    private LoginViewModel loginViewModel;

    private Button nextButton;
    private TextView loginStatusTitle;
    private TextView loginStatusText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_status, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        loginStatusTitle = view.findViewById(R.id.login_status_title);
        loginStatusText = view.findViewById(R.id.login_status_summary);
        nextButton = view.findViewById(R.id.login_goto_permission_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments.getBoolean("authorized", false)) {
            loginStatusTitle.setText(String.format("Succesfully logged in as %s", arguments.getString("username")));
            int activeProductsCount = arguments.getInt("active-products-count", -1);
            if (activeProductsCount == 0 || activeProductsCount == -1) {
                loginStatusText.setText("There are no active product subscriptions present.\nPlease register for any of our products.");
                logout();
                nextButton.setText("Retry");
                nextButton.setOnClickListener((onClickedView) -> {
                    NavHostFragment.findNavController(LoginStatusFragment.this)
                            .navigate(R.id.action_loginStatusFragment_to_usernameFragment);
                });
            } else {
                loginStatusText.setText("Before first time use you need to accept the following permissions:\n\n");
                if (arguments.getString("account-type", "").equals("standalone")) {
                    loginStatusText.setText(loginStatusText.getText() + "> Storage permission: for saving movies on the harddisk.\n");
                    loginStatusText.setText(loginStatusText.getText() + "> Location permission: for Bluetooth sensors.\n\n");
                    loginStatusText.setText(loginStatusText.getText() + "Please proceed to accept the permissions.");
                    nextButton.setOnClickListener((onClickedView) -> {
                        if (getStoragePermissionsForRequest().size() > 0) {
                            NavHostFragment.findNavController(LoginStatusFragment.this)
                                    .navigate(R.id.action_loginStatusFragment_to_storagePermissionFragment);
                        } else if (getLocationPermissionsForRequest().size() > 0) {
                            NavHostFragment.findNavController(LoginStatusFragment.this)
                                    .navigate(R.id.action_loginStatusFragment_to_locationPermissionFragment);
                        } else {
                            Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
                            startActivity(splashScreenActivity);
                            getActivity().finish();
                        }

                    });
                }
                if (arguments.getString("account-type", "").equals("streaming")) {
                    loginStatusText.setText(loginStatusText.getText() + "> Location permission - for Bluetooth sensors.\n\n");
                    loginStatusText.setText(loginStatusText.getText() + "Please proceed to accept the permissions.");

                    nextButton.setOnClickListener((onClickedView) -> {
                        if (getLocationPermissionsForRequest().size() > 0) {
                            NavHostFragment.findNavController(LoginStatusFragment.this)
                                    .navigate(R.id.action_loginStatusFragment_to_locationPermissionFragment);
                        } else {
                            Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
                            startActivity(splashScreenActivity);
                            getActivity().finish();
                        }
                    });
                }
            }
        } else {
            loginViewModel.setPassword("");
            loginStatusTitle.setTextColor(Color.RED);
            loginStatusTitle.setText("Failed to login.");

            loginStatusText.setText("Username and/or password are wrong.\nPlease try again.");
            logout();
            nextButton.setText("Retry");
            nextButton.setOnClickListener((onClickedView) -> {
                NavHostFragment.findNavController(LoginStatusFragment.this)
                        .navigate(R.id.action_loginStatusFragment_to_usernameFragment);
            });
        }
        nextButton.requestFocus();
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
            permissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
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

    private void logout() {
        SharedPreferences myPreferences = requireActivity().getApplication().getSharedPreferences("app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.clear();
        editor.commit();

        loginViewModel.deleteAllKnownConfigurations();
    }
}
