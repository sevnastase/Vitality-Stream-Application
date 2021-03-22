package com.videostreamtest.ui.phone.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.ProductType;
import com.videostreamtest.ui.phone.login.LoginActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.profiles.ProfilesActivity;
import com.videostreamtest.workers.ActiveProductsServiceWorker;

import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 2323;

    private SplashViewModel splashViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        SharedPreferences appPreferences = getApplication().getSharedPreferences("app",0);
        final String apiKey = appPreferences.getString("apiKey" ,null);

        requestDrawOverlayPermission();
        /*
        TODO: when API key is available, check is account is valid and still valid, else throw them to login.
            MAYBE ALREADY DONE BY HAVING NO AVAILABLE PRODUCTS
         */

        if (apiKey != null) {
            /*
            TODO: Retrieve products of the customer if not already in sharedPreferences or room database
                - Based on the number of products a productpicker will appear or not
                - Based on one product the first page of the product will appear with switch statement

                - Exception is for demo accounts which have all the products || OR || demo accounts have subscriptions which repeatedly are extended.
             */

            getCustomerActiveProducts(apiKey);

            splashViewModel.getProductList().observe(this, products -> {
                int productCount = products.size();
                if (productCount>0) {
                    //There are active and available products
                    if (productCount == 1) {
                        // There is 1 (one) active product
                        /*
                        Step 1: Determine productType
                        Step 2: map product to product type value
                        Step 3: switch statement and start linked product activity
                         */
                        ProductType productToLoad = getProductType(products.get(0));
                        switch(productToLoad) {
                            case PRAXFIT_STREAM:
                                startActivity(new Intent(SplashActivity.this, ProfilesActivity.class));
                                break;
                            case PRAXFIT_LOCAL:
                                break;
                            case PRAXFILM_LOCAL:
                                break;
                            case PRAXFILM_STREAM:
                                break;
                            case PRAXSPIN_LOCAL:
                                break;
                            case PRAXSPIN_STREAM:
                                break;
                            default:

                        }
                        //Close this activity and release resources
                        finish();
                    } else {
                        // There are multiple active products linked to this account
                        startActivity(new Intent(SplashActivity.this, ProductPickerActivity.class));
                        //Close this activity and release resources
                        finish();
                    }
                } else {
                    // There are no active products available for this account
                    // Show message to user no products are available on this account
                    Toast.makeText(getApplicationContext(), getString(R.string.products_none_available_warning), Toast.LENGTH_LONG).show();
                    // Release resources and reset apikey in sharedpreferences
                    SharedPreferences sp = getApplication().getSharedPreferences("app",0);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.clear();
                    editor.commit();
                    // Redirect to login activity
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    //Close this activity and release resources
                    finish();
                }
            });
        } else {
            // No Api key is available, redirecting to login activity to acquire one
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            //Close this activity and release resources
            finish();
        }
    }

    private ProductType getProductType(final Product product) {
        // Default value is PRAXFIT STREAM
        ProductType returnValue = ProductType.PRAXFIT_STREAM;
        // Walk through list of known product types
        for (ProductType productType: ProductType.values()) {
            // if product name contains product type name
            if (product.getProductName().contains(getProductTypeBasics(productType)[0])) {
                // if product which contains the producttype name also contains streaming edition
                if (product.getSupportStreaming()>0 && getProductTypeBasics(productType)[1].equalsIgnoreCase("stream")){
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
            // Launch the settings activity if the user prefers
            requestPermissions(new String[]{Settings.ACTION_MANAGE_OVERLAY_PERMISSION}, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
//            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:" + this.getPackageName()));
//            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    private void getCustomerActiveProducts(final String apikey) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);

        OneTimeWorkRequest productsRequest = new OneTimeWorkRequest.Builder(ActiveProductsServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("products")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(productsRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(productsRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("product-list");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            final Product products[] = objectMapper.readValue(result, Product[].class);
                            final ArrayList<Product> productArrayList = new ArrayList<>();
                            for (Product product: products) {
                                productArrayList.add(product);
                            }
                            splashViewModel.setProductList(productArrayList);

                        } catch (JsonMappingException jsonMappingException) {
                            Log.e(TAG, jsonMappingException.getLocalizedMessage());
                        } catch (JsonProcessingException jsonProcessingException) {
                            Log.e(TAG, jsonProcessingException.getLocalizedMessage());
                        }
                    }
                });
    }
}
