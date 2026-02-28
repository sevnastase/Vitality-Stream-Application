package com.videostreamtest.ui.phone.splash;

import static com.videostreamtest.constants.PraxConstants.ApkUpdate.PRAXTOUR_LAUNCHER_PACKAGE_NAME;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_ACCOUNT_TOKEN;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_DOWNLOADS;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_LAUNCHER;
import static com.videostreamtest.constants.PraxConstants.SharedPreferences.STATE_DOWNLOADS_COMPLETED;
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
import com.videostreamtest.helpers.NavHelper;
import com.videostreamtest.helpers.NetworkHelper;
import com.videostreamtest.ui.phone.downloads.DownloadsActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.AccountServiceWorker;
import com.videostreamtest.workers.download.DownloadStatusVerificationServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveConfigurationServiceWorker;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private static final int MY_REQUEST_CODE = 1337;
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323;

    private SplashViewModel splashViewModel;

    private Handler loadTimer;

    //Mutex booleans
    /** For some reason, redirects fire incredibly many times, so this is the solution. */
    private boolean isNavigating = false;
    private boolean productPickerLoaded = false;
    private String apikey;

    /**
     * Necessary to receive intents properly.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        handleIncoming(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.setWorkerProgress(0);

        requestDrawOverlayPermission();

        handleIncoming(getIntent());

        loadTimer = new Handler(Looper.getMainLooper());

        if (!NetworkHelper.isNetworkPraxtourLAN(this)) {
            checkDownloadStatusVerification();
            refreshAccountInformation();
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //New way
        splashViewModel.getCurrentConfig().observe(this, savedConfig -> {
            if (isNavigating || isFinishing() || isDestroyed()) return;

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
                    Log.d(TAG, "Greg saving apikey!");
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

                if (needToDownloadFiles()) {
                    Log.d(TAG, "Greg downloads activity");
                    redirectToActivity(DownloadsActivity.class);
                    return;
                }

                if (NetworkHelper.isNetworkPraxtourLAN(this)) {
                    startActivity(new Intent(this, ProductPickerActivity.class));
                    finish();
                    return;
                }

                /*
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
                        if(!products.isEmpty()) {
                            for (Product p: products) {
                                Log.d(TAG, "P.streaming: "+p.getSupportStreaming());
                                Log.d(TAG, "P.blocked: "+p.getBlocked());
                                Log.d(TAG, "P.name: "+p.getProductName());
                            }
                            Log.d(TAG, "Set current configuration");
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

                                redirectToActivity(ProductPickerActivity.class);
                            }
                        } else {
                            //Execute when number of products is 0
                            // This happens when the trial time expires
                            //Login activity will be shown
                            Log.d(TAG, "Unset current configuration when product count = 0");
                            LogHelper.WriteLogRule(getApplicationContext(), savedConfig.getAccountToken(), "WARNING! Subscriptions expired! Or closed during login process!", "ERROR", "");
                            NavHelper.openPraxtourLauncher(this, true);
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

                            redirectToActivity(SplashActivity.class);
                        }
                    }
                };

                live.observeForever(observer);
            }
        });

    }

    private void handleIncoming(Intent incomingIntent) {
        Log.d(TAG, "Greg incoming to SplashActivity");
        apikey = incomingIntent.getStringExtra(EXTRA_ACCOUNT_TOKEN);

        if (!incomingFromVerifiedSource(incomingIntent)) {
            Log.d(TAG, "\t greg not verified source");
            NavHelper.openPraxtourLauncher(this, false);
        }

        // first check: might be coming from downloads, then apikey can be null indeed
        if (apikey == null || apikey.isBlank()) {
            Log.d(TAG, "\t greg apikey was null or blank");
            // but in that case it should already be saved in sp
            SharedPreferences sp = getSharedPreferences("app", Context.MODE_PRIVATE);
            apikey = sp.getString("apikey", null);
            if (apikey == null || apikey.isBlank()) {
                Log.d(TAG, "\t\t greg and also wasn't saved:(");
                NavHelper.openPraxtourLauncher(this, true);
            } else {
                Log.d(TAG, "\t\t greg all good");
            }
        }
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

    private void redirectToActivity(Class<? extends Activity> destinationActivityClass) {
        isNavigating = true;
        Intent intent = new Intent(this, destinationActivityClass);
        if (destinationActivityClass.equals(SplashActivity.class)) {
            intent.putExtra(EXTRA_ACCOUNT_TOKEN, apikey);
            intent.putExtra(EXTRA_FROM_LAUNCHER, true);
        }
        startActivity(intent);
        finish();
    }

    private boolean needToDownloadFiles() {
        SharedPreferences sp = getSharedPreferences("app", Context.MODE_PRIVATE);
        return !sp.getBoolean(STATE_DOWNLOADS_COMPLETED, false);
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

    private boolean incomingFromVerifiedSource(Intent intent) {
        return intent.getBooleanExtra(EXTRA_FROM_LAUNCHER, false) ||
                intent.getBooleanExtra(EXTRA_FROM_DOWNLOADS, false);
    }
}
