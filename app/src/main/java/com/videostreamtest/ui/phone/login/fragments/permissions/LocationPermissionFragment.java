package com.videostreamtest.ui.phone.login.fragments.permissions;

import android.Manifest;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.login.LoginViewModel;

import org.jetbrains.annotations.NotNull;

public class LocationPermissionFragment extends Fragment {
    private LoginViewModel loginViewModel;
    private Button nextButton;
    private TextView locationPermissionTitle;
    private TextView locationPermissionText;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_permission, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        locationPermissionTitle = view.findViewById(R.id.login_location_permission_title);
        locationPermissionText = view.findViewById(R.id.login_location_permission_summary);
        nextButton = view.findViewById(R.id.login_goto_splashscreen_button);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean allPermissionsGranted = !result.containsValue(false);

                    if (allPermissionsGranted) {
                        gotoNextFragment();
                    } else {
                        Toast.makeText(getContext(), "Location permission is required to proceed.",
                                Toast.LENGTH_LONG).show();
                    }
                });

        nextButton.setOnClickListener(v -> {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        });

        locationPermissionText.setText(R.string.login_location_permission_summary);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nextButton.requestFocus();

        showCurrentStepInTitleView(locationPermissionTitle);
    }

    private void gotoNextFragment() {
        loginViewModel.addInstallationStep();
        NavHostFragment.findNavController(LocationPermissionFragment.this)
                .navigate(R.id.action_locationPermissionFragment_to_downloadSoundFragment, getArguments());
    }

    private void showCurrentStepInTitleView(final TextView titleView) {
        loginViewModel.getInstallationSteps().observe(getViewLifecycleOwner(), totalInstallationSteps -> {
            if (totalInstallationSteps != null) {
                loginViewModel.getCurrentInstallationStep().observe(getViewLifecycleOwner(), currentInstallationStep -> {
                    if (currentInstallationStep != null) {
                        titleView.setText(String.format(getString(R.string.login_proces_step_formatting),
                                currentInstallationStep,
                                totalInstallationSteps,
                                titleView.getText()));
                    }
                });
            }
        });
    }
}
