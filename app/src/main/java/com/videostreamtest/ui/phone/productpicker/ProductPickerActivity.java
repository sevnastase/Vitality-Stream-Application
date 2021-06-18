package com.videostreamtest.ui.phone.productpicker;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.workers.ActiveProductsServiceWorker;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

public class ProductPickerActivity extends AppCompatActivity {
    private final static String TAG = ProductPickerActivity.class.getSimpleName();

    private ProductPickerViewModel productPickerViewModel;
    private RecyclerView productOverview;
    private boolean refreshData = false;

    private Button signoutButton;

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

         /*
        Logout button
         */
        signoutButton = findViewById(R.id.productpicker_logout_button);
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

        productPickerViewModel.getCurrentConfig().observe(this, config -> {
            if (config != null) {
                if (refreshData) {
                    refreshData = false;
                    ConfigurationHelper.loadExternalData(this, config.getAccountToken());
                }
                // Add action onClick to signout button
                signoutButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences sp = getApplication().getSharedPreferences("app",0);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.clear();
                        editor.commit();
                        Log.d(TAG, "Finish productpicker");

                        config.setCurrent(false);
                        productPickerViewModel.updateConfiguration(config);

                        //Cancel all workers (in case of downloading)
                        WorkManager
                                .getInstance(getApplicationContext())
                                .cancelAllWork();

                        ProductPickerActivity.this.finish();
                    }
                });

                if(config.isLocalPlay()) {
                    signoutButton.setVisibility(View.GONE);
                }

                productPickerViewModel.getAccountProducts(config.getAccountToken(), !config.isLocalPlay()).observe(this, products ->{

                    List<Product> productList = new ArrayList<>();
                    if (products.size()>0) {
                        for (com.videostreamtest.config.entity.Product extProd : products) {
                            Product addProd = new Product();
                            addProd.setId(extProd.getUid());
                            addProd.setDefaultSettingsId(0);
                            addProd.setProductLogoButtonPath(extProd.getProductLogoButtonPath());
                            addProd.setSupportStreaming(extProd.getSupportStreaming());
                            addProd.setProductName(extProd.getProductName());
                            addProd.setBlocked(extProd.getBlocked());
                            addProd.setCommunicationType(extProd.getCommunicationType());
                            productList.add(addProd);
                        }
                    }

                    ProductPickerAdapter productPickerAdapter = new ProductPickerAdapter(productList.toArray(new Product[0]));
                    //set adapter to recyclerview
                    productOverview.setAdapter(productPickerAdapter);
                    //set recyclerview visible
                    productOverview.setVisibility(View.VISIBLE);

                    //For UI alignment in center with less then 5 products
                    int spanCount = 5;
                    if (products.size() < 5) {
                        spanCount = products.size();
                    }
                    //Grid Layout met een max 5 kolommen breedte
                    final GridLayoutManager gridLayoutManager = new GridLayoutManager(this,spanCount);
                    //Zet de layoutmanager erin
                    productOverview.setLayoutManager(gridLayoutManager);
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData = true;
    }
}
