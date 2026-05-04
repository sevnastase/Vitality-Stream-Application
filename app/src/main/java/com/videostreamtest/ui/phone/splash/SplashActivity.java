package com.videostreamtest.ui.phone.splash;

import static com.videostreamtest.constants.PraxConstants.DefaultValues.NO_APIKEY;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_ACCOUNT_TOKEN;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_DOWNLOADS;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_FROM_LAUNCHER;
import static com.videostreamtest.constants.PraxConstants.IntentExtra.EXTRA_LAUNCHER_UPDATE_CHECKED;
import static com.videostreamtest.constants.PraxConstants.NetworkConstants.ACCEPTABLE_DOWNLOAD_SPEED_MBPS;
import static com.videostreamtest.constants.PraxConstants.NetworkConstants.ACCEPTABLE_PING_TO_API;
import static com.videostreamtest.constants.PraxConstants.NetworkConstants.DOWNLOAD_CONNECTION_TIMEOUT_MS;
import static com.videostreamtest.constants.PraxConstants.NetworkConstants.MAX_PING_TO_API;
import static com.videostreamtest.constants.PraxConstants.NetworkConstants.MIN_DOWNLOAD_SPEED_MBPS;
import static com.videostreamtest.constants.PraxConstants.SharedPreferences.STATE_DOWNLOADS_COMPLETED;
import static com.videostreamtest.service.wifi.WifiSpeedtest.ERROR_NETWORK_VALUE;
import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
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
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.ConfigurationHelper;
import com.videostreamtest.helpers.LogHelper;
import com.videostreamtest.helpers.NavHelper;
import com.videostreamtest.helpers.NetworkHelper;
import com.videostreamtest.helpers.PraxCallbacks;
import com.videostreamtest.ui.phone.downloads.DownloadsActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.update.UpdateLauncherActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.AccountServiceWorker;
import com.videostreamtest.workers.download.DownloadStatusVerificationServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveConfigurationServiceWorker;

import java.util.List;
import java.util.Set;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323;

    private SplashViewModel splashViewModel;

    private Handler loadTimer;

    //Mutex booleans
    /** For some strange Android lifecycle reasons, redirects fire multiple times, so this is the solution. */
    private boolean isNavigating = false;
    private boolean productPickerLoaded = false;
    private String apikey;

    private TextView networkDownloadSpeedTextView;
    private TextView networkLatencyTextView;
    private TextView connectingToServerTextView;
    private final HandlerThread networkTesterThread = new HandlerThread("NetworkTesterThread");
    private Handler networkTesterHandler;
    private Runnable networkTesterRunnable;
    private final Handler networkConnectionUiHandler = new Handler(Looper.getMainLooper());
    private final Runnable networkConnectionUiRunnable = new Runnable() {
        int nrDots = 1;

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder(getString(R.string.connecting_to_server));
            for (int i = 0; i < nrDots; i++) {
                sb.append(".");
            }
            nrDots++;
            if (nrDots > 3) nrDots = 1;
            connectingToServerTextView.setText(sb.toString());
            networkConnectionUiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        networkDownloadSpeedTextView = findViewById(R.id.network_download_speed_textview);
        networkLatencyTextView = findViewById(R.id.network_latency_textview);
        connectingToServerTextView = findViewById(R.id.connecting_to_server_textview);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.setWorkerProgress(0);

        requestDrawOverlayPermission();

        final Intent incomingIntent = getIntent();
        apikey = incomingIntent.getStringExtra(EXTRA_ACCOUNT_TOKEN);

        if (firstOpenedSinceRestart(incomingIntent)) {
            connectingToServerTextView.setVisibility(View.VISIBLE);
        }

        if (!networkTesterThread.isAlive()) {
            networkTesterThread.start();
        }

        networkTesterHandler = new Handler(networkTesterThread.getLooper());

        networkTesterRunnable = NetworkHelper.getSpeedtestRunnable(networkTesterHandler, new PraxCallbacks.SpeedtestCallback() {
            @Override
            public void onSuccess(final long ping, final long downloadSpeedKbps) {
                // might be coming from downloads, then apikey can be null indeed
                if (apikey == null || apikey.isBlank()) {
                    Log.d(TAG, "\t greg apikey was null or blank");
                    // but in that case it should already be saved in sp
                    apikey = AccountHelper.getAccountToken(SplashActivity.this);
                    if (apikey == null || NO_APIKEY.equals(apikey)) {
                        Log.d(TAG, "\t\t greg and also wasn't saved");
                        isNavigating = true;
                        NavHelper.openPraxtourLauncher(SplashActivity.this, true, removeCallbacksForNetworkTester());
                        return;
                    } else {
                        Log.d(TAG, "\t\t greg all good");
                    }
                }

                if (!launcherUpdateChecked(incomingIntent)) {
                    if (isNavigating) return;
                    isNavigating = true;

                    Intent updateIntent = new Intent(SplashActivity.this, UpdateLauncherActivity.class);
                    updateIntent.putExtra(EXTRA_ACCOUNT_TOKEN, apikey);
                    startActivity(updateIntent);
                    finish();
                    return;
                }

                if (!incomingFromVerifiedSource(incomingIntent)) {
                    Log.d(TAG, "\t greg not verified source");
                    if (isNavigating) return;
                    isNavigating = true;
                    NavHelper.openPraxtourLauncher(SplashActivity.this, false, removeCallbacksForNetworkTester());
                    return;
                }

                runOnUiThread(() -> setup());
            }

            @Override
            public void onWarning(final long ping, final long downloadSpeedKbps) {
                showNetworkInfos(ping, downloadSpeedKbps);
            }

            @Override
            public void onFailure(final long ping, final long downloadSpeedKbps) {
                networkConnectionUiHandler.removeCallbacksAndMessages(null);

                if (apikey == null || apikey.isBlank()) {
                    apikey = AccountHelper.getAccountToken(SplashActivity.this);
                    isNavigating = true;
                    if (apikey == null || NO_APIKEY.equals(apikey)) {
                        NavHelper.openPraxtourLauncher(SplashActivity.this, true, removeCallbacksForNetworkTester());
                    } else {
                        redirectToActivity(ProductPickerActivity.class);
                    }
                } else {
                    redirectToActivity(ProductPickerActivity.class);
                }

                runOnUiThread(() -> {
                    connectingToServerTextView.setText(getString(R.string.failed_server_connection));
                    connectingToServerTextView.setVisibility(View.VISIBLE);
                });
            }
        });

        networkTesterHandler.postDelayed(networkTesterRunnable, 1000);
        networkConnectionUiHandler.postDelayed(networkConnectionUiRunnable, 1000);
        networkConnectionUiHandler.postDelayed(() -> {
            connectingToServerTextView.setVisibility(View.VISIBLE);
        }, DOWNLOAD_CONNECTION_TIMEOUT_MS);
    }

    private PraxCallbacks.OnFailureCallback removeCallbacksForNetworkTester() {
        return () -> {
            networkTesterHandler.removeCallbacks(networkTesterRunnable);
            Log.d(TAG, "Greg removing network callbacks");
        };
    }

    private void setup() {
        loadTimer = new Handler(Looper.getMainLooper());

        if (!NetworkHelper.isNetworkPraxtourLAN(this)) {
            checkDownloadStatusVerification();
            refreshAccountInformation();
        }

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
                            Toast.makeText(this, "Please login again", Toast.LENGTH_LONG).show();
                            isNavigating = true;
                            NavHelper.openPraxtourLauncher(this, true, removeCallbacksForNetworkTester());
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
                            if (config == null) {
                                NavHelper.openPraxtourLauncher(SplashActivity.this, true, removeCallbacksForNetworkTester());
                                return;
                            }

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
        runOnUiThread(() -> {
            isNavigating = true;
            Intent intent = new Intent(this, destinationActivityClass);
            if (destinationActivityClass.equals(SplashActivity.class)) {
                intent.putExtra(EXTRA_ACCOUNT_TOKEN, apikey);
                intent.putExtra(EXTRA_FROM_LAUNCHER, true);
            }
            startActivity(intent);
            finish();
        });
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

    private boolean launcherUpdateChecked(Intent intent) {
        return intent.getBooleanExtra(EXTRA_LAUNCHER_UPDATE_CHECKED, false);
    }

    private boolean incomingFromVerifiedSource(Intent intent) {
        return intent.getBooleanExtra(EXTRA_FROM_LAUNCHER, false) ||
                intent.getBooleanExtra(EXTRA_FROM_DOWNLOADS, false) ||
                intent.getBooleanExtra(EXTRA_LAUNCHER_UPDATE_CHECKED, false);
    }

    private boolean firstOpenedSinceRestart(Intent intent) {
        String action = intent.getAction();
        Set<String> categories = intent.getCategories();

        return intent.getBooleanExtra(EXTRA_FROM_LAUNCHER, false) ||
                Intent.ACTION_MAIN.equals(action) ||
                categories != null && categories.contains(Intent.CATEGORY_LAUNCHER) ||
                categories != null && categories.contains(Intent.CATEGORY_LEANBACK_LAUNCHER);
    }

    private synchronized void showNetworkInfos(long ping, long speedKbps) {
        final long speedMbps = speedKbps / 1000;

        runOnUiThread(() -> {
            String speedToDisplay = (speedMbps == ERROR_NETWORK_VALUE ? "-" : speedMbps) + " Mb/s";
            int speedTextColor;
            if (speedMbps == ERROR_NETWORK_VALUE || speedMbps < MIN_DOWNLOAD_SPEED_MBPS) {
                speedTextColor = Color.RED;
            } else if (speedMbps < ACCEPTABLE_DOWNLOAD_SPEED_MBPS) {
                speedTextColor = Color.YELLOW;
            } else {
                speedTextColor = Color.WHITE;
            }

            String pingToDisplay = (ping == ERROR_NETWORK_VALUE ? "-" : ping) + "ms";
            int pingTextColor;
            if (ping == ERROR_NETWORK_VALUE || ping > MAX_PING_TO_API) {
                pingTextColor = Color.RED;
            } else if (ping > ACCEPTABLE_PING_TO_API) {
                pingTextColor = Color.YELLOW;
            } else {
                pingTextColor = Color.WHITE;
            }

            networkLatencyTextView.setText(pingToDisplay);
            networkLatencyTextView.setTextColor(pingTextColor);
            networkLatencyTextView.setVisibility(View.VISIBLE);

            networkDownloadSpeedTextView.setText(speedToDisplay);
            networkDownloadSpeedTextView.setTextColor(speedTextColor);
            networkDownloadSpeedTextView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        networkConnectionUiHandler.removeCallbacksAndMessages(null);
        networkTesterHandler.removeCallbacks(networkTesterRunnable);
        networkTesterThread.quitSafely();
    }
}
