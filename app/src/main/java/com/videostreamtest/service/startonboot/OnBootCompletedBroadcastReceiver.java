package com.videostreamtest.service.startonboot;

import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_BOOT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.splash.SplashActivity;

public class OnBootCompletedBroadcastReceiver extends BroadcastReceiver {
    private final static String TAG = OnBootCompletedBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "Greg onreceive");
        final String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "Received greg intent " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_REBOOT.equals(action)) {
            Log.d(TAG, "Greg binnen");
            /*
             * To enable the start-on-boot you'll need to adjust something in the settings:
             * Settings > Apps > Special App Access > Display over other apps > Praxtour Launcher (set switch on)
             */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                final Intent splashActivity = new Intent(context, SplashActivity.class);
                splashActivity.putExtra(EXTRA_FROM_BOOT, true);
                splashActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(splashActivity);
            } else {
                Log.d(TAG, "Greg we high");
                Intent serviceIntent = new Intent(context, BootStartupService.class);
                serviceIntent.putExtra(EXTRA_FROM_BOOT, true);
                ContextCompat.startForegroundService(context, serviceIntent);
            }

            if (Settings.canDrawOverlays(context)) {
                Log.e(this.getClass().getSimpleName(), "Value SplashActivity 1: true");
            } else {
                Log.e(this.getClass().getSimpleName(), "Value SplashActivity 1: " + Settings.canDrawOverlays(context));
                Log.e(this.getClass().getSimpleName(), "Value SplashActivity 2: " + Build.VERSION.SDK_INT);
                Log.e(this.getClass().getSimpleName(), "Value SplashActivity 2: " + context.getString(R.string.app_name));
            }
        }
    }
}
