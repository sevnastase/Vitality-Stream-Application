package com.videostreamtest.helpers;

import static com.videostreamtest.constants.PraxConstants.ApkUpdate.PRAXTOUR_LAUNCHER_PACKAGE_NAME;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class NavHelper {
    public static void openPraxtourLauncher(@NonNull Activity activity, boolean logout) {
        Intent installerIntent = activity.getPackageManager().getLaunchIntentForPackage(PRAXTOUR_LAUNCHER_PACKAGE_NAME);
        if (installerIntent != null) {
            installerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            activity.startActivity(installerIntent);
            activity.finishAffinity();
        }
    }
}
