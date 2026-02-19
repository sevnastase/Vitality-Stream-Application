package com.videostreamtest.ui.phone.splash;

import static com.videostreamtest.constants.PraxConstants.EXTRA_ACCOUNT_TOKEN;
import static com.videostreamtest.constants.PraxConstants.EXTRA_FIRST_LOGIN;
import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.ConfigurationHelper;
import com.videostreamtest.helpers.LogHelper;
import com.videostreamtest.helpers.NetworkHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.AccountServiceWorker;
import com.videostreamtest.workers.UpdatePackageServiceWorker;
import com.videostreamtest.workers.download.DownloadStatusVerificationServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveConfigurationServiceWorker;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private static final int MY_REQUEST_CODE = 1337;
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323;

    // For update manager
    private static final String EXTRA_APK_FILENAME = "com.praxupdatemanager.APK_FILENAME";
    private static final String EXTRA_UPDATE_VERSION = "com.praxupdatemanager.APK_UPDATE_VERSION";
    private static final String EXTRA_CURRENT_VERSION = "com.praxupdatemanager.APK_CURRENT_VERSION";
    private static final String ACTION_PRAX_REMOTE_UPDATE = "com.praxupdatemanager.ACTION_PRAX_REMOTE_UPDATE";
    private static final String PRAXTOUR_UPDATE_MANAGER_PACKAGE_NAME = "com.praxtourupdatemanager"; // TODO put in database and fetch

    private SplashViewModel splashViewModel;

    private Handler loadTimer;

    //Mutex booleans
    private boolean profileViewLoaded = false;
    private boolean productPickerLoaded = false;
    private boolean isWaitingForUpdateCheck = true; // FIXME set to false at some point
    private final Handler waitingForUpdateCheckHandler = new Handler(Looper.getMainLooper());
    private String apikey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.setWorkerProgress(0);

//        checkForRecommendedScreenDpi();
        requestDrawOverlayPermission();

        Intent incomingIntent = getIntent();
        apikey = incomingIntent.getStringExtra(EXTRA_ACCOUNT_TOKEN);

        if (apikey == null || apikey.isBlank()) {
            // redirect to launcher WITHOUT waiting for update.
            Toast.makeText(this, "No apikey", Toast.LENGTH_LONG).show();
            return;
        }

        boolean firstLogin = incomingIntent.getBooleanExtra(EXTRA_FIRST_LOGIN, false);
        if (firstLogin) {
            redirectToAfterUpdateCheckIsComplete(LoginActivity.class);
        }

        loadTimer = new Handler(Looper.getMainLooper());

        if (!NetworkHelper.isNetworkPraxtourLAN(this)) {
//            checkForUpdates();
            checkDownloadStatusVerification();
            refreshAccountInformation();
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //New way
        splashViewModel.getCurrentConfig().observe(this, savedConfig -> {
            if (savedConfig != null || !getSharedPreferences("app", MODE_PRIVATE).getString("apikey","").equals("")) {
                loadTimer.removeCallbacksAndMessages(null);
            }
            if (savedConfig != null) {
                Log.d(TAG, "Token :: " + savedConfig.getAccountToken() + " > Current =  " + savedConfig.isCurrent());
                //If there's internet, retrieve account info and/or synchronize data
                if (!NetworkHelper.isNetworkPraxtourLAN(this)) {
                    ConfigurationHelper.loadExternalData(this, savedConfig.getAccountToken());
                }

                splashViewModel.resetUsageTracker(savedConfig.getAccountToken());
                splashViewModel.resetInterruptedDownloads();

                if (AccountHelper.getAccountToken(getApplicationContext()).equalsIgnoreCase("unauthorized")) {
                    SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                    editor.putString("apikey",  savedConfig.getAccountToken());
                    editor.commit();
                }
                if (AccountHelper.getAccountType(getApplicationContext()).equalsIgnoreCase("undefined")) {
                    SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                    editor.putString("account-type", savedConfig.getAccountType().toLowerCase());
                    editor.commit();
                }
                if (!AccountHelper.getAccountMediaServerUrl(getApplicationContext()).equalsIgnoreCase(PRAXCLOUD_MEDIA_URL)) {
                    SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                    if (!savedConfig.getPraxCloudMediaServerLocalUrl().isEmpty()) {
                        editor.putString("media-server-url", savedConfig.getPraxCloudMediaServerLocalUrl());
                    } else {
                        editor.putString("media-server-url",  PRAXCLOUD_MEDIA_URL);
                    }
                    editor.commit();
                }
                SharedPreferences.Editor editor = getSharedPreferences("app", MODE_PRIVATE).edit();
                editor.putBoolean("bootable", savedConfig.isBootOnStart());
                editor.commit();

                if (NetworkHelper.isNetworkPraxtourLAN(this)) {
                    startActivity(new Intent(this, ProductPickerActivity.class));
                    finish();
                    return;
                }

                /**
                 * TODO  if accounttoken is valid (create worker)
                 *  then continue to next page which is:
                 *  based on number of products > 1 == ProductPicker
                 *  based on number of products == 1 > Load product
                 *  based on number of products == 0 > login activity
                 */
                splashViewModel.getAllAccountProducts(savedConfig.getAccountToken()).observe(this, products -> {
                    if (products != null) {
                        Log.d(TAG, "Current product count: "+products.size());
                        Log.d(TAG, "config.isLocalPlay() = "+savedConfig.isLocalPlay());
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
                                        LogHelper.WriteLogRule(getApplicationContext(), savedConfig.getAccountToken(), "Sensor device not registered in app memory." ,"DEBUG", "");
                                    }

                                    VideoLanLib.getLibVLC(getApplicationContext());

                                    redirectToAfterUpdateCheckIsComplete(ProductPickerActivity.class);
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
                            LogHelper.WriteLogRule(getApplicationContext(), savedConfig.getAccountToken(), "WARNING! Subscriptions expired! Or closed during login process!", "ERROR", "");
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
                // Config is null, need to add it to repository
                // Note: sticking to an old system here to not break anything,
                // but could (and should) be reworked in the future

                Constraints constraint = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                Data data = new Data.Builder()
                        .putString("apikey", apikey)
                        .build();

                OneTimeWorkRequest accountConfigurationRequest = new OneTimeWorkRequest.Builder(ActiveConfigurationServiceWorker.class)
                        .setConstraints(constraint)
                        .setInputData(data)
                        .addTag("account-configuration")
                        .build();

                WorkManager.getInstance(this).enqueueUniqueWork(
                        "account-configuration",
                        ExistingWorkPolicy.KEEP,
                        accountConfigurationRequest);

                LiveData<List<WorkInfo>> live = WorkManager.getInstance(this)
                        .getWorkInfosForUniqueWorkLiveData("account-configuration");

                Observer<List<WorkInfo>> observer = new Observer<List<WorkInfo>>() {
                    @Override
                    public void onChanged(List<WorkInfo> workInfos) {
                        WorkInfo workInfo = workInfos.get(0);

                        if (!workInfo.getState().isFinished()) return;

                        live.removeObserver(this);

                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            final Configuration config = new GsonBuilder().create().fromJson(workInfo.getOutputData().getString("configurationObject"), Configuration.class);

                            //UPDATE local database with new account configuration
                            Configuration newConfig = extractConfiguration(config);
                            splashViewModel.insertConfig(newConfig);
                            splashViewModel.insertUsageTracker(apikey);

                            redirectToAfterUpdateCheckIsComplete(SplashActivity.class);
                        }
                    }
                };

                live.observeForever(observer);
            }
        });

    }

    private Configuration extractConfiguration(Configuration config) {
        Configuration newConfig = new Configuration();
        newConfig.setAccountToken(apikey);
        newConfig.setCurrent(true);
        newConfig.setLocalPlay(config.isLocalPlay());
        newConfig.setCommunicationDevice(config.getCommunicationDevice());
        newConfig.setUpdatePraxCloud(config.isUpdatePraxCloud());
        newConfig.setPraxCloudMediaServerLocalUrl(config.getPraxCloudMediaServerLocalUrl());
        newConfig.setPraxCloudMediaServerUrl(config.getPraxCloudMediaServerUrl());
        newConfig.setAccountType(config.getAccountType());
        return newConfig;
    }

    private void checkForRecommendedScreenDpi(){
        final int densityDpi = getApplicationContext().getResources().getDisplayMetrics().densityDpi;
        if ( densityDpi < ApplicationSettings.RECOMMENDED_DENSITY_DPI) {
//            Toast.makeText(getApplicationContext(), "The screen density is not optimal for this app. \nPlease upgrade your hardware or change device for an optimal experience.", Toast.LENGTH_LONG).show();
            LogHelper.WriteLogRule(getApplicationContext(), getSharedPreferences("app" , Context.MODE_PRIVATE).getString("apikey", ""), "The screen density is not optimal for this app. Please upgrade your hardware or change device for an optimal experience. Client Density: "+densityDpi ,"ERROR", "");
        }
    }

    private void checkForUpdates() {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest updateServiceWorker = new OneTimeWorkRequest.Builder(UpdatePackageServiceWorker.class)
                .setConstraints(constraint)
                .addTag("update-checker")
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "update-checker",
                ExistingWorkPolicy.KEEP,
                updateServiceWorker);

        LiveData<List<WorkInfo>> live = WorkManager.getInstance(getApplicationContext()).getWorkInfosForUniqueWorkLiveData("update-checker");

        Observer<List<WorkInfo>> observer = new Observer<>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                WorkInfo workInfo = workInfos.get(0);

                if (!workInfo.getState().isFinished()) return;

                live.removeObserver(this);

                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    Data outputData = workInfo.getOutputData();
                    boolean updateAvailable = outputData.getBoolean("updateAvailable", false);
                    String updateFilename = outputData.getString("updateFilename");
                    String updateVersion = outputData.getString("updateVersion");

                    if (updateAvailable && updateFilename != null && !updateFilename.isBlank()) {
                        waitingForUpdateCheckHandler.removeCallbacksAndMessages(null);
                        openPraxtourUpdateManager(updateFilename, updateVersion);
                    }
                }
            }
        };

        live.observeForever(observer);
    }

    private void redirectToAfterUpdateCheckIsComplete(Class<? extends Activity> desinationActivityClass) {
        waitingForUpdateCheckHandler.postDelayed(new Runnable() {
            final int WAIT_TIME_SECONDS = 5;
            int counter = 0;
            @Override
            public void run() {
                Log.d(TAG, "Greg count " + counter);
                if (isWaitingForUpdateCheck && counter < WAIT_TIME_SECONDS) {
                    counter++;
                    waitingForUpdateCheckHandler.postDelayed(this, 1000);
                } else {
                    if (isWaitingForUpdateCheck) {
                        // Cancel only if we exceeded the timer; not when the check completed
                        WorkManager.getInstance(SplashActivity.this).cancelUniqueWork("update-checker");
                    }
                    waitingForUpdateCheckHandler.removeCallbacksAndMessages(null);
                    startActivity(new Intent(SplashActivity.this, desinationActivityClass));
                    SplashActivity.this.finish();
                }
            }
        }, 1000);
    }

    private void openPraxtourUpdateManager(String updateFilename, String updateVersion) {
        Intent updateIntent = new Intent(ACTION_PRAX_REMOTE_UPDATE);
        updateIntent.putExtra(EXTRA_APK_FILENAME, updateFilename);
        updateIntent.putExtra(EXTRA_UPDATE_VERSION, updateVersion);
        updateIntent.putExtra(EXTRA_CURRENT_VERSION, ConfigurationHelper.getVersionNumber(this));
        startActivity(updateIntent);
        finish();
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
