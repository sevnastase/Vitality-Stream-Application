package com.videostreamtest.helpers;

import static com.videostreamtest.constants.PraxConstants.ApkUpdate.PRAXTOUR_LAUNCHER_PACKAGE_NAME;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_LOGOUT;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.splash.SplashActivity;

public class NavHelper {
    public static void openPraxtourLauncher(@NonNull Activity activity, boolean logout, @Nullable PraxCallbacks.OnFailureCallback onFailureCallback) {
        activity.runOnUiThread(() -> {
            Intent installerIntent = activity.getPackageManager().getLaunchIntentForPackage(PRAXTOUR_LAUNCHER_PACKAGE_NAME);
            if (installerIntent != null) {
                installerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                installerIntent.putExtra(EXTRA_LOGOUT, logout);
                activity.startActivity(installerIntent);
                activity.finishAffinity();
            } else {
                if (onFailureCallback != null) {
                    onFailureCallback.run();
                }

                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.open_launcher_failed_title))
                        .setMessage(activity.getString(R.string.open_launcher_failed_description))
                        .setIcon(AppCompatResources.getDrawable(activity, R.drawable.praxtour_logo))
                        .setPositiveButton(activity.getString(R.string.retry_button_text), (dialog, which) -> {
                            activity.startActivity(new Intent(activity, SplashActivity.class));
                        })
                        .setCancelable(false)
                        .create().show();
            }
        });
    }
}
