package com.videostreamtest.ui.phone.productpicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.log.DeviceInformation;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.ConfigurationHelper;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.helpers.LogHelper;
import com.videostreamtest.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.screensaver.ScreensaverActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.PeriodicInstallPackageServiceWorker;
import com.videostreamtest.workers.SoundInformationServiceWorker;
import com.videostreamtest.workers.UpdateRegisteredMovieServiceWorker;
import com.videostreamtest.workers.UpdateRoutePartsServiceWorker;
import com.videostreamtest.workers.download.DownloadMovieServiceWorker;
import com.videostreamtest.workers.download.DownloadStatusVerificationServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductMovieLinksServiceWorker;
import com.videostreamtest.workers.synchronisation.ActiveProductsServiceWorker;
import com.videostreamtest.workers.synchronisation.SyncFlagsServiceWorker;
import com.videostreamtest.workers.synchronisation.SyncMovieFlagsServiceWorker;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static com.videostreamtest.utils.ApplicationSettings.THREAD_POOL_EXECUTOR;

public class ProductPickerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final static String TAG = ProductPickerActivity.class.getSimpleName();

    private ProductPickerViewModel productPickerViewModel;
    private String apikey = "";

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    private Button settingsButton;
    private Button testButton;
    private ImageView chinesportLogo;
    Data.Builder syncData = new Data.Builder();

    private Handler screensaverhandler;
    private Looper screensaverLooper;
    private Runnable screensaverRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .permitDiskReads()
                .permitDiskWrites()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

        setContentView(R.layout.activity_productpicker);

        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        productPickerViewModel = new ViewModelProvider(this).get(ProductPickerViewModel.class);

        requestAppPermissions();

        initScreensaverHandler();
        startScreensaverHandler();

        settingsButton = findViewById(R.id.productpicker_settings_button);
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = drawerLayout.findViewById(R.id.nav_view);
        chinesportLogo = findViewById(R.id.chinesport_logo_imageview);

        apikey = getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");
        testButton = findViewById(R.id.test_button);
        syncData.putString("apikey",  apikey);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetScreensaverTimer();
    }

    @Override
    protected void onPostCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        findViewById(android.R.id.content).invalidate();
        navView.setNavigationItemSelectedListener(this);
        //LOG INFORMATION
        logDeviceInformation();

        //PERIODIC ACTIONS
//        isValidAccount();
        syncMovieDatabasePeriodically();
        checkForAppUpdatePeriodically();

        PermissionHelper.requestPermission(getApplicationContext(), this);

        //START BLE SERVICE IF PRODUCT NEEDS SENSOR
        productPickerViewModel
                .getAccountProducts(AccountHelper.getAccountToken(getApplicationContext()), !AccountHelper.isLocalPlay(getApplicationContext()))
                .observe(this, products -> {
            if (products!=null && products.size()>0) {
                boolean sensorNeeded = false;
                for (final Product product: products) {
                    if (!product.getCommunicationType().equals("NONE")) {
                        sensorNeeded = true;
                    }
                }
                if (sensorNeeded && !AccountHelper.isChinesportAccount(this)) {
                    startBleService();
                }
            }
        });

        //HIDE UI MENU ITEMS BASED ON ACCOUNT TYPE
        if (AccountHelper.isLocalPlay(getApplicationContext())) {
            hideMenuItem(R.id.nav_logout);
            downloadLocalMovies();
            downloadStatusVerificationCheck();
        } else {
            hideMenuItem(R.id.nav_downloads);
        }

        if (AccountHelper.isChinesportAccount(this)) {
            hideMenuItem(R.id.nav_ble);
            chinesportLogo.setVisibility(View.VISIBLE);
        } else {
            hideMenuItem(R.id.nav_motolife);
            chinesportLogo.setVisibility(View.GONE);
        }

        //SET ONLICK BEHAVIOUR OF SETTINGS BUTTON
        settingsButton.setOnClickListener(onClickedView -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);

                if (navView.getCheckedItem()!= null) {
                    navView.requestFocus();
                } else {
                    navView.requestFocus();
                }
            }
        });

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constraints constraint = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest productMovieSoundsRequest = new OneTimeWorkRequest.Builder(SoundInformationServiceWorker.class)
                        .setConstraints(constraint)
                        .setInputData(syncData.build())
                        .addTag("moviesounds-sync")
                        .build();

                WorkManager.getInstance(getApplicationContext())
                        .beginWith(productMovieSoundsRequest)
                        .enqueue();
            }
        });

        Log.d(TAG, "LIBVLC hash: "+VideoLanLib.getLibVLC(getApplicationContext()).hashCode());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoLanLib.getLibVLC(getApplicationContext()).release();
        VideoLanLib.setLibVlc(null);
        THREAD_POOL_EXECUTOR.getQueue().drainTo(new ArrayList<>());
    }

    private boolean isTouchScreen() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    private void initScreensaverHandler() {
        screensaverRunnable = new Runnable() {
            @Override
            public void run() {
                if (VideoplayerActivity.getInstance() != null && !VideoplayerActivity.getInstance().isActive()) {
                    //Start Screensaver service to show screensaver after predetermined user inactivity
                    Intent screensaverActivity = new Intent(getApplicationContext(), ScreensaverActivity.class);
                    startActivity(screensaverActivity);
                    ApplicationSettings.setScreensaverActive(true);
                } else {
                    resetScreensaverTimer();
                }
            }
        };
    }

    private void startScreensaverHandler() {
        if (screensaverLooper == null) {
            HandlerThread thread = new HandlerThread("ServiceStartArguments",
                    Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();

            // Get the HandlerThread's Looper and use it for our Handler
            screensaverLooper = thread.getLooper();
        }
        screensaverhandler = new Handler(screensaverLooper);
        Log.d(TAG, "call postDelayed with delay of "+ApplicationSettings.SCREENSAVER_TRIGGER_SECONDS*1000+" ms");
        screensaverhandler.postDelayed(screensaverRunnable, ApplicationSettings.SCREENSAVER_TRIGGER_SECONDS*1000);
    }

    private void resetScreensaverTimer() {
        Log.d(TAG, "reset check if Screensaver state = "+ApplicationSettings.SCREENSAVER_ACTIVE);
        if (screensaverhandler != null && !ApplicationSettings.SCREENSAVER_ACTIVE) {
            screensaverhandler.removeCallbacksAndMessages(null);
            startScreensaverHandler();
        }
    }

    private void requestAppPermissions() {
        PermissionHelper.requestPermission(getApplicationContext(), this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem menuItem) {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        switch (menuItem.getItemId()) {
            case R.id.nav_ble:
                navController.navigate(R.id.bleDeviceInformationBoxFragment);
                break;
            case R.id.nav_downloads:
                navController.navigate(R.id.downloadsFragment);
                break;
            case R.id.nav_home:
                navController.navigate(R.id.productPickerFragment);
                break;
            case R.id.nav_motolife:
                navController.navigate(R.id.motoLifeLoginFragment);
                break;
            case R.id.nav_settings:
                navController.navigate(R.id.settingsFragment);
                break;
            case R.id.nav_logout:
                navController.navigate(R.id.logoutFragment);
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    private void checkForAppUpdatePeriodically() {
        Data.Builder syncData = new Data.Builder();
        syncData.putString("apikey",  apikey);
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest productUpdaterRequest = new PeriodicWorkRequest.Builder(PeriodicInstallPackageServiceWorker.class, 2, TimeUnit.HOURS)
                .setInputData(syncData.build())
                .setConstraints(constraint)
                .addTag("product-updater")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-product-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productUpdaterRequest);
    }

    private void startBleService() {
        final String bluetoothDeviceAddress = getSharedPreferences("app", MODE_PRIVATE).getString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, "NONE");
        if (bluetoothDeviceAddress.equals("NONE") || bluetoothDeviceAddress.equals("")) {
            NavHostFragment navHostFragment =
                    (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            NavController navController = navHostFragment.getNavController();
            if (navController.getCurrentDestination().getId() != R.id.bleDeviceInformationBoxFragment) {
                navController.navigate(R.id.bleDeviceInformationBoxFragment);
            }
        } else {
            Intent bleService = new Intent(getApplicationContext(), BleService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(bleService);
            } else {
                startService(bleService);
            }
        }
    }

    private void hideMenuItem(final int id)
    {
        if (navView != null) {
            Menu nav_Menu = navView.getMenu();
            nav_Menu.findItem(id).setVisible(false);
        }
    }

    private void downloadLocalMovies() {
            if (isStoragePermissionGranted(AccountHelper.isLocalPlay(getApplicationContext()))) {
                productPickerViewModel.getRoutefilms(AccountHelper.getAccountToken(getApplicationContext())).observe(this, routefilms -> {
                    if (routefilms.size()>0) {
                        //CHECK IF ALL MOVIES FIT TO DISK
                        // Accumulate size of movies
                        long totalDownloadableMovieFileSizeOnDisk = 0L;
                        for (Routefilm routefilm: routefilms) {
                            if (!DownloadHelper.isMoviePresent(getApplicationContext(), Movie.fromRoutefilm(routefilm))) {
                                totalDownloadableMovieFileSizeOnDisk += routefilm.getMovieFileSize();
                            }
                        }
                        if (DownloadHelper.canFileBeCopiedToLargestVolume(getApplicationContext(), totalDownloadableMovieFileSizeOnDisk)) {
                            for (final Routefilm routefilm: routefilms) {
                                if (!DownloadHelper.isMoviePresent(getApplicationContext(), Movie.fromRoutefilm(routefilm))) {
                                    startDownloadWorker(routefilm.getMovieId(), AccountHelper.getAccountMediaServerUrl(getApplicationContext()));
                                }
                            }
                        } else {
                            Log.d(TAG, "Movies do not fit on disk, DownloadRunners not started.");
                            LogHelper.WriteLogRule(getApplicationContext(), AccountHelper.getAccountToken(getApplicationContext()), "Movies do not fit on disk, DownloadRunners aborted.","DEBUG", "");
                        }
                    }
                });
            }
    }

    private void startDownloadWorker(final int movieId, final String localMediaServerUrl) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data.Builder mediaDownloadInputData = new Data.Builder();
        mediaDownloadInputData.putString("localMediaServer", localMediaServerUrl);
        mediaDownloadInputData.putInt("movie-id", movieId);

        OneTimeWorkRequest downloadRunner = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
                .setConstraints(constraints)
                .setInputData(mediaDownloadInputData.build())
                .addTag("download-runner-request-movie-id-"+movieId)
                .build();
        WorkManager.getInstance(getApplicationContext())
                .beginUniqueWork("download-runner-cluster-"+movieId, ExistingWorkPolicy.KEEP, downloadRunner)
                .enqueue();
    }

    private void downloadStatusVerificationCheck() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest downloadStatusVerificationWorker = new PeriodicWorkRequest.Builder(DownloadStatusVerificationServiceWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("download-status-verification")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("download-status-verification-worker", ExistingPeriodicWorkPolicy.KEEP, downloadStatusVerificationWorker);

    }

    public boolean isStoragePermissionGranted(final boolean isLocalPlay) {
        if (isLocalPlay) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (getApplicationContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission is granted");
                    return true;
                } else {
                    Log.v(TAG, "Permission is revoked");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                Log.v(TAG, "Permission is granted");
                return true;
            }
        }
        return false;
    }

    private void syncMovieDatabasePeriodically() {
        Data.Builder syncData = new Data.Builder();
        syncData.putString("apikey",  apikey);
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest productRequest = new PeriodicWorkRequest.Builder(ActiveProductsServiceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("products")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-products-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productRequest);


        PeriodicWorkRequest productMovieRequest = new PeriodicWorkRequest.Builder(ActiveProductMovieLinksServiceWorker.class, 30, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("productmovie-link")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-pms-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productMovieRequest);

        PeriodicWorkRequest syncDatabaseWorkRequest = new PeriodicWorkRequest.Builder(UpdateRegisteredMovieServiceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("sync-database")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-movies-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, syncDatabaseWorkRequest);

        PeriodicWorkRequest productMoviePartsRequest = new PeriodicWorkRequest.Builder(UpdateRoutePartsServiceWorker.class, 45, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("movieparts-link")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-routeparts-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productMoviePartsRequest);

        PeriodicWorkRequest productMovieSoundsRequest = new PeriodicWorkRequest.Builder(SoundInformationServiceWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("moviesounds-sync")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-sounds-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productMovieSoundsRequest);

        PeriodicWorkRequest flagRequest = new PeriodicWorkRequest.Builder(SyncFlagsServiceWorker.class, 20, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("flags-sync")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-flags", ExistingPeriodicWorkPolicy.REPLACE, flagRequest);

        PeriodicWorkRequest movieflagRequest = new PeriodicWorkRequest.Builder(SyncMovieFlagsServiceWorker.class, 45, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("movieflags-sync")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-movieflags", ExistingPeriodicWorkPolicy.REPLACE, movieflagRequest);
    }

    private void logDeviceInformation() {
        DeviceInformation deviceInformation = new DeviceInformation();
        deviceInformation.setObjectType("DeviceInformation");
        Log.d(TAG, String.format("Android RELEASE version: %s", Build.VERSION.RELEASE));
        deviceInformation.setRelease(Build.VERSION.RELEASE);
        Log.d(TAG, String.format("Android SDK version: %s", Build.VERSION.SDK_INT));
        deviceInformation.setSdk_int(Build.VERSION.SDK_INT);
        Log.d(TAG, "Hardware: "+Build.HARDWARE);
        deviceInformation.setHardware(Build.HARDWARE);
        Log.d(TAG, "Manufacturer: "+Build.MANUFACTURER);
        deviceInformation.setManufacturer(Build.MANUFACTURER);
        Log.d(TAG, "Device: "+Build.DEVICE);
        deviceInformation.setDevice(Build.DEVICE);
        Log.d(TAG, "Board: "+Build.BOARD);
        deviceInformation.setBoard(Build.BOARD);
        Log.d(TAG, "Brand: "+Build.BRAND);
        deviceInformation.setBrand(Build.BRAND);
        Log.d(TAG, "Model: "+Build.MODEL);
        deviceInformation.setModel(Build.MODEL);
        Log.d(TAG, "Product: "+Build.PRODUCT);
        deviceInformation.setProduct(Build.PRODUCT);
        Log.d(TAG, "Bootloader: "+Build.BOOTLOADER);
        deviceInformation.setTouchScreen(isTouchScreen());
        Log.d(TAG, "isTouchscreen: "+isTouchScreen());

        if (Build.SUPPORTED_ABIS.length>0) {
            Log.d(TAG, "ABIS length: "+Build.SUPPORTED_ABIS.length);
            for (String abi : Build.SUPPORTED_ABIS) {
                Log.d(TAG, "ABIS item: "+abi);
            }
        }
        deviceInformation.setRamMemoryBytes(ConfigurationHelper.getMemorySizeInBytes(getApplicationContext()).intValue());
        Log.d(TAG, "Memory Mb: "+(ConfigurationHelper.getMemorySizeInBytes(getApplicationContext())/1024/1024));
        Log.d(TAG, "Memory Gb: "+(ConfigurationHelper.getMemorySizeInBytes(getApplicationContext())/1024/1024/1024));
        LogHelper.WriteLogRule(getApplicationContext(), apikey, new GsonBuilder().create().toJson(deviceInformation, DeviceInformation.class).replaceAll("\"","\'"), "DEBUG", "");
    }

}
