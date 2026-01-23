package com.videostreamtest.ui.phone.login.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.ui.phone.listeners.PraxFormOnEditorActionListener;
import com.videostreamtest.ui.phone.login.LoginViewModel;
import com.videostreamtest.workers.synchronisation.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.LoginServiceWorker;

import org.jetbrains.annotations.NotNull;

public class PasswordFragment extends Fragment {
    private static final String TAG = PasswordFragment.class.getSimpleName();
    private LoginViewModel loginViewModel;

    private TextView passwordTitle;
    private EditText passwordInput;
    private EditText usernameInsertedInput;
    private Button nextButton;
    private Button previousButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_password, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        passwordTitle = view.findViewById(R.id.login_insert_password_title);
        passwordInput = view.findViewById(R.id.login_insert_password_input);
        usernameInsertedInput = view.findViewById(R.id.login_show_inserted_username);
        nextButton = view.findViewById(R.id.login_goto_login_result_button);
        previousButton = view.findViewById(R.id.login_goto_username_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loginViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            if (username != null && !username.isEmpty()) {
                usernameInsertedInput.setText(username);
            }
        });

        loginViewModel.getPassword().observe(getViewLifecycleOwner(), password -> {
            if (password != null) {
                if (password.isEmpty()) {
                    passwordInput.getText().clear();
                }
            }
        });

        previousButton.setOnClickListener((onClickedView)-> {
            passwordInput.getText().clear();
            NavHostFragment.findNavController(PasswordFragment.this)
                    .navigate(R.id.action_passwordFragment_to_usernameFragment);
        });

        nextButton.setOnClickListener((onClickedView)-> {
            if (passwordInput.getText().length() > 0) {
                loginViewModel.setPassword(passwordInput.getText().toString());
                loginViewModel.getUsername().observe(requireActivity(), username -> {
                    if (passwordInput.getText().length() > 0) {
                        login(username, passwordInput.getText().toString());
                    }
                });
            }
        });

        final PraxFormOnEditorActionListener praxFormOnEditorActionListener = new PraxFormOnEditorActionListener(nextButton);
        passwordInput.setOnEditorActionListener(praxFormOnEditorActionListener);
        passwordInput.requestFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loginViewModel.getUsername().removeObservers(this);
    }

    private void login(final String username, final String password) {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data.Builder networkData = new Data.Builder();
        networkData.putString("username", username);
        networkData.putString("password", password);

        OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(LoginServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(networkData.build())
                .addTag("login")
                .build();

        //Account Configuration
        OneTimeWorkRequest accountConfigurationRequest = new OneTimeWorkRequest.Builder(ActiveConfigurationServiceWorker.class)
                .setConstraints(constraint)
                .addTag("accountconfiguration")
                .build();

        WorkManager
                .getInstance(requireActivity())
                .beginWith(loginRequest)
                .then(accountConfigurationRequest)
                .enqueue();

        WorkManager.getInstance(requireActivity())
                .getWorkInfoByIdLiveData(loginRequest.getId())
                .observe(requireActivity(), workInfo -> {
                    if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Bundle arguments = new Bundle();
                        arguments.putBoolean("authorized", false);
                        arguments.putBoolean("authorizedDevice", false);

                        loginViewModel.setPassword("");
                        loginViewModel.setUsername("");
                        NavHostFragment.findNavController(PasswordFragment.this)
                                .navigate(R.id.action_passwordFragment_to_loginStatusFragment, arguments);
                    }
                });

        WorkManager.getInstance(requireActivity())
                .getWorkInfoByIdLiveData(accountConfigurationRequest.getId())
                .observe(requireActivity(), workInfo -> {
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        final String accountToken = workInfo.getOutputData().getString("apikey");
                        final String receivedPassword = workInfo.getOutputData().getString("password");
                        final int activeProductsCount = workInfo.getOutputData().getInt("active-products-count",-1);

                        final boolean isStreamingAccount = workInfo.getOutputData().getBoolean("isStreamingAccount", false);
                        final com.videostreamtest.data.model.response.Configuration config = new GsonBuilder().create().fromJson(workInfo.getOutputData().getString("configurationObject"), com.videostreamtest.data.model.response.Configuration.class);

                        final Bundle arguments = new Bundle();
                        arguments.putBoolean("authorizedDevice", true);

                        if (accountToken!= null &&
                                (accountToken.equalsIgnoreCase("unauthorized") || accountToken.isEmpty() )) {
                            arguments.putBoolean("authorized", false);
                        } else {
                            //performance and security wise to put data in application shared preferences
                            SharedPreferences myPreferences = requireActivity().getApplication().getSharedPreferences("app",0);
                            SharedPreferences.Editor editor = myPreferences.edit();
                            editor.putString("apikey", accountToken);
                            editor.putString("password", receivedPassword);

                            Log.d(getClass().getSimpleName(), "Login accountToken: "+accountToken);
                            Log.d(getClass().getSimpleName(), "Config not found, inserting new one.");

                            //UPDATE local database with new account configuration
                            Configuration newConfig = new Configuration();
                            newConfig.setAccountToken(accountToken);
                            newConfig.setCurrent(true);
                            newConfig.setLocalPlay(config.isLocalPlay());
                            newConfig.setCommunicationDevice(config.getCommunicationDevice());
                            newConfig.setUpdatePraxCloud(config.isUpdatePraxCloud());
                            newConfig.setPraxCloudMediaServerLocalUrl(config.getPraxCloudMediaServerLocalUrl());
                            newConfig.setPraxCloudMediaServerUrl(config.getPraxCloudMediaServerUrl());
                            newConfig.setAccountType(config.getAccountType());
                            loginViewModel.insert(newConfig);
                            loginViewModel.insertUsageTracker(accountToken);

                            String accountType = config.getAccountType().toLowerCase();

                            arguments.putBoolean("authorized", true);
                            arguments.putString("username", username);
                            arguments.putInt("active-products-count", activeProductsCount);
                            arguments.putString("account-type", accountType);

                            editor.putString("account-type", accountType);
                            editor.putBoolean("bootable", config.isBootOnStart());
                            editor.commit();
                        }
                        loginViewModel.setPassword("");
                        loginViewModel.setUsername("");
                        NavHostFragment.findNavController(PasswordFragment.this)
                                .navigate(R.id.action_passwordFragment_to_loginStatusFragment, arguments);
                    }
                });
    }
}
