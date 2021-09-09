package com.videostreamtest.ui.phone.productview;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.productview.fragments.PlainScreenFragment;
import com.videostreamtest.ui.phone.productview.fragments.TouchScreenFragment;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.ui.phone.screensaver.ScreensaverActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.DownloadMovieImagesServiceWorker;
import com.videostreamtest.workers.DownloadMovieServiceWorker;
import com.videostreamtest.workers.DownloadRoutepartsServiceWorker;
import com.videostreamtest.workers.DownloadSoundServiceWorker;
import com.videostreamtest.workers.LocalMediaServerAvailableServiceWorker;
import com.videostreamtest.workers.UpdateRegisteredMovieServiceWorker;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
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

                Log.d(getClass().getSimpleName(), "currentConfig pCount: "+currentConfig.getProductCount() + " Bundle pCount: 1");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Started Product: "+selectedProduct.getProductName(), "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Screensize: wxh: "+width+" x "+height, "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Localip: "+getLocalIpAddress(), "DEBUG", "");

                Bundle arguments = getIntent().getExtras();
                arguments.putString("communication_device", currentConfig.getCommunicationDevice());

                syncMovieDatabasePeriodically(currentConfig.getAccountToken());
                loadFragmentBasedOnScreenType(arguments);

                appBuildNumber.setText(ConfigurationHelper.getVersionNumber(getApplicationContext())+":"+currentConfig.getAccountToken());

                if (currentConfig.getProductCount() > 1) {
                    signoutButton.setText(getString(R.string.productpicker_close_button_text));
                }
                signoutButton.setOnClickListener(view -> {
                    if (currentConfig.getProductCount() == 1) {
                        productViewModel.signoutCurrentAccount(currentConfig);
                        //Cancel all workers (in case of downloading)
                        WorkManager
                                .getInstance(getApplicationContext())
                                .cancelAllWork();
                    }
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

        if (!selectedProduct.getCommunicationType().toLowerCase().contains("none")) {
            Intent bleService = new Intent(getApplicationContext(), BleService.class);
            startService(bleService);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        downloadMovieSupportImages();
        downloadSound();
        downloadMovieRouteParts();
        downloadLocalMovies();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetScreensaverTimer();
    }

    private void loadFragmentBasedOnScreenType(final Bundle arguments) {
        if (isTouchScreen()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, TouchScreenFragment.class, arguments)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, PlainScreenFragment.class, arguments)
                    .commit();
        }
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

    private void downloadMovieRouteParts() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(this, routefilms -> {
                    if (routefilms.size() > 0 && currentConfig.isLocalPlay()) {
                        for (Routefilm routefilm : routefilms) {
                            Data.Builder mediaDownloader = new Data.Builder();
                            mediaDownloader.putString("apikey",  currentConfig.getAccountToken());
                            mediaDownloader.putInt("movie-id", routefilm.getMovieId());
                            Constraints constraint = new Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build();
                            OneTimeWorkRequest routepartsWorkRequest = new OneTimeWorkRequest.Builder(DownloadRoutepartsServiceWorker.class)
                                    .setConstraints(constraint)
                                    .setInputData(mediaDownloader.build())
                                    .addTag("routeparts-routefilm-"+routefilm.getMovieId())
                                    .build();
                            WorkManager.getInstance(this)
                                    .beginUniqueWork("download-movieparts-"+routefilm.getMovieId(), ExistingWorkPolicy.KEEP, routepartsWorkRequest)
                                    .enqueue();
                        }
                    }
                });
            }
        });
    }

    private void downloadLocalMovies() {
        productViewModel.getCurrentConfig().observe(this, currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(this, routefilms -> {
                    if (routefilms.size()>0 && currentConfig.isLocalPlay()) {
                        //CHECK IF ALL MOVIES FIT TO DISK
                        // Accumulate size of movies
                        long totalMovieFileSizeOnDisk = 0L;
                        for (Routefilm routefilm: routefilms) {
                            totalMovieFileSizeOnDisk += routefilm.getMovieFileSize();
                        }

                        if (DownloadHelper.canFileBeCopied(getApplicationContext(), totalMovieFileSizeOnDisk)) {
                            for (Routefilm routefilm : routefilms) {
                                if (!DownloadHelper.isMoviePresent(getApplicationContext(), Movie.fromRoutefilm(routefilm))) {
                                    Constraints constraint = new Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build();

                                    Data.Builder mediaDownloader = new Data.Builder();
                                    mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
                                    mediaDownloader.putString("localMediaServer", currentConfig.getPraxCloudMediaServerLocalUrl());

                                    OneTimeWorkRequest availabilityWorker = new OneTimeWorkRequest.Builder(LocalMediaServerAvailableServiceWorker.class)
                                            .setConstraints(constraint)
                                            .setInputData(mediaDownloader.build())
                                            .addTag("local-" + routefilm.getMovieId())
                                            .build();

                                    OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
                                            .setConstraints(constraint)
                                            .setInputData(mediaDownloader.build())
                                            .addTag("routefilm-" + routefilm.getMovieId())
                                            .build();

                                    WorkManager.getInstance(this)
                                            .beginUniqueWork("download-movie-" + routefilm.getMovieId(), ExistingWorkPolicy.KEEP, availabilityWorker)
                                            .then(oneTimeWorkRequest)
                                            .enqueue();
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
        PeriodicWorkRequest syncDatabaseWorkRequest = new PeriodicWorkRequest.Builder(UpdateRegisteredMovieServiceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraint)
                .setInputData(syncData.build())
                .addTag("sync-database")
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sync-database-"+apikey, ExistingPeriodicWorkPolicy.REPLACE, syncDatabaseWorkRequest);
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

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
