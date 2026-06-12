package com.videostreamtest.service.startonboot;

import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_BOOT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.splash.SplashActivity;

public class BootStartupService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "boot_channel")
                .setContentTitle("Starting " + getString(R.string.app_name))
                .setContentText("Preparing app after reboot...")
                .setSmallIcon(R.drawable.praxtour_logo)
                .build();

        startForeground(1, notification);

        Intent splash = new Intent(this, SplashActivity.class);
        splash.putExtra(EXTRA_FROM_BOOT, intent.getBooleanExtra(EXTRA_FROM_BOOT, false));
        splash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(splash);
        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "boot_channel",
                    "Boot Startup",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
