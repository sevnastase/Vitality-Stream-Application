package com.videostreamtest.ui.phone.productview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.productview.fragments.AbstractProductScreenFragment;
import com.videostreamtest.ui.phone.productview.fragments.PlainScreenFragment;
import com.videostreamtest.ui.phone.productview.fragments.TouchScreenFragment;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.ui.phone.screensaver.ScreensaverActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.ActiveProductMovieLinksServiceWorker;
import com.videostreamtest.workers.DataIntegrityCheckServiceWorker;
import com.videostreamtest.workers.DownloadMovieImagesServiceWorker;
import com.videostreamtest.workers.DownloadMovieServiceWorker;
import com.videostreamtest.workers.DownloadRoutepartsServiceWorker;
import com.videostreamtest.workers.DownloadSoundServiceWorker;
import com.videostreamtest.workers.SoundInformationServiceWorker;
import com.videostreamtest.workers.UpdateRegisteredMovieServiceWorker;
import com.videostreamtest.workers.UpdateRoutePartsServiceWorker;

import java.util.concurrent.TimeUnit;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

public class ProductActivity extends AppCompatActivity {
    private final static String TAG = ProductActivity.class.getSimpleName();

    private ProductViewModel productViewModel;
    private Button signoutButton;
    private ImageView productLogo;
    private TextView appBuildNumber;

    private Handler screensaverhandler;
    private Looper screensaverLooper;
    private Runnable screensaverRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        initScreensaverHandler();
        startScreensaverHandler();

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        signoutButton = findViewById(R.id.product_logout_button);
        productLogo = findViewById(R.id.product_logo_view);
        appBuildNumber = findViewById(R.id.app_build_number);

        Product selectedProduct = new GsonBuilder().create().fromJson(getIntent().getExtras().getString("product_object", "{}"), Product.class);
        Log.d(ProductActivity.class.getSimpleName(), "Product ID Loaded: "+selectedProduct.getId());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        Log.d(TAG, "Screen resolution: w:"+width+" h:"+height);

        //Set product logo in view
        Picasso.get()
                .load(selectedProduct.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productLogo);

        productViewModel.getCurrentConfig().observe(this, currentConfig ->{
            if (currentConfig != null) {
                PermissionHelper.requestPermission(getApplicationContext(), this, currentConfig);

                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Started Product: "+selectedProduct.getProductName(), "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Screensize: wxh: "+width+" x "+height, "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Localip: "+DownloadHelper.getLocalIpAddress(), "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Density: sw-"+this.getResources().getDisplayMetrics().densityDpi, "DEBUG", "");

                Bundle arguments = getIntent().getExtras();
                arguments.putString("communication_device", currentConfig.getCommunicationDevice());

                //PERIODIC ACTIONS FOR DATABASE
                syncMovieDatabasePeriodically(currentConfig.getAccountToken());

                //PERIODIC ACTIONS FOR STANDALONE SPECIFIC
                if (currentConfig.isLocalPlay()) {
                    periodicSyncDownloadMovieRouteParts(currentConfig.getAccountToken());
//                    periodicCheckMovieFileDataIntegrity(currentConfig.getAccountToken()); TODO: Make available through Account Config
//                    startSingleDataIntegrityWorker(currentConfig.getAccountToken());
                }
                loadFragmentBasedOnScreenType(arguments);

                appBuildNumber.setText(ConfigurationHelper.getVersionNumber(getApplicationContext())+":"+currentConfig.getAccountToken());


                signoutButton.setOnClickListener(view -> {
                    ProductActivity.this.finish();
                });
                signoutButton.setOnFocusChangeListener((view, hasFocus) -> {
                    if (hasFocus) {
                        final Drawable border = getDrawable(R.drawable.imagebutton_blue_border);
                        signoutButton.setBackground(border);
                    } else {
                        signoutButton.setBackground(null);
                    }
                });
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        downloadMovieSupportImages();
        downloadSound();
        downloadLocalMovies();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetScreensaverTimer();
    }

    public boolean isStoragePermissionGranted() {
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
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    private void loadFragmentBasedOnScreenType(final Bundle arguments) {
//        Log.d(TAG, "Hardware: "+Build.HARDWARE);
//        Log.d(TAG, "Manufacturer: "+Build.MANUFACTURER);
//        Log.d(TAG, "Device: "+Build.DEVICE);
//        Log.d(TAG, "Board: "+Build.BOARD);
//        Log.d(TAG, "Brand: "+Build.BRAND);
//        Log.d(TAG, "Model: "+Build.MODEL);
//        Log.d(TAG, "Product: "+Build.PRODUCT);
//        Log.d(TAG, "Bootloader: "+Build.BOOTLOADER);
//        if (Build.SUPPORTED_ABIS.length>0) {
//            Log.d(TAG, "ABIS length: "+Build.SUPPORTED_ABIS.length);
//            for (String abi : Build.SUPPORTED_ABIS) {
//                Log.d(TAG, "ABIS item: "+abi);
//            }
//        }
        Log.d(TAG, "Memory Mb: "+(ConfigurationHelper.getMemorySizeInBytes(getApplicationContext())/1024/1024));
        Log.d(TAG, "Memory Gb: "+(ConfigurationHelper.getMemorySizeInBytes(getApplicationContext())/1024/1024/1024));

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, AbstractProductScreenFragment.class, arguments)
                .commit();

//        if (isTouchScreen()) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .setReorderingAllowed(true)
//                    .replace(R.id.fragment_container_view, TouchScreenFragment.class, arguments)
//                    .commit();
//        } else {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .setReorderingAllowed(true)
//                    .replace(R.id.fragment_container_view, PlainScreenFragment.class, arguments)
//                    .commit();
//        }
    }

    private void downloadSound() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                if (!DownloadHelper.isSoundPresent(getApplicationContext())) {
                    Constraints constraint = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();

                    OneTimeWorkRequest downloadSoundWorker = new OneTimeWorkRequest.Builder(DownloadSoundServiceWorker.class)
                            .setConstraints(constraint)
                            .setInputData(new Data.Builder().putString("apikey", currentConfig.getAccountToken()).build())
                            .build();

                    WorkManager.getInstance(this)
                            .beginUniqueWork("download-sound", ExistingWorkPolicy.KEEP, downloadSoundWorker)
                            .enqueue();
                }
            }
        });
    }

    private void downloadMovieSupportImages() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(this, routefilms -> {
                    if (routefilms.size() > 0 && currentConfig.isLocalPlay()) {
                        for (Routefilm routefilm : routefilms) {
                            //SPECIFY INPUT
                            Data.Builder mediaDownloader = new Data.Builder();
                            mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
                            mediaDownloader.putString("localMediaServer", currentConfig.getPraxCloudMediaServerLocalUrl());

                            //COSNTRAINTS
                            Constraints constraint = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build();
                            //WORKREQUEST
                            OneTimeWorkRequest downloadMovieSupportImagesWorkRequest = new OneTimeWorkRequest.Builder(DownloadMovieImagesServiceWorker.class)
                                    .setConstraints(constraint)
                                    .setInputData(mediaDownloader.build())
                                    .addTag("support-images-routefilm-"+routefilm.getMovieId())
                                    .build();
                            //START WORKING
                            WorkManager.getInstance(this)
                                    .beginUniqueWork("download-support-images-"+routefilm.getMovieId(), ExistingWorkPolicy.KEEP, downloadMovieSupportImagesWorkRequest)
                                    .enqueue();
                        }
                    }
                });
            }
        });
    }

    private void periodicSyncDownloadMovieRouteParts(final String apikey) {
        Data.Builder mediaDownloader = new Data.Builder();
        mediaDownloader.putString("apikey", apikey);

        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest routepartsDownloadRequest = new PeriodicWorkRequest.Builder(DownloadRoutepartsServiceWorker.class, 35, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(mediaDownloader.build())
                .addTag("download-movieparts")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-pms-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, routepartsDownloadRequest);

    }

    private void startSingleDataIntegrityWorker(final String apikey) {
        Data.Builder mediaDownloader = new Data.Builder();
        mediaDownloader.putString("apikey", apikey);

        //COSNTRAINTS
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DataIntegrityCheckServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(mediaDownloader.build())
                .addTag("data-integrity")
                .build();

        WorkManager.getInstance(this)
                .beginUniqueWork("data-integrity", ExistingWorkPolicy.KEEP, oneTimeWorkRequest)
                .enqueue();
    }

    private void periodicCheckMovieFileDataIntegrity(final String apikey) {
        Data.Builder mediaDownloader = new Data.Builder();
        mediaDownloader.putString("apikey", apikey);

        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest checkMovieFileDataIntegrityRequest = new PeriodicWorkRequest.Builder(DataIntegrityCheckServiceWorker.class, 8, TimeUnit.HOURS)
                .setConstraints(constraint)
                .setInputData(mediaDownloader.build())
                .addTag("check-data-integrity")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("check-data-integrity-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, checkMovieFileDataIntegrityRequest);
    }

    private void downloadLocalMovies() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null && isStoragePermissionGranted()) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(this, routefilms -> {
                    if (routefilms.size()>0 && currentConfig.isLocalPlay()) {
                        //CHECK IF ALL MOVIES FIT TO DISK
                        // Accumulate size of movies
                        long totalMovieFileSizeOnDisk = 0L;
                        for (Routefilm routefilm: routefilms) {
                            totalMovieFileSizeOnDisk += routefilm.getMovieFileSize();
                        }

                        LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(), "AllMovieDownloader ready to download","DEBUG", "");

                        if (DownloadHelper.canFileBeCopiedToLargestVolume(getApplicationContext(), totalMovieFileSizeOnDisk)) {
                            for (final Routefilm routefilm : routefilms) {
                                if (!DownloadHelper.isMoviePresent(getApplicationContext(), Movie.fromRoutefilm(routefilm))) {
                                    Constraints constraint = new Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build();

                                    Data.Builder mediaDownloader = new Data.Builder();
                                    mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
                                    mediaDownloader.putString("localMediaServer", currentConfig.getPraxCloudMediaServerLocalUrl());
                                    mediaDownloader.putString("apikey", currentConfig.getAccountToken());

                                    OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
                                            .setConstraints(constraint)
                                            .setInputData(mediaDownloader.build())
                                            .setBackoffCriteria(
                                                    BackoffPolicy.LINEAR,
                                                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                                    TimeUnit.MILLISECONDS)
                                            .addTag("routefilm-" + routefilm.getMovieId())
                                            .build();

                                    WorkManager.getInstance(this)
                                            .beginUniqueWork("download-movie-" + routefilm.getMovieId(), ExistingWorkPolicy.KEEP, oneTimeWorkRequest)
                                            .enqueue();
                                } else {
                                    LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(), routefilm.getMovieTitle()+":Movie already present","DEBUG", "");
                                }
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.warning_need_more_disk_capacity), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void syncMovieDatabasePeriodically(final String apikey) {
        Data.Builder syncData = new Data.Builder();
        syncData.putString("apikey",  apikey);
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest productMovieRequest = new PeriodicWorkRequest.Builder(ActiveProductMovieLinksServiceWorker.class, 15, TimeUnit.MINUTES)
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

        PeriodicWorkRequest productMoviePartsRequest = new PeriodicWorkRequest.Builder(UpdateRoutePartsServiceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("movieparts-link")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-routeparts-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productMoviePartsRequest);

        PeriodicWorkRequest productMovieSoundsRequest = new PeriodicWorkRequest.Builder(SoundInformationServiceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("moviesounds-sync")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-sounds-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, productMovieSoundsRequest);
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
}
