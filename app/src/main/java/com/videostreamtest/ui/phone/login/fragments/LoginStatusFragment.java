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
            loginViewModel.setCurrentInstallationStep(0);
            loginStatusTitle.setText(String.format(getString(R.string.login_success_summary_title), arguments.getString("username")));
            int activeProductsCount = arguments.getInt("active-products-count", -1);
            if (activeProductsCount == 0 || activeProductsCount == -1) {
                loginStatusText.setText(R.string.login_no_products);
                logout();
                nextButton.setText(R.string.retry_permission_check_button);
                nextButton.setOnClickListener((onClickedView) -> {
                    NavHostFragment.findNavController(LoginStatusFragment.this)
                            .navigate(R.id.action_loginStatusFragment_to_usernameFragment);
                });
            } else {
                switch (arguments.getString("account-type", "").toLowerCase()) {
                    case "standalone":
                        loginStatusText.setText(R.string.login_status_summary_standalone);
                        nextButton.setOnClickListener((onClickedView) -> {
                            if (getStoragePermissionsForRequest().size() > 0) {
                                NavHostFragment.findNavController(LoginStatusFragment.this)
                                        .navigate(R.id.action_loginStatusFragment_to_storagePermissionFragment, arguments);
                            } else if (getLocationPermissionsForRequest().size() > 0) {
                                NavHostFragment.findNavController(LoginStatusFragment.this)
                                        .navigate(R.id.action_loginStatusFragment_to_locationPermissionFragment, arguments);
                            } else {
                                Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
                                startActivity(splashScreenActivity);
                                getActivity().finish();
                            }
                            loginViewModel.addInstallationStep();
                        });
                        loginViewModel.setInstallationSteps(7);
                        break;
                    case "streaming":
                        loginStatusText.setText(getString(R.string.login_status_summary_streaming));

                        nextButton.setOnClickListener((onClickedView) -> {
                            if (getLocationPermissionsForRequest().size() > 0) {
                                NavHostFragment.findNavController(LoginStatusFragment.this)
                                        .navigate(R.id.action_loginStatusFragment_to_locationPermissionFragment, arguments);
                            } else {
                                NavHostFragment.findNavController(LoginStatusFragment.this)
                                        .navigate(R.id.action_loginStatusFragment_to_downloadSoundFragment, arguments);
                            }
                            loginViewModel.addInstallationStep();
                        });

                        loginViewModel.setInstallationSteps(3);
                        break;
                    case "hybrid":
                        loginStatusText.setText(R.string.login_status_summary_standalone);
                        nextButton.setOnClickListener((onClickedView) -> {
                            if (getStoragePermissionsForRequest().size() > 0) {
                                NavHostFragment.findNavController(LoginStatusFragment.this)
                                        .navigate(R.id.action_loginStatusFragment_to_storagePermissionFragment, arguments);
                            } else if (getLocationPermissionsForRequest().size() > 0) {
                                NavHostFragment.findNavController(LoginStatusFragment.this)
                                        .navigate(R.id.action_loginStatusFragment_to_locationPermissionFragment, arguments);
                            } else {
                                Intent splashScreenActivity = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
                                startActivity(splashScreenActivity);
                                getActivity().finish();
                            }
                            loginViewModel.addInstallationStep();
                        });
                        loginViewModel.setInstallationSteps(7);
                        break;
                    default:
                        failedLogin("Account type error", "Contact your distributor.");
                }
            }
        } else {
            failedLogin(getString(R.string.login_failed), getString(R.string.login_message_failed_description));
        }
        nextButton.requestFocus();
    }

    private void failedLogin(String title, String text) {
        loginViewModel.setPassword("");
        loginStatusTitle.setTextColor(Color.RED);
        loginStatusTitle.setText(title);

        loginStatusText.setText(text);
        logout();
        nextButton.setText(R.string.retry_permission_check_button);
        nextButton.setOnClickListener((onClickedView) -> {
            NavHostFragment.findNavController(LoginStatusFragment.this)
                    .navigate(R.id.action_loginStatusFragment_to_usernameFragment);
        });
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

    private void logout() {
        SharedPreferences myPreferences = requireActivity().getApplication().getSharedPreferences("app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.clear();
        editor.commit();

        loginViewModel.deleteAllKnownConfigurations();
    }
}
