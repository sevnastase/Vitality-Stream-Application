package com.videostreamtest.ui.phone.productpicker;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;

public class ProductPickerAdapter extends RecyclerView.Adapter<ProductPickerViewHolder> {
    private final static String TAG = ProductPickerAdapter.class.getSimpleName();
    private Product[] productList;
    private int selectedProduct = 0;

    public ProductPickerAdapter(Product[] productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductPickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_single_product, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new ProductPickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductPickerViewHolder holder, int position) {
        Log.d(TAG, "Binding products");
        if (selectedProduct == position) {
            ImageButton productImageButton = holder.itemView.findViewById(R.id.product_avatar);
            productImageButton.setFocusableInTouchMode(true);
            productImageButton.setFocusable(true);
            productImageButton.requestFocus();
        }
        holder.itemView.setSelected(selectedProduct==position);
        if (productList.length > 0) {
            holder.bind(productList[position], position);
        }
    }

    @Override
    public int getItemCount() {
        return productList.length;
    }
}
