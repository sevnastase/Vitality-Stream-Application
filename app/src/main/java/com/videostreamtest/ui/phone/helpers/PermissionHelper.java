package com.videostreamtest.ui.phone.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    private final static String TAG = PermissionHelper.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 2323;

    public static void requestPermissions(Activity activity) {
        Context context = activity.getApplicationContext();

        List<String> permissions = new ArrayList<>();
        Log.d(TAG, "Checking permissions for Build Version Code "+Build.VERSION.SDK_INT);
        // Check if Android S (12) or lower
        // Android Q (10) is our least target build sdk
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {

            permissions.add(Manifest.permission.INTERNET);
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);

            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);

            if (AccountHelper.isAccountBootable(context)) {
                permissions.add(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            }

        } else { // Android 12 or higher
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (AccountHelper.getAccountType(context).equalsIgnoreCase("standalone")) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!permissions.isEmpty()) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context,
                        permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.format("Permission not GRANTED: " + permission));
                    ActivityCompat.requestPermissions(
                            activity,
                            new String[]{permission},
                            PERMISSION_REQUEST_CODE);
                }
            }
        }
    }

    public static void forceLocationServices(Context context) {
        /**
         * BECAUSE FUNCTIONALITY CHANGED IN ANDROID 10 LocationServices needs to be enabled to enable BLE to find(show) devices,
         * Below the check if this service is enabled or disabled.
         */
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            Log.d(TAG, "LocationManager location services are inactive!");
            // Start Location Settings Activity, you should explain to the user why he need to enable location before.
            ActivityCompat.startActivity(context, new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void forceOverlayForBoot(Context context) {
        if(!Settings.canDrawOverlays(context)) {
            Log.d(TAG, "PackageManager draw-over-app permissions are inactive!");
            ActivityCompat.startActivity(context, new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), null);
        }
    }

    public static boolean isBleFeatureSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static boolean canBleBeUsed(Context context) {
        boolean basicFeaturesrequired = false;
        boolean belowVersion10Features = false;
        boolean higherThanVersion10Features = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            basicFeaturesrequired = (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            belowVersion10Features = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean locationServicesActive = LocationManagerCompat.isLocationEnabled(locationManager);

            higherThanVersion10Features = (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) && locationServicesActive;

        }

        return basicFeaturesrequired && belowVersion10Features && higherThanVersion10Features;
    }
}
