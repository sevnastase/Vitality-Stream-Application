package com.videostreamtest.config.application;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        PraxtourApplication.currentActivity = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (PraxtourApplication.currentActivity == this) {
            PraxtourApplication.currentActivity = null;
        }
    }
}
