package com.videostreamtest.ui.phone.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.workers.LoginServiceWorker;

public class LoginActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
        Data.Builder networkData = new Data.Builder();
        networkData.putString("username", username);
        networkData.putString("password", password);

        OneTimeWorkRequest sendPingRequest = new OneTimeWorkRequest.Builder(LoginServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("login")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(sendPingRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(sendPingRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        progressBar.setVisibility(View.VISIBLE);

                        final String result = workInfo.getOutputData().getString("result");
                        if (result.equalsIgnoreCase("unauthorized")) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.failed_login),
                                    Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                        } else {
                            //Put ApiKey in sharedpreferences
                            SharedPreferences myPreferences = getApplication().getSharedPreferences("app",0);
                            SharedPreferences.Editor editor = myPreferences.edit();
                            editor.putString("apiKey", result);
                            editor.commit();
                            // Start profile overview
                            Intent profileOverview = new Intent(getApplicationContext(), ProfilesActivity.class);
                            startActivity(profileOverview);
                            finish();
                        }

                    }
                });
    }
}
