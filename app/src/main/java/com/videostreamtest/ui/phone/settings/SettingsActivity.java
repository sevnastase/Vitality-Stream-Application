package com.videostreamtest.ui.phone.settings;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import com.videostreamtest.R;
import com.videostreamtest.service.ant.AntPlusService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.videostreamtest.ui.phone.catalog.CatalogActivity;
import com.videostreamtest.ui.phone.catalog.CatalogViewModel;
import com.videostreamtest.ui.phone.videoplayer.advanced.AdvancedVideoPlayerActivity;
import com.videostreamtest.workers.NetworkInfoWorker;

public class SettingsActivity extends AppCompatActivity {
    private static SettingsActivity thisSettingsActivity;

    private Intent antplusService;

    private SettingsAntPlusBroadcastReceiver settingsAntPlusBroadcastReceiver;

    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisSettingsActivity = this;
        setContentView(R.layout.activity_settings);

        Log.d("ANTPLUS_SETTINGS", "Version number: "+ AntPluginPcc.getInstalledPluginsVersionNumber(getApplicationContext()));

        //Hookup code to layout xml files
        Toolbar toolbar = findViewById(R.id.toolbar);
        final TextView hostReachable = findViewById(R.id.host_available_notification);
        final Button pingButton = findViewById(R.id.buttonPing);
        final Switch antServiceSwitch = findViewById(R.id.antPlusSwitch);
        final EditText apiKeyInput = findViewById(R.id.editTextTextApiKey);
        final Button apiKeySaveButton = findViewById(R.id.saveApiKey);
        final TextView cadenceValueTextView = findViewById(R.id.cadenceSensorValueTextView);
        final Button resetFactoryDefaultsButton = findViewById(R.id.factorySettingsButton);

        //Set Toolbar
        setSupportActionBar(toolbar);

        //Load ViewModel
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        //Define broadcastReceiver
        settingsAntPlusBroadcastReceiver = new SettingsAntPlusBroadcastReceiver();


        //Define Observers
        settingsViewModel.getPingDescription().observe(this, networkInfo -> {
            hostReachable.setText(networkInfo);
        });

        settingsViewModel.getPingValue().observe(this, networkInfo -> {
            if (networkInfo.equalsIgnoreCase("100")) {
                pingButton.setBackgroundColor(Color.GREEN);
            } else if (networkInfo.equalsIgnoreCase("700")) {
                pingButton.setBackgroundColor(Color.YELLOW);
            } else {
                pingButton.setBackgroundColor(Color.RED);
            }
        });

        settingsViewModel.getAntServiceSwitch().observe(this, antServiceStatus -> {
            antServiceSwitch.setChecked(antServiceStatus);
        });

        settingsViewModel.getApiKey().observe(this, apikey -> {
            apiKeyInput.setText(apikey);
        });

        //Define Ant+ service
        antplusService = new Intent(getApplicationContext(), AntPlusService.class);

        //Check for Ant+ service if installed
        if (!isAntPlusPresent() ) {
            antServiceSwitch.setClickable(false);
            antServiceSwitch.setAlpha(0.3f);

            TextView antDescription = findViewById(R.id.antplusDesription);
            antDescription.setText("No Ant+ support found. Please get in touch with {COMPANY_NAME}.");
        }

        if (antServiceSwitch.isChecked()) {
            IntentFilter filter = new IntentFilter("com.fitstream.ANTDATA");
            this.registerReceiver(settingsAntPlusBroadcastReceiver, filter);
        }

        // Define listeners
        antServiceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(antServiceSwitch.isChecked()) {
                    settingsViewModel.setAntServiceSwitch(true);
                    startService(antplusService);

                    IntentFilter filter = new IntentFilter("com.fitstream.ANTDATA");
                    registerReceiver(settingsAntPlusBroadcastReceiver, filter);

                    Toast.makeText(getApplicationContext(), "Started Ant+ service", Toast.LENGTH_LONG).show();
                } else {
                    settingsViewModel.setAntServiceSwitch(false);
                    stopService(antplusService);
                    Toast.makeText(getApplicationContext(), "Stopped Ant+ service", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Define Reachablitit -> WIP
        hostReachable.setText(settingsViewModel.getPingDescription().getValue());
        pingButton.setBackgroundColor(Color.RED);

        isHostReachable("46.101.137.215");

        pingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHostReachable("46.101.137.215");
            }
        });

        apiKeySaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsViewModel.setApiKey(apiKeyInput.getText().toString());
            }
        });

        resetFactoryDefaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences("app",0);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
            }
        });

        // Define floating Action button listener
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Visit our site for more information", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Intent advancedPlayer = new Intent(getApplicationContext(), AdvancedVideoPlayerActivity.class);
                startActivity(advancedPlayer);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(settingsAntPlusBroadcastReceiver);
        } catch (Exception exception) {
            Log.e(this.getClass().getSimpleName(), exception.getLocalizedMessage());
        }
    }

    public static SettingsActivity getInstance() {
        return thisSettingsActivity;
    }

    public void updateTheTextView(final String t) {
        SettingsActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                TextView textV1 = (TextView) findViewById(R.id.cadenceSensorValueTextView);
                textV1.setText(t);
            }
        });
    }

    private boolean isAntPlusPresent() {
        Log.d("ANTPLUS", "Version number: "+ AntPluginPcc.getInstalledPluginsVersionNumber(getApplicationContext()));
        return (AntPluginPcc.getInstalledPluginsVersionNumber(getApplicationContext()) > 0);
    }

    public void isHostReachable(String ipAddress)
    {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("ipAddress", ipAddress);

        OneTimeWorkRequest sendPingRequest = new OneTimeWorkRequest.Builder(NetworkInfoWorker.class)
                .setInputData(networkData.build())
                .addTag("networkinfo")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(sendPingRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(sendPingRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        settingsViewModel.setPingDescription(
                                workInfo.getOutputData().getString("ping_result"));
                        settingsViewModel.setPingValue(
                                workInfo.getOutputData().getString("ping_value"));
                    }
                });
    }

}