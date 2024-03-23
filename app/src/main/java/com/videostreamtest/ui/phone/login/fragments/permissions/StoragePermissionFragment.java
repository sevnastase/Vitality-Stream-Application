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

public class StoragePermissionFragment extends Fragment {
    private LoginViewModel loginViewModel;
    private Button nextButton;
    private TextView storagePermissionText;
    private TextView storagePermissionTitle;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_storage_permission, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        storagePermissionTitle = view.findViewById(R.id.login_storage_permission_title);
        storagePermissionText = view.findViewById(R.id.login_storage_permission_summary);
        nextButton = view.findViewById(R.id.login_goto_location_permission_button);

        nextButton.setOnClickListener(v -> {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE});
        });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean allPermissionsGranted = !result.containsValue(false);

                    if (allPermissionsGranted) {
                        gotoNextFragment();
                    } else {
                        Toast.makeText(getContext(), "Storage permission is required to proceed.",
                                Toast.LENGTH_LONG).show();
                    }
                });

        storagePermissionText.setText(R.string.login_storage_permission_summary);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nextButton.requestFocus();

        showCurrentStepInTitleView(storagePermissionTitle);
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
