package com.videostreamtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.videostreamtest.ui.phone.splash.SplashActivity;
import com.videostreamtest.utils.ApplicationSettings;

import static android.os.Build.VERSION_CODES.M;

public class OnBootCompletedBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();

        if(action != null) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                    && ApplicationSettings.START_ON_BOOT) {

                // Code to handle BOOT COMPLETED EVENT
                /**
                 * To enable the start-on-boot you'll need to adjust something in the settings:
                 * Settings > Apps > Special App Access > Display over other apps > Praxtour (set switch on)
                 */

                final Intent splashActivity = new Intent(context, SplashActivity.class);
                splashActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(splashActivity);

                if (Build.VERSION.SDK_INT > M) {
                    if (Settings.canDrawOverlays(context)) {
                        Log.e(this.getClass().getSimpleName(), "Value SplashActivity 1: true");
                    } else {
                        Log.e(this.getClass().getSimpleName(), "Value SplashActivity 1: false");
                    }
                }
            }
        }
    }
}
