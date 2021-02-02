package com.videostreamtest.ui.phone.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.videoplayer.VideoPlayerConfig;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.workers.AvailableMediaServiceWorker;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class CatalogActivity extends AppCompatActivity {
     private CatalogViewModel catalogViewModel;
     private RecyclerView availableMediaRecyclerView;

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);
        catalogViewModel.getApiKey().observe(this, observer -> {
            Log.d(this.getClass().getSimpleName(), "ApiKey found! ");//+catalogViewModel.getApiKey().getValue());
        });

        final TextView profileName = findViewById(R.id.current_loaded_profile);
        SharedPreferences myPreferences = getSharedPreferences("app",0);
        profileName.setTextSize(20);
        profileName.setTextColor(Color.WHITE);
        profileName.setText(myPreferences.getString("profileName", ""));

        availableMediaRecyclerView = findViewById(R.id.recyclerview_available_media);
        availableMediaRecyclerView.setHasFixedSize(true);
        //Maak lineaire layoutmanager en zet deze op horizontaal
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        //Zet de layoutmanager erin
        availableMediaRecyclerView.setLayoutManager(layoutManager);

        getAvailableMedia(catalogViewModel.getApiKey().getValue());
    }

    public void playSelectedVideo(View view) {
        final Intent videoPlayer = new Intent(getApplicationContext(), VideoplayerActivity.class);
        startActivity(videoPlayer);
        finish();
    }

    public void getAvailableMedia(final String apikey) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);

        OneTimeWorkRequest routeMoviesRequest = new OneTimeWorkRequest.Builder(AvailableMediaServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("available-movies")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(routeMoviesRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(routeMoviesRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("movie-list");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            Movie movieList[] = objectMapper.readValue(result, Movie[].class);
                            //pass profiles to adapter
                            AvailableMediaAdapter availableMediaAdapter = new AvailableMediaAdapter(movieList);
                            //set adapter to recyclerview
                            availableMediaRecyclerView.setAdapter(availableMediaAdapter);
                            //set recyclerview visible
                            availableMediaRecyclerView.setVisibility(View.VISIBLE);
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    //TODO: When a networkspeed is too low to stream, possible to download video locally first.
    private void downloadVideo(  ) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(VideoPlayerConfig.DEFAULT_VIDEO_URL);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("My Video");
        request.setDescription("Downloading");//request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"game-of-life");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadManager.enqueue(request);
    }
}