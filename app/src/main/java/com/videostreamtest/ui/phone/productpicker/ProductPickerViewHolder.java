package com.videostreamtest.ui.phone.productpicker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.routefilmpicker.RoutefilmPickerActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProductPickerViewHolder extends RecyclerView.ViewHolder {

    final static String TAG = ProductPickerViewHolder.class.getSimpleName();

    private ImageButton productButton;
    private AtomicBoolean isNavigating = new AtomicBoolean(false);

    public ProductPickerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(Product product, int position) {
        productButton = itemView.findViewById(R.id.product_avatar);

        final Uri productLogoPath = product.getAppropriateProductLogoPath(itemView.getContext());
        Picasso.get()
                .load(productLogoPath)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .resize(300, 225)
                .into(productButton);

        if (!isTouchScreen()) {
            initBorders();
            initOnFocusChangeListener();
        }
        initOnClickListener(product);
    }

    private void initBorders() {
        drawSelectionBorder();
        undrawSelectionBorder();

        if (productButton.isSelected() ) {
            drawSelectionBorder();
        } else {
            undrawSelectionBorder();
        }
    }

    private void drawSelectionBorder() {
        final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_blue_border);
        productButton.setBackground(border);
        productButton.setAlpha(1.0f);
    }

    private void undrawSelectionBorder() {
        productButton.setBackground(null);
        productButton.setAlpha(0.7f);
    }

    private void initOnFocusChangeListener() {
        productButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "hasFocus: "+hasFocus);
                itemView.setSelected(true);
                if (hasFocus) {
                    drawSelectionBorder();
                } else {
                    undrawSelectionBorder();
                }
            }
        });
    }

    public void initOnClickListener(final Product product) {
        //Set onclick on imagebutton
        productButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNavigating.compareAndSet(false, true)) return;

                productButton.requestFocus();

                Bundle arguments = new Bundle();
                arguments.putString("product_object", new GsonBuilder().create().toJson(product, Product.class));

                Intent productView = new Intent(view.getContext(), RoutefilmPickerActivity.class);
                productView.putExtras(arguments);

                SharedPreferences sharedPreferences = view.getContext().getSharedPreferences("app", Context.MODE_PRIVATE);
                String accounttoken = sharedPreferences.getString("apikey", "");

                PraxtourDatabase.databaseWriterExecutor.execute(()->{
                    PraxtourDatabase.getDatabase(view.getContext().getApplicationContext()).usageTrackerDao().setSelectedProduct(accounttoken, product.getId());
                    Log.d(TAG, "Written to db: "+accounttoken+" , "+product.getId());
                });

                view.getContext().startActivity(productView);
                new Handler().postDelayed(() -> {
                    if (isNavigating != null) {
                        isNavigating.set(false);
                    }
                }, 250);
            }
        });
    }

    private boolean isTouchScreen() {
        return itemView.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

}
