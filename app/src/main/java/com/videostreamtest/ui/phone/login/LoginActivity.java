package com.videostreamtest.ui.phone.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.dao.ProfileDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.workers.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.LoginServiceWorker;

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
        final Button loginButton = findViewById(R.id.login);
        final Button registerButton = findViewById(R.id.registerButton);
        final EditText username = findViewById(R.id.username);
        final EditText password = findViewById(R.id.password);
        progressBar = findViewById(R.id.loading);

        loginButton.setOnClickListener(new View.OnClickListener(){
               @Override
               public void onClick(View v) {
                    login(username.getText().toString(),password.getText().toString());
               }
           }
        );
        registerButton.setOnClickListener(new View.OnClickListener(){
               @Override
               public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), getString(R.string.under_construction), Toast.LENGTH_LONG).show();
               }
           }
        );
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
                            editor.putString("apiKey", accounttoken);
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
}
