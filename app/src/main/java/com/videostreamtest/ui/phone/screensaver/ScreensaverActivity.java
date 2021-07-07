package com.videostreamtest.ui.phone.screensaver;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.videostreamtest.utils.ApplicationSettings;


public class ScreensaverActivity extends AppCompatActivity {
    private final static String TAG = ScreensaverActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Screensaver Started!");
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        ApplicationSettings.setScreensaverActive(false);
        ScreensaverActivity.this.finish();
    }

}
