package com.videostreamtest.ui.phone.splash;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
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
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.ProductType;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.productview.ProductActivity;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.workers.InstallPackageServiceWorker;
import com.videostreamtest.workers.UpdatePackageServiceWorker;


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

        requestDrawOverlayPermission();

//        resetBluetoothAdapter();

        loadTimer = new Handler(Looper.getMainLooper());

        checkForUpdates();

        //New way
        splashViewModel.getCurrentConfig().observe(this, config -> {
            if (config != null) {
                loadTimer.removeCallbacksAndMessages(null);
                Log.d(TAG, "Token :: " + config.getAccountToken() + " > Current =  " + config.isCurrent());
                //If there's internet, retrieve account info and/or synchronize data
                ConfigurationHelper.loadExternalData(this, config.getAccountToken());


                /**
                 * TODO  if accounttoken is valid (create worker)
                 *  then continue to next page which is:
                 *  based on number of products > 1 == ProductPicker
                 *  based on number of products == 1 > Load product
                 *  based on number of products == 0 > login activity
                 */
                splashViewModel.getAccountProducts(config.getAccountToken(), !config.isLocalPlay()).observe(this, products -> {
                    if (products != null) {
//                        config.setProductCount(products.size());
//                        splashViewModel.updateConfiguration(config);
                        if(products.size() > 0) {
                            Log.d(TAG, "Set current configuration");
//                            if (products.size() > 1 ) {
                                if (!productPickerLoaded) {
                                    productPickerLoaded = true;
                                    Log.d(TAG, "Number of products :: " + products.size());
                                    for (final com.videostreamtest.config.entity.Product p : products) {
                                        Log.d(TAG, "Product ID :: " + p.getUid() + " :: Product name :: " + p.getProductName());
                                    }
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
                            Runnable showLoginScreen = new Runnable() {
                                public void run() {
                                    config.setCurrent(false);
                                    splashViewModel.updateConfiguration(config);
                                    //Redirect to login activity
                                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                                    //Close this activity and release resources
                                    SplashActivity.this.finish();
                                }
                            };
                            //Redirect to login activity if timer exceeds 5 seconds
                            loadTimer.postDelayed( showLoginScreen, 15000 );
                        }
                    }
                });
            } else {
                Log.d(TAG, "Configuration == null ");

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

    private ProductType getProductType(final String productName, final boolean supportStreaming) {
        // Default value is PRAXFIT STREAM
        ProductType returnValue = ProductType.PRAXFIT_STREAM;
        // Walk through list of known product types
        for (ProductType productType: ProductType.values()) {
            // if product name contains product type name
            if (productName.contains(getProductTypeBasics(productType)[0])) {
                // if product which contains the producttype name also contains streaming edition
                if (supportStreaming && getProductTypeBasics(productType)[1].equalsIgnoreCase("stream")){
                    //return streaming product type
                    returnValue = productType;
                } else {
                    // else return stand-alone product type
                    returnValue = productType;
                }
            }
        }return returnValue;
    }

    private String[] getProductTypeBasics(final ProductType productType) {
        return productType.name().split("_");
    }

    private void requestDrawOverlayPermission() {
        // Check if Android M or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show alert dialog to the user saying a separate permission is needed
            requestPermissions(new String[]{Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            if(!Settings.canDrawOverlays(this)) {
               Log.d(TAG, "checkpermission "+getPackageManager().checkPermission(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, getPackageName())) ;
            }
        }
    }

    private void resetBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            Toast.makeText(this, "Bluetooth restarting...", Toast.LENGTH_LONG).show();
        }

        Handler resetBleHandler = new Handler();
        Runnable bleDeviceRunnable = new Runnable() {
            @Override
            public void run() {
                BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                assert bluetoothManager != null;
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                    Toast.makeText(SplashActivity.this, "Bluetooth succesfully started!", Toast.LENGTH_LONG).show();
                }
            }
        };

        resetBleHandler.postDelayed(bleDeviceRunnable, 8000);

    }

}
