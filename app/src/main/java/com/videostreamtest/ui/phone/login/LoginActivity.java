package com.videostreamtest.ui.phone.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.workers.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.LoginServiceWorker;
import com.videostreamtest.workers.ServerStatusServiceWorker;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private LoginViewModel loginViewModel;
    private ProgressBar progressBar;

    private Configuration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        Log.d(this.getClass().getSimpleName(), "Density: "+this.getResources().getDisplayMetrics());

        //Link layout to code components
//        final Button loginButton = findViewById(R.id.login);
//        final Button registerButton = findViewById(R.id.registerButton);
//        final EditText username = findViewById(R.id.username);
//        final EditText password = findViewById(R.id.password);
        progressBar = findViewById(R.id.loading);

//        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                boolean handled = false;
//                if (actionId == EditorInfo.IME_ACTION_SEND) {
//                    handled = loginButton.callOnClick();
//                }
//                return handled;
//            }
//        });
//
//        loginButton.setOnClickListener(new View.OnClickListener(){
//               @Override
//               public void onClick(View v) {
//                    login(username.getText().toString(),password.getText().toString());
//               }
//           }
//        );
//        registerButton.setOnClickListener(new View.OnClickListener(){
//               @Override
//               public void onClick(View v) {
//                    Toast.makeText(getApplicationContext(), getString(R.string.under_construction), Toast.LENGTH_LONG).show();
//               }
//           }
//        );

        startPeriodicGetServerOnlineStatusWorker();
    }

    @Override
    protected void onPostCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loginViewModel.getServerStatusLiveData().observe(this, serverStatus -> {
            if(serverStatus != null) {
                final ImageView serverStatusIndicator = findViewById(R.id.server_status_indicator);
                final TextView networkUnreachableDialog = findViewById(R.id.warning_contact_network_admin_dialog_text);
                final View fragment = findViewById(R.id.login_form_fragment_frame);
                final EditText usernameInput = fragment.findViewById(R.id.login_insert_username_input);
                if(serverStatus.isServerOnline()) {
                    usernameInput.setEnabled(true);
                    usernameInput.setBackgroundColor(Color.WHITE);
                    networkUnreachableDialog.setVisibility(View.GONE);
                    serverStatusIndicator.setImageDrawable(getDrawable(R.drawable.ic_checked));
                    serverStatusIndicator.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                } else {
                    usernameInput.setEnabled(false);
                    usernameInput.setBackgroundColor(Color.LTGRAY);
                    networkUnreachableDialog.setVisibility(View.VISIBLE);
                    serverStatusIndicator.setImageDrawable(getDrawable(R.drawable.ic_close));
                    serverStatusIndicator.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                }
            }
        });
    }

    private void login(final String username, final String password) {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data.Builder networkData = new Data.Builder();
        networkData.putString("username", username);
        networkData.putString("password", password);

        OneTimeWorkRequest sendPingRequest = new OneTimeWorkRequest.Builder(LoginServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(networkData.build())
                .addTag("login")
                .build();

        //Account Configuration
//        Data.Builder configurationData = new Data.Builder();
//        configurationData.putString("apikey", accountToken);
        OneTimeWorkRequest accountConfigurationRequest = new OneTimeWorkRequest.Builder(ActiveConfigurationServiceWorker.class)
                .setConstraints(constraint)
                .addTag("accountconfiguration")
                .build();

        WorkManager
                .getInstance(this)
                .beginWith(sendPingRequest)
                .then(accountConfigurationRequest)
                .enqueue();

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(accountConfigurationRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        progressBar.setVisibility(View.VISIBLE);

                        final String accounttoken = workInfo.getOutputData().getString("apikey");
                        final String receivedPassword = workInfo.getOutputData().getString("password");

                        final boolean isStreamingAccount = workInfo.getOutputData().getBoolean("isStreamingAccount", false);
                        final com.videostreamtest.data.model.response.Configuration config = new GsonBuilder().create().fromJson(workInfo.getOutputData().getString("configurationObject"), com.videostreamtest.data.model.response.Configuration.class);

                        if (accounttoken.equalsIgnoreCase("unauthorized")) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.failed_login),
                                    Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                        } else {
                            //Old way
                            //Put ApiKey in sharedpreferences
                            SharedPreferences myPreferences = getApplication().getSharedPreferences("app",0);
                            SharedPreferences.Editor editor = myPreferences.edit();
                            editor.putString("apikey", accounttoken);
                            editor.putString("password", receivedPassword);
                            editor.commit();

                            Log.d(TAG, "Login accounttoken: "+accounttoken);
                            Log.d(TAG, "Config not found, inserting new one.");

                            Configuration newConfig = new Configuration();
                            newConfig.setAccountToken(accounttoken);
                            newConfig.setCurrent(true);
                            newConfig.setLocalPlay(config.isLocalPlay());
                            newConfig.setCommunicationDevice(config.getCommunicationDevice());
                            newConfig.setUpdatePraxCloud(config.isUpdatePraxCloud());
                            newConfig.setPraxCloudMediaServerLocalUrl(config.getPraxCloudMediaServerLocalUrl());
                            newConfig.setPraxCloudMediaServerUrl(config.getPraxCloudMediaServerUrl());
                            loginViewModel.insert(newConfig);
                            Intent splashScreenActivity = new Intent(getApplicationContext(), SplashActivity.class);
                            startActivity(splashScreenActivity);
                            LoginActivity.this.finish();
                        }

                    }
                });
    }

    private void startPeriodicGetServerOnlineStatusWorker() {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest serverStatusRequester = new PeriodicWorkRequest.Builder(ServerStatusServiceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .addTag("server-online-status")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("server-online-status", ExistingPeriodicWorkPolicy.REPLACE, serverStatusRequester);
    }
}
