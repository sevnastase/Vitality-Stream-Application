package com.videostreamtest.ui.phone.splash;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.ConfigurationHelper;
import com.videostreamtest.helpers.LogHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.AccountServiceWorker;
import com.videostreamtest.workers.InstallPackageServiceWorker;
import com.videostreamtest.workers.UpdatePackageServiceWorker;
import com.videostreamtest.workers.download.DownloadStatusVerificationServiceWorker;


public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private static final int MY_REQUEST_CODE = 1337;
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323;

    private SplashViewModel splashViewModel;

    private Handler loadTimer;

    //Mutex booleans
    private boolean profileViewLoaded = false;
    private boolean productPickerLoaded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.setWorkerProgress(0);

//        checkForRecommendedScreenDpi();
        requestDrawOverlayPermission();

        loadTimer = new Handler(Looper.getMainLooper());

        checkForUpdates();
        checkDownloadStatusVerification();
        refreshAccountInformation();

        //New way
        splashViewModel.getCurrentConfig().observe(this, config -> {
            if (config != null || !getSharedPreferences("app", MODE_PRIVATE).getString("apikey","").equals("")) {
                loadTimer.removeCallbacksAndMessages(null);
            }
            if (config != null) {
                Log.d(TAG, "Token :: " + config.getAccountToken() + " > Current =  " + config.isCurrent());
                //If there's internet, retrieve account info and/or synchronize data
                ConfigurationHelper.loadExternalData(this, config.getAccountToken());

                splashViewModel.resetUsageTracker(config.getAccountToken());
                splashViewModel.resetInterruptedDownloads();

                if (AccountHelper.getAccountToken(getApplicationContext()).equalsIgnoreCase("unauthorized")) {
                    SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                    editor.putString("apikey",  config.getAccountToken());
                    editor.commit();
                }
                if (AccountHelper.getAccountType(getApplicationContext()).equalsIgnoreCase("undefined")) {
                    SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                    editor.putString("account-type", config.getAccountType().toLowerCase());
                    editor.commit();
                }
                if (!AccountHelper.getAccountMediaServerUrl(getApplicationContext()).equalsIgnoreCase(ApplicationSettings.PRAXCLOUD_MEDIA_URL)) {
                    SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                    if (!config.getPraxCloudMediaServerLocalUrl().isEmpty()) {
                        editor.putString("media-server-url", config.getPraxCloudMediaServerLocalUrl());
                    } else {
                        editor.putString("media-server-url",  ApplicationSettings.PRAXCLOUD_MEDIA_URL);
                    }
                    editor.commit();
                }
                SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                editor.putBoolean("bootable", config.isBootOnStart());
                editor.commit();

                /**
                 * TODO  if accounttoken is valid (create worker)
                 *  then continue to next page which is:
                 *  based on number of products > 1 == ProductPicker
                 *  based on number of products == 1 > Load product
                 *  based on number of products == 0 > login activity
                 */
                splashViewModel.getAllAccountProducts(config.getAccountToken()).observe(this, products -> {
                    if (products != null) {
                        Log.d(TAG, "Current product count: "+products.size());
                        Log.d(TAG, "config.isLocalPlay() = "+config.isLocalPlay());
                        if(products.size() > 0) {
                            for (Product p: products) {
                                Log.d(TAG, "P.streaming: "+p.getSupportStreaming());
                                Log.d(TAG, "P.blocked: "+p.getBlocked());
                                Log.d(TAG, "P.name: "+p.getProductName());
                            }
                            Log.d(TAG, "Set current configuration");
//                            if (products.size() > 1 ) {
                                if (!productPickerLoaded) {
                                    productPickerLoaded = true;
                                    Log.d(TAG, "Number of products :: " + products.size());
                                    for (final com.videostreamtest.config.entity.Product p : products) {
                                        Log.d(TAG, "Product ID :: " + p.getUid() + " :: Product name :: " + p.getProductName());
                                    }

                                    //SENSOR CHECK
                                    SharedPreferences sharedPreferences = getSharedPreferences("app" , Context.MODE_PRIVATE);
                                    String deviceAddress = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY,"NONE");

                                    if (deviceAddress.equals("NONE") || deviceAddress.equals("")) {
                                        BluetoothDefaultDevice bluetoothDefaultDevice = new BluetoothDefaultDevice();
                                        bluetoothDefaultDevice.setBleId(1);
                                        bluetoothDefaultDevice.setBleAddress("NONE");
                                        bluetoothDefaultDevice.setBleName("");
                                        bluetoothDefaultDevice.setBleSensorType("");
                                        bluetoothDefaultDevice.setBleSignalStrength("--");
                                        bluetoothDefaultDevice.setBleBatterylevel("--");
                                        splashViewModel.insertBluetoothDefaultDevice(bluetoothDefaultDevice);

                                        Log.d(TAG, "Sensor Device internal db synced with internal in-memory value");
                                        LogHelper.WriteLogRule(getApplicationContext(), config.getAccountToken(), "Sensor device not registered in app memory." ,"DEBUG", "");
                                    }

                                    VideoLanLib.getLibVLC(getApplicationContext());

                                    //If Done correctly go to Productpicker and start have fun!
                                    startActivity(new Intent(SplashActivity.this, ProductPickerActivity.class));
                                    SplashActivity.this.finish();
                                }
//                            } else {
//                                if (!profileViewLoaded) {
//                                    profileViewLoaded = true;
//                                    // only one product so start product immediately based on streamingAccount
//                                    Log.d(TAG, "Single product :: " + products.get(0).getProductName() + " standalone: "+config.isLocalPlay());
//                                    if (config.isLocalPlay()) { //TODO: APPEND CHECK WITH: && products.get(0).getSupportStreaming().intValue()==0
//                                        Bundle arguments = new Bundle();
//                                        arguments.putString("product_object", new GsonBuilder().create().toJson(Product.fromProductEntity(products.get(0)), Product.class));
//
//                                        Intent productView = new Intent(SplashActivity.this, ProductActivity.class);
//                                        productView.putExtras(arguments);
//
//                                        startActivity(productView);
//                                    } else {
//                                        startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
//                                    }
//                                    SplashActivity.this.finish();
//                                }
//                            }
                        } else {
                            //Execute when number of products is 0
                            // This happens when the trial time expires
                            //Login activity will be shown
                            Log.d(TAG, "Unset current configuration when product count = 0");
                            LogHelper.WriteLogRule(getApplicationContext(), config.getAccountToken(), "WARNING! Subscriptions expired! Or closed during login process!", "ERROR", "");
//                            Runnable showLoginScreen = new Runnable() {
//                                public void run() {
//                                    config.setCurrent(false);
//                                    splashViewModel.updateConfiguration(config);
//                                    //Redirect to login activity
//                                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//                                    //Close this activity and release resources
//                                    SplashActivity.this.finish();
//                                }
//                            };
                            //Redirect to login activity if timer exceeds 5 seconds
//                            loadTimer.postDelayed( showLoginScreen, 15000 );
                        }
                    }
                });
            } else {
                Log.d(TAG, "Configuration == null ");
                Log.d(TAG, "SharedPreferences != null > "+getSharedPreferences("app", MODE_PRIVATE).getString("apikey","").equals(""));
                Log.d(TAG, "SharedPreferences value:  "+getSharedPreferences("app", MODE_PRIVATE).getString("apikey",""));

                Runnable showLoginScreen = new Runnable() {
                    public void run() {
                        //Redirect to login activity
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        //Close this activity and release resources
                        SplashActivity.this.finish();
                    }
                };
                //Redirect to login activity if timer exceeds 5 seconds
                loadTimer.postDelayed( showLoginScreen, 5000 );
            }

        });

    }

    private void checkForRecommendedScreenDpi(){
        final int densityDpi = getApplicationContext().getResources().getDisplayMetrics().densityDpi;
        if ( densityDpi < ApplicationSettings.RECOMMENDED_DENSITY_DPI) {
//            Toast.makeText(getApplicationContext(), "The screen density is not optimal for this app. \nPlease upgrade your hardware or change device for an optimal experience.", Toast.LENGTH_LONG).show();
            LogHelper.WriteLogRule(getApplicationContext(), getSharedPreferences("app" , Context.MODE_PRIVATE).getString("apikey", ""), "The screen density is not optimal for this app. Please upgrade your hardware or change device for an optimal experience. Client Density: "+densityDpi ,"ERROR", "");
        }
    }

    private void checkForUpdates() {
        boolean updatedByGPS = ConfigurationHelper.verifyInstalledByGooglePlayStore(getApplicationContext());

        if (!updatedByGPS) {
            Constraints constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest updateServiceWorker = new OneTimeWorkRequest.Builder(UpdatePackageServiceWorker.class)
                    .setConstraints(constraint)
                    .addTag("local-updater")
                    .build();

            OneTimeWorkRequest installPackageServiceWorker = new OneTimeWorkRequest.Builder(InstallPackageServiceWorker.class)
                    .setConstraints(constraint)
                    .addTag("local-update-installer")
                    .build();

            WorkManager.getInstance(this)
                    .beginWith(updateServiceWorker)
                    .then(installPackageServiceWorker)
                    .enqueue();
        } else {
            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());

            // Returns an intent object that you use to check for an update.
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

            // Checks that the platform will allow the specified type of update.
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        // This example applies an immediate update. To apply a flexible update
                        // instead, pass in AppUpdateType.FLEXIBLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    // Request the update.
                    Log.d(TAG, "REQUEST THE GPS UPDATE WHICH IS AVAILABLE");
                    Toast.makeText(this, getString(R.string.update_available), Toast.LENGTH_LONG).show();
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                appUpdateInfo,
                                // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                AppUpdateType.IMMEDIATE,
                                // The current activity making the update request.
                                this,
                                // Include a request code to later monitor this update request.
                                MY_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        Log.e(TAG, sendIntentException.getLocalizedMessage());
                    }
                } else {
                    Log.d(TAG, "NO UPDATE AVAILABLE");
                }
            });
        }
    }

    private void refreshAccountInformation() {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest accountServiceWorker = new OneTimeWorkRequest.Builder(AccountServiceWorker.class)
                .setConstraints(constraint)
                .addTag("account-information-checker")
                .build();

        WorkManager.getInstance(this)
                .beginWith(accountServiceWorker)
                .enqueue();
    }

    private void requestDrawOverlayPermission() {
        // Check if Android M or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show alert dialog to the user saying a separate permission is needed
            requestPermissions(new String[]{Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Manifest.permission.INSTALL_PACKAGES, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            if(!Settings.canDrawOverlays(this)) {
               Log.d(TAG, "checkpermission "+getPackageManager().checkPermission(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, getPackageName())) ;
            }
        }
    }

    private void checkDownloadStatusVerification() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest downloadStatusVerificationWorker = new OneTimeWorkRequest.Builder(DownloadStatusVerificationServiceWorker.class)
                .setConstraints(constraints)
                .addTag("download-status-verification")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniqueWork("download-status-verification-worker", ExistingWorkPolicy.REPLACE, downloadStatusVerificationWorker);
    }

}
