package com.videostreamtest.config.application;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.ser.Serializers;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        PraxtourApplication.currentActivity = this;

        Log.d(BaseActivity.class.getSimpleName() + " :: ",
                PraxtourApplication.currentActivity.getClass().getSimpleName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (PraxtourApplication.currentActivity == this) {
            PraxtourApplication.currentActivity = null;
        }
    }
}
