package com.videostreamtest.utils;

import android.app.Activity;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class BlePermission {

    private static final int REQUEST_CODE = 2705;
    private static final String MSG = "The product needs bluetooth permissions";

    public static void ask(Activity activity, String perm) {
        Toast.makeText(activity.getApplicationContext(), MSG, Toast.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(activity, new String[]{perm}, REQUEST_CODE);
    }
}
