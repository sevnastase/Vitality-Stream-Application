package com.videostreamtest.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.enums.CommunicationType;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.UpdateRegisteredMovieServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductMovieLinksServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductsServiceWorker;
import com.videostreamtest.workers.synchronisation.AvailableRoutePartsServiceWorker;
import com.videostreamtest.workers.ProfileServiceWorker;
import com.videostreamtest.workers.SoundInformationServiceWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationHelper {
    public static CommunicationDevice getCommunicationDevice(final String communicationDevice) {
        for(CommunicationDevice device : CommunicationDevice.values()){
            if(communicationDevice.equalsIgnoreCase(device.name())){
                return device;
            }
        }
        return CommunicationDevice.NONE;
    }

    public static CommunicationDevice getCommunicationDevice(final CommunicationType communicationType) {
        switch (communicationType) {
            case RPM:
                return CommunicationDevice.BLE;
            case ACTIVE:
                return CommunicationDevice.BLE;
            case NONE:
            default:
                return CommunicationDevice.NONE;
        }
    }

    public static void loadExternalData(Context context, final String accountToken) {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //Account Configuration
        Data.Builder configurationData = new Data.Builder();
        configurationData.putString("apikey", accountToken);
        OneTimeWorkRequest accountConfigurationRequest = new OneTimeWorkRequest.Builder(ActiveConfigurationServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(configurationData.build())
                .addTag("accountconfiguration")
                .build();

//        //Active Products
//        Data.Builder productData = new Data.Builder();
//        productData.putString("apikey", accountToken);
//        OneTimeWorkRequest productsRequest = new OneTimeWorkRequest.Builder(ActiveProductsServiceWorker.class)
//                .setConstraints(constraint)
//                .setInputData(productData.build())
//                .addTag("products")
//                .build();
//
//        //Account Profiles
//        Data.Builder profileData = new Data.Builder();
//        profileData.putString("apikey", accountToken);
//        OneTimeWorkRequest profilesRequest = new OneTimeWorkRequest.Builder(ProfileServiceWorker.class)
//                .setConstraints(constraint)
//                .setInputData(profileData.build())
//                .addTag("profiles")
//                .build();
//
//        //Routefilms
//        Data.Builder routeFilmdata = new Data.Builder();
//        routeFilmdata.putString("apikey", accountToken);
//        OneTimeWorkRequest routefilmsRequest = new OneTimeWorkRequest.Builder(UpdateRegisteredMovieServiceWorker.class)
//                .setConstraints(constraint)
//                .setInputData(routeFilmdata.build())
//                .addTag("routefilms")
//                .build();
//
//        //Routeparts
//        OneTimeWorkRequest routeMoviepartsRequest = new OneTimeWorkRequest.Builder(AvailableRoutePartsServiceWorker.class)
//                .addTag("available-movieparts")
//                .build();
//
//        OneTimeWorkRequest soundInformationRequest = new OneTimeWorkRequest.Builder(SoundInformationServiceWorker.class)
//                .addTag("available-sounds")
//                .build();
//
//        //Routefilms
//        Data.Builder pmData = new Data.Builder();
//        pmData.putString("apikey", accountToken);
//        OneTimeWorkRequest productMovieRequest = new OneTimeWorkRequest.Builder(ActiveProductMovieLinksServiceWorker.class)
//                .setInputData(pmData.build())
//                .addTag("productmovie-link")
//                .build();

        //Chain workers and enqueue them
        WorkManager
                .getInstance(context).beginUniqueWork("data-loader", ExistingWorkPolicy.KEEP, accountConfigurationRequest)
//                .beginWith(networkInfoRequest)
//                .then(validateAccountTokenRequest)
//                .beginWith(accountConfigurationRequest)
//                .then(profilesRequest)
//                .then(productsRequest)
//                .then(routefilmsRequest)
//                .then(routeMoviepartsRequest)
//                .then(soundInformationRequest)
//                .then(productMovieRequest)
                .enqueue();
    }

    public static String getVersionNumber(Context context) {
        PackageManager manager = context.getPackageManager();
        String myversionName = "";
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            myversionName = info.versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            myversionName = "Unknown-01";
        }
        return myversionName;
    }

    public static int getVersionNumberCode(Context context) {
        PackageManager manager = context.getPackageManager();
        int versionCode =0;
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            versionCode = (int) PackageInfoCompat.getLongVersionCode(info);
        }
        catch (PackageManager.NameNotFoundException e) {
            versionCode = -1;
        }
        return versionCode;
    }

    public static PackageInfo getLocalUpdatePackageInfo(final Context context) {
        final PackageManager pm = context.getPackageManager();
//      String apkName = "example.apk";//for example, remove later
        String fullPath = "/error";

        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER;
            File possibleUpdatePackageLocation = new File(pathname);
            if (possibleUpdatePackageLocation.exists() && possibleUpdatePackageLocation.listFiles().length>0) {
                for (File updateFile : possibleUpdatePackageLocation.listFiles()) {
                    if (updateFile.getName().toLowerCase().endsWith(".apk")) {
                        fullPath = updateFile.getAbsolutePath();
                    }
                }
            }
        }
//        String fullPath = Environment.getExternalStorageDirectory() + "/" + apkName;
        if (new File(fullPath).exists()) {
            PackageInfo info = pm.getPackageArchiveInfo(fullPath, 0);
            return info;
        } else {
            return null;
        }
    }

    public static boolean verifyInstalledByGooglePlayStore(Context context) {
        // A list with valid installers package name
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));

        // The package name of the app that has installed your app
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());

        // true if your app has been downloaded from Play Store
        return installer != null && validInstallers.contains(installer);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public float convertPxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    /**
     * Returns the available ammount of RAM of your Android device in Bytes e.g 1567342592 (1.5GB)
     * @return {Long}
     */
    public static Long getMemorySizeInBytes(final Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long totalMemory = memoryInfo.totalMem;

        return totalMemory;
    }
}
