package com.videostreamtest.helpers;

import static com.videostreamtest.constants.PermissionRequestConstants.BLUETOOTH_REQUEST_CODE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.videostreamtest.config.application.PraxtourApplication;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    private final static String TAG = PermissionHelper.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 2323;

    public static void requestPermissions(ArrayList<String> permissions, Activity activity ) {
        Log.d(TAG, "Checking specific permissions");

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(PraxtourApplication.getAppContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, String.format("Permission not GRANTED: "+permission));
                ActivityCompat.requestPermissions(activity, new String[]{permission}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    public static void requestPermission(Context context, Activity activity) {
        List<String> permissions = new ArrayList<>();
        Log.d(TAG, "Checking permissions for Build Version Code "+Build.VERSION.SDK_INT);
        // Check if Android M or higher
        // Android M is also our least target build sdk
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            permissions.add(Manifest.permission.INTERNET);
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);

            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);

            if (AccountHelper.isAccountBootable(context)) {
                permissions.add(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            }

            if (AccountHelper.isLocalPlay(context)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
//            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (permissions.size()>0) {
            for (String permission: permissions) {
                if (ContextCompat.checkSelfPermission(context,
                        permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.format("Permission not GRANTED: "+permission));
                    ActivityCompat.requestPermissions(
                            activity,
                            new String[]{permission},
                            PERMISSION_REQUEST_CODE);
                }
            }
        }
    }

    public static void checkBluetoothPermissions(@NonNull Activity activity) {
        if (!hasBluetoothPermissions(activity)) requestBluetoothPermissions(activity);
    }

    public static boolean hasBluetoothPermissions(@NonNull Context context) {
        // Other permissions (for older devices) don't need runtime approval
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestBluetoothPermissions(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                activity,
                new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                },
                BLUETOOTH_REQUEST_CODE);
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

    /**
     * Returns true when the app has all permissions needed to scan for and connect to a BLE sensor.
     * <p>
     * Requirements per version:
         * API 31+: BLUETOOTH_SCAN/BLUETOOTH_CONNECT
         * API 30-: location permission on every Android version, otherwise scans silently fail
     * Location Services being switched off is only logged, not treated as fatal
     */
    public static boolean canBleBeUsed(Context context) {
        if (!isBleFeatureSupported(context)) {
            Log.w(TAG, "canBleBeUsed: FEATURE_BLUETOOTH_LE missing");
            return false;
        }

        final int targetSdk = context.getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdk >= Build.VERSION_CODES.S) {
            boolean granted = hasBluetoothPermissions(context);
            if (!granted) Log.w(TAG, "canBleBeUsed: BLUETOOTH_SCAN/CONNECT not granted");
            return granted;
        }

        boolean locationGranted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)  == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!locationGranted) {
            Log.w(TAG, "canBleBeUsed: no location permission, BLE scans would return nothing");
            return false;
        }

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager)) {
                Log.w(TAG, "canBleBeUsed: Location Services are off, scan results may be empty");
            }
        } catch (Exception e) {
            Log.w(TAG, "canBleBeUsed: could not check Location Services: " + e);
        }
        return true;
    }
}
