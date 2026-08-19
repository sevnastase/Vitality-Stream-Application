package com.videostreamtest.ui.phone.screensaver;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.videostreamtest.utils.ApplicationSettings;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class ScreensaverActivity extends AppCompatActivity {
    private final static String TAG = ScreensaverActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        Log.d(TAG, "Screensaver Started!");

        new Handler(Looper.getMainLooper()).postDelayed(this::shutdownWithPowerService, 5000);
    }

    private void shutdownWithPowerService() {
        try {
            Log.d(TAG, "Greg starting shutdown");
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            powerManager.reboot("Maintenance restart");
        } catch (SecurityException e) {
            Log.e(TAG, "Issue while shutdown: " + e);
        }
    }

    private void shutdownWithSudoCmd() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "su",
                    "-c",
                    "reboot -p"
            });

            String errOutput = readStream(process.getErrorStream());
            String stdOutput = readStream(process.getInputStream());
            int exitCode = process.waitFor();
            Log.d(TAG, "Shutdown - exit-code=" + exitCode + " stdout=" + stdOutput + " stderr=" + errOutput);
        } catch (IOException e) {
            Log.w(TAG, "Failed to shut device down " + e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to read exit code " + e);
        }
    }

    private String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        return sb.toString();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        ApplicationSettings.setScreensaverActive(false);
        ScreensaverActivity.this.finish();
    }

}
