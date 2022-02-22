package com.videostreamtest.ui.phone.productpicker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.navigation.NavigationView;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.PermissionHelper;
import com.videostreamtest.ui.phone.screensaver.ScreensaverActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.utils.VideoLanLib;
import com.videostreamtest.workers.PeriodicInstallPackageServiceWorker;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

public class ProductPickerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final static String TAG = ProductPickerActivity.class.getSimpleName();

    private ProductPickerViewModel productPickerViewModel;

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    private Button settingsButton;

    private Handler screensaverhandler;
    private Looper screensaverLooper;
    private Runnable screensaverRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        productPickerViewModel.getCurrentConfig().observe(this, config -> {
            if (config != null) {
                PermissionHelper.requestPermission(getApplicationContext(), this, config);

                checkForUpdatePeriodically(config.getAccountToken());

                LogHelper.WriteLogRule(getApplicationContext(), config.getAccountToken(),"isTouchscreen: "+isTouchScreen(), "DEBUG", "");

                productPickerViewModel.getAccountProducts(config.getAccountToken(), !config.isLocalPlay()).observe(this, products -> {
                    if (products!=null && products.size()>0) {
                        boolean sensorNeeded = false;
                        for (final Product product: products) {
                            if (!product.getCommunicationType().equals("NONE")) {
                                sensorNeeded = true;
                            }
                        }
                        if (sensorNeeded) {
                            startBleService();
                        }
                    }
                });

                if (config.isLocalPlay()) {
                    hideMenuItem(R.id.nav_logout);
                } else {
                    hideMenuItem(R.id.nav_downloads);
                }

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
            }
            Log.d(TAG, "LIBVLC hash: "+VideoLanLib.getLibVLC(getApplicationContext()).hashCode());
        });
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
        // Check if Android M or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show alert dialog to the user saying a separate permission is needed
            requestPermissions(new String[]{
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
//                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION, //ANDROID 10 FOR BLE
//                    Manifest.permission.ACCESS_COARSE_LOCATION, //ANDROID 10 OR OLDER THEN 7.0 FOR BLE
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 2323);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "BLUETOOTH PERMISSION GRANTED");
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "BLUETOOTH_ADMIN PERMISSION GRANTED");
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "ACCESS_COARSE_LOCATION PERMISSION GRANTED");
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "ACCESS_FINE_LOCATION PERMISSION GRANTED");
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "ACCESS_BACKGROUND_LOCATION PERMISSION GRANTED");
            }
        }
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
            case R.id.nav_logout:
                navController.navigate(R.id.logoutFragment);
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    private void checkForUpdatePeriodically(final String apikey) {
        Data.Builder syncData = new Data.Builder();
        syncData.putString("apikey",  apikey);
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest productUpdaterRequest = new PeriodicWorkRequest.Builder(PeriodicInstallPackageServiceWorker.class, 20, TimeUnit.MINUTES)
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
            startService(bleService);
        }
    }

    private void hideMenuItem(final int id)
    {
        if (navView != null) {
            Menu nav_Menu = navView.getMenu();
            nav_Menu.findItem(id).setVisible(false);
        }
    }

}
