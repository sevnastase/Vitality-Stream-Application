package com.videostreamtest.ui.phone.productpicker;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.workers.ActiveProductsServiceWorker;

public class ProductPickerActivity extends AppCompatActivity {
    private final static String TAG = ProductPickerActivity.class.getSimpleName();

    private RecyclerView productOverview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productpicker);

        /*
        Logout button actions
         */
        final Button signoutButton = findViewById(R.id.productpicker_logout_button);
        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getApplication().getSharedPreferences("app",0);
                SharedPreferences.Editor editor = sp.edit();
                editor.clear();
                editor.commit();
                finish();
            }
        });

        signoutButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    final Drawable border = v.getContext().getDrawable(R.drawable.imagebutton_blue_border);
                    signoutButton.setBackground(border);
                } else {
                    signoutButton.setBackground(null);
                }
            }
        });

        /*
        Haal de producten op om vervolgens in de recyclerview weer te geven
         */

        //Koppel de recyclerView aan de layout xml
        productOverview = findViewById(R.id.recyclerview_products);
        productOverview.setHasFixedSize(true);

        SharedPreferences appPreferences = getApplication().getSharedPreferences("app",0);
        final String apiKey = appPreferences.getString("apiKey" ,null);
        loadActiveProducts(apiKey);
    }

    private void loadActiveProducts(final String apikey) {
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
                            Product products[] = objectMapper.readValue(result, Product[].class);
                            //pass profiles to adapter
                            ProductPickerAdapter productPickerAdapter = new ProductPickerAdapter(products);
                            //set adapter to recyclerview
                            productOverview.setAdapter(productPickerAdapter);
                            //set recyclerview visible
                            productOverview.setVisibility(View.VISIBLE);

                            //For UI alignment in center with less then 5 products
                            int spanCount = 5;
                            if (products.length < 5) {
                                spanCount = products.length;
                            }
                            //Grid Layout met een max 5 kolommen breedte
                            final GridLayoutManager gridLayoutManager = new GridLayoutManager(this,spanCount);
                            //Zet de layoutmanager erin
                            productOverview.setLayoutManager(gridLayoutManager);
                        } catch (JsonMappingException jsonMappingException) {
                            Log.e(TAG, jsonMappingException.getLocalizedMessage());
                        } catch (JsonProcessingException jsonProcessingException) {
                            Log.e(TAG, jsonProcessingException.getLocalizedMessage());
                        }
                    }
                });
    }
}
