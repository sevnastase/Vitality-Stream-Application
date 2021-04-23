package com.videostreamtest.ui.phone.productview;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.productview.fragments.TouchScreenFragment;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

public class ProductActivity extends AppCompatActivity {

    private ProductViewModel productViewModel;
    private Button signoutButton;
    private ImageView productLogo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        signoutButton = findViewById(R.id.product_logout_button);
        productLogo = findViewById(R.id.product_logo_view);

        Product selectedProduct = new GsonBuilder().create().fromJson(getIntent().getExtras().getString("product_object", "{}"), Product.class);
        Log.d(ProductActivity.class.getSimpleName(), "Product ID Loaded: "+selectedProduct.getId());

        //Set product logo in view
        Picasso.get()
                .load(selectedProduct.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productLogo);

        productViewModel.getCurrentConfig().observe(this, currentConfig ->{
            if (currentConfig != null) {
                Bundle arguments = getIntent().getExtras();
                arguments.putString("communication_device", currentConfig.getCommunicationDevice());

                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.fragment_container_view, TouchScreenFragment.class, arguments)
                        .commit();

                if (currentConfig.getProductCount() > 1) {
                    signoutButton.setText("Close");
                }
                signoutButton.setOnClickListener(view -> {
                    if (currentConfig.getProductCount() == 1) {
                        productViewModel.signoutCurrentAccount(currentConfig);
                    }
                    ProductActivity.this.finish();
                });
            }
        });

        /**
         * TODO: pseudo steps
         *  1. get current configuration
         *  2. get current selected product
         *  3. Get screen type ( Touch or Plain )
         *  4. Is account for standalone or streaming
         *  4. Build up activity with fragments and/or nav_graphs
         */
    }

    private boolean isTouchScreen() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }
}
