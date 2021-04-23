package com.videostreamtest.ui.phone.productpicker;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.productview.ProductActivity;

public class ProductPickerViewHolder extends RecyclerView.ViewHolder {

    final static String TAG = ProductPickerViewHolder.class.getSimpleName();

    private ImageButton productButton;

    public ProductPickerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(Product product, int position) {
        productButton = itemView.findViewById(R.id.product_avatar);

        //Set product image in button
        Picasso.get()
                .load(product.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productButton);

        initBorders();
        initOnFocusChangeListener();
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
                Log.d(TAG, "Selected ProductButton: "+getAdapterPosition()+" hasFocus: "+hasFocus);
                itemView.setSelected(true);
                if (hasFocus) {
                    drawSelectionBorder();
                } else {
                    undrawSelectionBorder();
                }
            }
        });
    }

    private void initOnClickListener(final Product product) {
        //Set onclick on imagebutton
        productButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                productButton.requestFocus();

                Bundle arguments = new Bundle();
                arguments.putString("product_object", new GsonBuilder().create().toJson(product, Product.class));

                Intent productView = new Intent(view.getContext(), ProductActivity.class);
                productView.putExtras(arguments);

                view.getContext().startActivity(productView);
            }
        });
    }
}
