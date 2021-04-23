package com.videostreamtest.ui.phone.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.ProductType;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.productview.ProductActivity;
import com.videostreamtest.ui.phone.profiles.AddProfileActivity;
import com.videostreamtest.ui.phone.profiles.ProfileAdapter;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.workers.ActiveConfigurationServiceWorker;
import com.videostreamtest.workers.ActiveProductsServiceWorker;
import com.videostreamtest.workers.AvailableMediaServiceWorker;
import com.videostreamtest.workers.AvailableRoutePartsServiceWorker;
import com.videostreamtest.workers.DownloadServiceWorker;
import com.videostreamtest.workers.NetworkInfoWorker;
import com.videostreamtest.workers.ProfileServiceWorker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
                loadExternalData(config.getAccountToken());

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
                            Log.d(TAG, "Unset current configuration");
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

            //Check if account token is valid
//            if (validateAccountToken(config.getAccountToken())) {
//                Log.d(TAG, "DB ACCOUNTTOKEN :: "+config.getAccountToken());
//                //If there's internet, retrieve account info and/or synchronize data
//
//                //Based on number of products continue
//                splashViewModel.getAccountProducts(config.getAccountToken()).observe(this, productlist ->{
//                    int productCount = productlist.size();
//                    Log.d(TAG, "Product Count: "+productCount);
//                    if (productCount > 0) {
//                        //There are active and available products
//                        if (productCount == 1) {
//                            // There is 1 (one) active product
//                        /*
//                        Step 1: Determine productType
//                        Step 2: map product to product type value
//                        Step 3: switch statement and start linked product activity
//                         */
//                            ProductType productToLoad = getProductType(productlist.get(0).getProductName(), (productlist.get(0).getSupportStreaming()>0));
//                            switch(productToLoad) {
//                                case PRAXFIT_STREAM:
//                                    Log.d(TAG, "PraxFit Stream found!");
//                                    //startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
//                                    break;
//                                case PRAXFIT_LOCAL:
//                                    break;
//                                case PRAXFILM_LOCAL:
//                                    break;
//                                case PRAXFILM_STREAM:
//                                    break;
//                                case PRAXSPIN_LOCAL:
//                                    break;
//                                case PRAXSPIN_STREAM:
//                                    break;
//                                default:
//
//                            }
//                            //Close this activity and release resources
//                            //finish();
//                        } else {
//                            // There are multiple active products linked to this account
//                            //startActivity(new Intent(SplashActivity.this, ProductPickerActivity.class));
//                            //Close this activity and release resources
//                            //finish();
//                        }
//                    } else {
//                        //TODO: May be some releasing current loaded profile
//                        //Redirect to login activity
//                        //startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//                        //Close this activity and release resources
//                        //finish();
//                    }
//                });
//
//            } else {
//                // Redirect to login activity
////                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//                //Close this activity and release resources
////                finish();
//            }
        });

//        requestDrawOverlayPermission();
        /*
        TODO: when API key is available, check is account is valid and still valid, else throw them to login.
            MAYBE ALREADY DONE BY HAVING NO AVAILABLE PRODUCTS
         */

//        if (apiKey != null) {
//            /*
//            TODO: Retrieve products of the customer if not already in sharedPreferences or room database
//                - Based on the number of products a productpicker will appear or not
//                - Based on one product the first page of the product will appear with switch statement
//
//                - Exception is for demo accounts which have all the products || OR || demo accounts have subscriptions which repeatedly are extended.
//             */
//
//            getCustomerActiveProducts(apiKey);
//
//            splashViewModel.getProductList().observe(this, products -> {
//                int productCount = products.size();
//                if (productCount>0) {
//                    //There are active and available products
//                    if (productCount == 1) {
//                        // There is 1 (one) active product
//                        /*
//                        Step 1: Determine productType
//                        Step 2: map product to product type value
//                        Step 3: switch statement and start linked product activity
//                         */
//                        ProductType productToLoad = getProductType(products.get(0).getProductName(), (products.get(0).getSupportStreaming()>0));
//                        switch(productToLoad) {
//                            case PRAXFIT_STREAM:
//                                startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
//                                break;
//                            case PRAXFIT_LOCAL:
//                                break;
//                            case PRAXFILM_LOCAL:
//                                break;
//                            case PRAXFILM_STREAM:
//                                break;
//                            case PRAXSPIN_LOCAL:
//                                break;
//                            case PRAXSPIN_STREAM:
//                                break;
//                            default:
//
//                        }
//                        //Close this activity and release resources
//                        finish();
//                    } else {
//                        // There are multiple active products linked to this account
//                        startActivity(new Intent(SplashActivity.this, ProductPickerActivity.class));
//                        //Close this activity and release resources
//                        finish();
//                    }
//                } else {
//                    // There are no active products available for this account
//                    // Show message to user no products are available on this account
//                    Toast.makeText(getApplicationContext(), getString(R.string.products_none_available_warning), Toast.LENGTH_LONG).show();
//                    // Release resources and reset apikey in sharedpreferences
//                    SharedPreferences sp = getApplication().getSharedPreferences("app",0);
//                    SharedPreferences.Editor editor = sp.edit();
//                    editor.clear();
//                    editor.commit();
//                    // Redirect to login activity
//                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//                    //Close this activity and release resources
//                    finish();
//                }
//            });
//        } else {
//            // No Api key is available, redirecting to login activity to acquire one
//            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//            //Close this activity and release resources
//            finish();
//        }
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
