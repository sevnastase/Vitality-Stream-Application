package com.videostreamtest.ui.phone.productview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.BackoffPolicy;
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
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.log.DeviceInformation;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.ui.phone.screensaver.ScreensaverActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.synchronisation.ActiveProductMovieLinksServiceWorker;
import com.videostreamtest.workers.DataIntegrityCheckServiceWorker;
import com.videostreamtest.workers.download.DownloadFlagsServiceWorker;
import com.videostreamtest.workers.download.DownloadMovieImagesServiceWorker;
import com.videostreamtest.workers.download.DownloadMovieServiceWorker;
import com.videostreamtest.workers.download.DownloadRoutepartsServiceWorker;
import com.videostreamtest.workers.download.DownloadSoundServiceWorker;
import com.videostreamtest.workers.SoundInformationServiceWorker;
import com.videostreamtest.workers.synchronisation.SyncFlagsServiceWorker;
import com.videostreamtest.workers.synchronisation.SyncMovieFlagsServiceWorker;
import com.videostreamtest.workers.UpdateRegisteredMovieServiceWorker;
import com.videostreamtest.workers.UpdateRoutePartsServiceWorker;

import java.util.concurrent.TimeUnit;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

import static com.videostreamtest.utils.ApplicationSettings.NUMBER_OF_DOWNLOAD_RUNNERS;

import org.jetbrains.annotations.NotNull;

public class ProductActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final static String TAG = ProductActivity.class.getSimpleName();

    private ProductViewModel productViewModel;
    private Button signoutButton;
    private Button menuButton;
    private ImageView productLogo;
    private TextView appBuildNumber;
    private DrawerLayout drawerLayout;
    private NavigationView navView;

    private Handler screensaverhandler;
    private Looper screensaverLooper;
    private Runnable screensaverRunnable;

    private Product selectedProduct;

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
        menuButton = findViewById(R.id.product_menu_button);
        drawerLayout = findViewById(R.id.product_drawer_layout);
        navView = drawerLayout.findViewById(R.id.product_nav_view);

        selectedProduct = new GsonBuilder().create().fromJson(getIntent().getExtras().getString("product_object", "{}"), Product.class);
        Log.d(ProductActivity.class.getSimpleName(), "Product ID Loaded: "+selectedProduct.getId());

        //Set product logo in view
        Picasso.get()
                .load(selectedProduct.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productLogo);

        if (selectedProduct.getSupportStreaming()==1) {
            hideMenuItem(R.id.nav_downloads);
        }

        menuButton.setOnClickListener((clickedView) -> {
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

        signoutButton.setOnClickListener(view -> {
            ProductActivity.this.finish();
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        navView.setNavigationItemSelectedListener(this);

        productViewModel.getCurrentConfig().observe(this, currentConfig ->{
            if (currentConfig != null) {
                PermissionHelper.requestPermission(getApplicationContext(), this);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                int width = displayMetrics.widthPixels;
                Log.d(TAG, "Screen resolution: w:"+width+" h:"+height);
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Started Product: "+selectedProduct.getProductName(), "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Screensize: wxh: "+width+" x "+height, "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Localip: "+DownloadHelper.getLocalIpAddress(), "DEBUG", "");
                LogHelper.WriteLogRule(getApplicationContext(), currentConfig.getAccountToken(),"Density: sw-"+this.getResources().getDisplayMetrics().densityDpi, "DEBUG", "");

                //PERIODIC ACTIONS FOR STANDALONE SPECIFIC
                if (currentConfig.isLocalPlay()) {
                    periodicSyncDownloadMovieRouteParts(currentConfig.getAccountToken());
//                    periodicCheckMovieFileDataIntegrity(currentConfig.getAccountToken()); TODO: Make available through Account Config
//                    startSingleDataIntegrityWorker(currentConfig.getAccountToken());
                }

                appBuildNumber.setText(ConfigurationHelper.getVersionNumber(getApplicationContext())+":"+currentConfig.getAccountToken());

                Log.d(TAG, "LIBVLC hash: "+VideoLanLib.getLibVLC(getApplicationContext()).hashCode());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //TODO: switch to onetime only executions on startup phase or productpicker periodic updater.
        downloadFlags();
        downloadMovieSupportImages();
        downloadSound();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetScreensaverTimer();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem menuItem) {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
        NavController navController = navHostFragment.getNavController();
        switch (menuItem.getItemId()) {
            case R.id.nav_routes:
                navController.navigate(R.id.routeFragment);
                break;
            case R.id.nav_results:
//                navController.navigate(R.id.productPickerFragment);
                break;
            case R.id.nav_downloads:
                navController.navigate(R.id.downloadsFragment);
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    private void hideMenuItem(final int id)
    {
        if (navView != null) {
            Menu nav_Menu = navView.getMenu();
            nav_Menu.findItem(id).setVisible(false);
        }
    }

    private void downloadSound() {
        if (!DownloadHelper.isSoundPresent(getApplicationContext())) {
            Constraints constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest downloadSoundWorker = new OneTimeWorkRequest.Builder(DownloadSoundServiceWorker.class)
                    .setConstraints(constraint)
                    .setInputData(new Data.Builder().putString("apikey", AccountHelper.getAccountToken(getApplicationContext())).build())
                    .build();

            WorkManager.getInstance(this)
                    .beginUniqueWork("download-sound", ExistingWorkPolicy.KEEP, downloadSoundWorker)
                    .enqueue();
        }
    }

    private void downloadFlags() {
        if (AccountHelper.getAccountType(getApplicationContext()).equalsIgnoreCase("standalone")) {
            Constraints constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest downloadFlagsWorker = new OneTimeWorkRequest.Builder(DownloadFlagsServiceWorker.class)
                    .setConstraints(constraint)
                    .setInputData(new Data.Builder().putString("apikey", AccountHelper.getAccountToken(getApplicationContext())).build())
                    .build();

            WorkManager.getInstance(this)
                    .beginUniqueWork("download-sound", ExistingWorkPolicy.KEEP, downloadFlagsWorker)
                    .enqueue();
        }
    }

    private void downloadMovieSupportImages() {
        productViewModel.getRoutefilms(AccountHelper.getAccountToken(getApplicationContext())).observe(this, routefilms -> {
            if (routefilms.size() > 0 && AccountHelper.getAccountType(getApplicationContext()).equalsIgnoreCase("standalone")) {
                for (Routefilm routefilm : routefilms) {
                    //SPECIFY INPUT
                    Data.Builder mediaDownloader = new Data.Builder();
                    mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
                    mediaDownloader.putString("localMediaServer", AccountHelper.getAccountMediaServerUrl(getApplicationContext()));

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
