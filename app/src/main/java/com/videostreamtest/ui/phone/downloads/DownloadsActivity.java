package com.videostreamtest.ui.phone.downloads;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.videostreamtest.workers.synchronisation.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.LoginServiceWorker;
import com.videostreamtest.workers.ServerStatusServiceWorker;

import java.util.concurrent.TimeUnit;

public class DownloadsActivity extends AppCompatActivity {
    private static final String TAG = DownloadsActivity.class.getSimpleName();

    private DownloadsViewModel downloadsViewModel;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Greg in downloads activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        downloadsViewModel = new ViewModelProvider(this).get(DownloadsViewModel.class);

        Log.d(this.getClass().getSimpleName(), "Density: "+this.getResources().getDisplayMetrics());
        progressBar = findViewById(R.id.loading);
        startPeriodicGetServerOnlineStatusWorker();
    }

    @Override
    protected void onPostCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        downloadsViewModel.getServerStatusLiveData().observe(this, serverStatus -> {
            if(serverStatus != null) {
                final ImageView serverStatusIndicator = findViewById(R.id.server_status_indicator);
                final TextView networkUnreachableDialog = findViewById(R.id.warning_contact_network_admin_dialog_text);

                if (serverStatus.isServerOnline()) {
                    networkUnreachableDialog.setVisibility(View.GONE);
                    serverStatusIndicator.setImageDrawable(getDrawable(R.drawable.ic_checked));
                    serverStatusIndicator.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                } else {
                    networkUnreachableDialog.setVisibility(View.VISIBLE);
                    serverStatusIndicator.setImageDrawable(getDrawable(R.drawable.ic_close));
                    serverStatusIndicator.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
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
