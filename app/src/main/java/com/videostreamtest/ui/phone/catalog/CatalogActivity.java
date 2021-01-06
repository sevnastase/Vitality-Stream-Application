package com.videostreamtest.ui.phone.catalog;

import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.videostreamtest.R;
import com.videostreamtest.ui.phone.settings.SettingsActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoPlayerConfig;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class CatalogActivity extends AppCompatActivity {
     private CatalogViewModel catalogViewModel;

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
        if (catalogViewModel.getApiKey().getValue() == null ) {
            new ApiKeyDialogFragment().show(getSupportFragmentManager(), "ApiKeyDialogFragment");
        }

        catalogViewModel.getApiKey().observe(this, observer -> {
            //TODO: Check if APIKEY is valid, else show ApiKeyDialogFragment

            Log.d(this.getClass().getSimpleName(), "ApiKey :: "+catalogViewModel.getApiKey().getValue());
        });

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingsActivity = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsActivity);
            }
        });

    }

    public void playSelectedVideo(View view) {
        final Intent videoPlayer = new Intent(getApplicationContext(), VideoplayerActivity.class);
        startActivity(videoPlayer);
        finish();
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