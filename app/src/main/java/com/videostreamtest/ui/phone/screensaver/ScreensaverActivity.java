package com.videostreamtest.ui.phone.screensaver;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.videostreamtest.config.application.BaseActivity;
import com.videostreamtest.utils.ApplicationSettings;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;


public class ScreensaverActivity extends BaseActivity {
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
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        ApplicationSettings.setScreensaverActive(false);
        ScreensaverActivity.this.finish();
    }

}
