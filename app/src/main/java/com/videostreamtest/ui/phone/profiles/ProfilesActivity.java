package com.videostreamtest.ui.phone.profiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.workers.ProfileServiceWorker;
import com.videostreamtest.workers.RestServiceWorker;

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
            //Koppel de recyclerView aan de layout xml
            recyclerView = findViewById(R.id.recyclerview_profiles);
            recyclerView.setHasFixedSize(true);
            //Zet de layoutmanager erin
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            loadProfiles(apiKey);
        }
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
