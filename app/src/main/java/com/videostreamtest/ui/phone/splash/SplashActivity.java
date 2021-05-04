package com.videostreamtest.ui.phone.splash;

import android.content.Intent;
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
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.ProductType;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.productview.ProductActivity;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.workers.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.ActiveProductsServiceWorker;
import com.videostreamtest.workers.AvailableMediaServiceWorker;
import com.videostreamtest.workers.AvailableRoutePartsServiceWorker;
import com.videostreamtest.workers.NetworkInfoWorker;
import com.videostreamtest.workers.ProfileServiceWorker;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
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

        loadTimer = new Handler(Looper.getMainLooper());

        //New way
        splashViewModel.getCurrentConfig().observe(this, config -> {
            Log.d(TAG, "CurrentConfig Value = "+config);
            Log.d(TAG, "CurrentConfig FakeTouch = "+getPackageManager().hasSystemFeature(PackageManager.FEATURE_FAKETOUCH));
            Log.d(TAG, "CurrentConfig Touch = "+getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN));
            Log.d(TAG, "CurrentConfig MultiTouch = "+getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH));
            if (config != null) {
                loadTimer.removeCallbacksAndMessages(null);
                Log.d(TAG, "Token :: " + config.getAccountToken() + " > Current =  " + config.isCurrent());
                //If there's internet, retrieve account info and/or synchronize data
                ConfigurationHelper.loadExternalData(this, config.getAccountToken());

//                TODO: Put the startActivities in a seperate method to be able to wait and then show the next activity
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException interruptedException) {
//                    Log.e(TAG, interruptedException.getLocalizedMessage());
//                }

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
                            if (products.size() > 1 ) {
                                if (!productPickerLoaded) {
                                    productPickerLoaded = true;
                                    Log.d(TAG, "Number of products :: " + products.size());
                                    for (final com.videostreamtest.config.entity.Product p : products) {
                                        Log.d(TAG, "Product ID :: " + p.getUid() + " :: Product name :: " + p.getProductName());
                                    }
                                    startActivity(new Intent(this, ProductPickerActivity.class));
                                    SplashActivity.this.finish();
                                }
                            } else {
                                if (!profileViewLoaded) {
                                    profileViewLoaded = true;
                                    // only one product so start product immediately based on streamingAccount
                                    Log.d(TAG, "Single product :: " + products.get(0).getProductName() + " standalone: "+config.isLocalPlay());
                                    if (config.isLocalPlay()) { //TODO: APPEND CHECK WITH: && products.get(0).getSupportStreaming().intValue()==0
                                        Bundle arguments = new Bundle();
                                        arguments.putString("product_object", new GsonBuilder().create().toJson(Product.fromProductEntity(products.get(0)), Product.class));

                                        Intent productView = new Intent(SplashActivity.this, ProductActivity.class);
                                        productView.putExtras(arguments);

                                        startActivity(productView);
                                    } else {
                                        startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
                                    }
                                    SplashActivity.this.finish();
                                }
                            }
                        } else {
                            //Execute when number of products is 0
                            Log.d(TAG, "Unset current configuration or product count = 0");
                            //TODO: in worker listener that acts when activeproductsworker is done.
                            // Because timing is of essence here the products can be 0 when this is executed
//                            if (false) {
//                                Toast.makeText(this, "No subscriptions available for this account.", Toast.LENGTH_LONG).show();
//                                Configuration configuration = config;
//                                configuration.setCurrent(false);
//                                splashViewModel.updateConfiguration(configuration);
//                            }
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
            requestPermissions(new String[]{Settings.ACTION_MANAGE_OVERLAY_PERMISSION}, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            if(!Settings.canDrawOverlays(this)) {
               Log.d(TAG, "checkpermission "+getPackageManager().checkPermission(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, getPackageName())) ;
            }
        }
    }

    private void loadExternalData(final String accountToken) {
        Constraints constraint = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //NetworkInfo
        Data.Builder networkData = new Data.Builder();
        OneTimeWorkRequest networkInfoRequest = new OneTimeWorkRequest.Builder(NetworkInfoWorker.class)
                .setConstraints(constraint)
                .setInputData(networkData.build())
                .addTag("connection-status")
                .build();

        //Account Configuration
        Data.Builder configurationData = new Data.Builder();
        configurationData.putString("apikey", accountToken);
        OneTimeWorkRequest accountConfigurationRequest = new OneTimeWorkRequest.Builder(ActiveConfigurationServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(configurationData.build())
                .addTag("accountconfiguration")
                .build();

        //Active Products
        Data.Builder productData = new Data.Builder();
        productData.putString("apikey", accountToken);
        OneTimeWorkRequest productsRequest = new OneTimeWorkRequest.Builder(ActiveProductsServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(productData.build())
                .addTag("products")
                .build();

        //Account Profiles
        Data.Builder profileData = new Data.Builder();
        profileData.putString("apikey", accountToken);
        OneTimeWorkRequest profilesRequest = new OneTimeWorkRequest.Builder(ProfileServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(profileData.build())
                .addTag("profiles")
                .build();

        //Routefilms
        Data.Builder routeFilmdata = new Data.Builder();
        routeFilmdata.putString("apikey", accountToken);
        OneTimeWorkRequest routefilmsRequest = new OneTimeWorkRequest.Builder(AvailableMediaServiceWorker.class)
                .setConstraints(constraint)
                .setInputData(routeFilmdata.build())
                .addTag("routefilms")
                .build();

        //Routeparts
        OneTimeWorkRequest routeMoviepartsRequest = new OneTimeWorkRequest.Builder(AvailableRoutePartsServiceWorker.class)
                .addTag("available-movieparts")
                .build();

        //Test Download media worker
//        Data.Builder mediaDownloader = new Data.Builder();
//        OneTimeWorkRequest downloadMediaRequest = new OneTimeWorkRequest.Builder(DownloadServiceWorker.class)
//                .setConstraints(constraint)
//                .setInputData(mediaDownloader.build())
//                .addTag("downloader")
//                .build();

        //Chain workers and enqueue them
        WorkManager
                .getInstance(this)
                .beginWith(networkInfoRequest)
//                .then(validateAccountTokenRequest)
                .then(accountConfigurationRequest)
                .then(profilesRequest)
                .then(productsRequest)
                .then(routefilmsRequest)
                .then(routeMoviepartsRequest)
//                .then(downloadMediaRequest)
                .enqueue();

//        //Listen to output of profiles
//        WorkManager.getInstance(this)
//                .getWorkInfoByIdLiveData(profilesRequest.getId())
//                .observe(this, workInfo -> {
//                    Log.d(TAG, "WORKERINFO PROFILES :: "+workInfo.getState().name());
//                    if (workInfo.getState() != null &&
//                            workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                        splashViewModel.getWorkerProgress().setValue(80);
//                    } else {
//                        Log.d(TAG, "workerInfo not succeeded :: " + workInfo.getState().name());
//                    }
//                });

//        //Listen to output of products
//        WorkManager.getInstance(this)
//                .getWorkInfoByIdLiveData(productsRequest.getId())
//                .observe(this, workInfo -> {
//                    Log.d(TAG, "WORKERINFO PRODUCTS :: "+workInfo.getState().name());
//                    Log.d(TAG, "WORKERINFO PRODUCTS :: "+workInfo.getState().isFinished());
//                    if (workInfo.getState() != null &&
//                            workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                        splashViewModel.getWorkerProgress().setValue(100);
//                    } else {
//                        Log.d(TAG, "workerInfo not succeeded :: " + workInfo.getState().name());
//                    }
//                });
    }

}
