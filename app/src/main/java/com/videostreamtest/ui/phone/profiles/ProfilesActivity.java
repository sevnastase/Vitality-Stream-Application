package com.videostreamtest.ui.phone.profiles;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.workers.ProfileServiceWorker;

public class ProfilesActivity extends AppCompatActivity {
   private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_overview);

        //retrieve API key
        SharedPreferences myPreferences = getApplication().getSharedPreferences("app",0);
        String apiKey = myPreferences.getString("apiKey", "unauthorized");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equalsIgnoreCase("unauthorized")) {
            Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(loginActivity);
            finish();
        } else {
            //Write log for stating antplus status and version
            Log.d("ANTPLUS_SETTINGS", "Version number: "+ AntPluginPcc.getInstalledPluginsVersionNumber(getApplicationContext()));
            if (AntPlusService.isAntPlusDevicePresent(getApplicationContext())) {
                checkForSystemApprovalAntPlusService();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Ant+ device not found!").setTitle("Ant+ plugin error");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }

            //Koppel de recyclerView aan de layout xml
            recyclerView = findViewById(R.id.recyclerview_profiles);
            recyclerView.setHasFixedSize(true);
            //Maak lineaire layoutmanager en zet deze op horizontaal
            LinearLayoutManager layoutManager
                    = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            //Grid Layout
//            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4, GridLayoutManager.HORIZONTAL, false);
            //Zet de layoutmanager erin
            recyclerView.setLayoutManager(layoutManager);

            loadProfiles(apiKey);
        }
    }

    private void checkForSystemApprovalAntPlusService() {
        Intent antPlusService = new Intent(getApplicationContext(), AntPlusService.class);
        startService(antPlusService);
        Runnable stopAntPlusService = () -> stopService(antPlusService);
        new Handler(Looper.getMainLooper()).postDelayed( stopAntPlusService, 8000 );
    }

    private void loadProfiles(final String apikey) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);

        OneTimeWorkRequest profilesRequest = new OneTimeWorkRequest.Builder(ProfileServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("profiles")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(profilesRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(profilesRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("profiles");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            Profile profiles[] = objectMapper.readValue(result, Profile[].class);
                            //pass profiles to adapter
                            ProfileAdapter profileAdapter = new ProfileAdapter(profiles);
                            //set adapter to recyclerview
                            recyclerView.setAdapter(profileAdapter);
                            //set recyclerview visible
                            recyclerView.setVisibility(View.VISIBLE);
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
