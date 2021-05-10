package com.videostreamtest.ui.phone.helpers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.content.pm.PackageInfoCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.workers.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.ActiveProductMovieLinksServiceWorker;
import com.videostreamtest.workers.ActiveProductsServiceWorker;
import com.videostreamtest.workers.AvailableMediaServiceWorker;
import com.videostreamtest.workers.AvailableRoutePartsServiceWorker;
import com.videostreamtest.workers.NetworkInfoWorker;
import com.videostreamtest.workers.ProfileServiceWorker;
import com.videostreamtest.workers.SoundInformationServiceWorker;

public class ConfigurationHelper {
    public static CommunicationDevice getCommunicationDevice(final String communicationDevice) {
        for(CommunicationDevice device : CommunicationDevice.values()){
            if(communicationDevice.equalsIgnoreCase(device.name())){
                return device;
            }
        }
        return CommunicationDevice.NONE;
    }

    public static void loadExternalData(Context context, final String accountToken) {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //NetworkInfo
        Data.Builder networkData = new Data.Builder();
        OneTimeWorkRequest networkInfoRequest = new OneTimeWorkRequest.Builder(NetworkInfoWorker.class)
                .setConstraints(constraint)
                .setInputData(networkData.build())
                .addTag("connection-status")
                .build();

        //Account Configuration
        Data.Builder configurationData = new Data.Builder();
        configurationData.putString("apikey", accountToken);
        OneTimeWorkRequest accountConfigurationRequest = new OneTimeWorkRequest.Builder(ActiveConfigurationServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(configurationData.build())
                .addTag("accountconfiguration")
                .build();

        //Active Products
        Data.Builder productData = new Data.Builder();
        productData.putString("apikey", accountToken);
        OneTimeWorkRequest productsRequest = new OneTimeWorkRequest.Builder(ActiveProductsServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(productData.build())
                .addTag("products")
                .build();

        //Account Profiles
        Data.Builder profileData = new Data.Builder();
        profileData.putString("apikey", accountToken);
        OneTimeWorkRequest profilesRequest = new OneTimeWorkRequest.Builder(ProfileServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(profileData.build())
                .addTag("profiles")
                .build();

        //Routefilms
        Data.Builder routeFilmdata = new Data.Builder();
        routeFilmdata.putString("apikey", accountToken);
        OneTimeWorkRequest routefilmsRequest = new OneTimeWorkRequest.Builder(AvailableMediaServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(routeFilmdata.build())
                .addTag("routefilms")
                .build();

        //Routeparts
        OneTimeWorkRequest routeMoviepartsRequest = new OneTimeWorkRequest.Builder(AvailableRoutePartsServiceWorker.class)
                .addTag("available-movieparts")
                .build();

        OneTimeWorkRequest soundInformationRequest = new OneTimeWorkRequest.Builder(SoundInformationServiceWorker.class)
                .addTag("available-sounds")
                .build();

        //Routefilms
        Data.Builder pmData = new Data.Builder();
        pmData.putString("apikey", accountToken);
        OneTimeWorkRequest productMovieRequest = new OneTimeWorkRequest.Builder(ActiveProductMovieLinksServiceWorker.class)
                .setInputData(pmData.build())
                .addTag("productmovie-link")
                .build();

        //Chain workers and enqueue them
        WorkManager
                .getInstance(context)
                .beginWith(networkInfoRequest)
//                .then(validateAccountTokenRequest)
                .then(accountConfigurationRequest)
                .then(profilesRequest)
                .then(productsRequest)
                .then(routefilmsRequest)
                .then(routeMoviepartsRequest)
                .then(soundInformationRequest)
                .then(productMovieRequest)
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
}
