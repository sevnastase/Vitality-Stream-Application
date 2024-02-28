package com.videostreamtest.ui.phone.productpicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.videoplayer.MQTTService;

import java.util.ArrayList;

public class ProductPickerAdapter extends RecyclerView.Adapter<ProductPickerViewHolder> {
    private final static String TAG = ProductPickerAdapter.class.getSimpleName();
    private Product[] productList;
    private int selectedProduct = 0;

    private Product currentFilm;
    ProductPickerViewHolder holder;

    public ProductPickerAdapter(Product[] productList) {
        this.productList = productList;
    }

//    private BroadcastReceiver motoLifeDataReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            ArrayList<String> motoLifeData = intent.getStringArrayListExtra("motoLifeData");
//
//            Log.d(TAG, "motoLifeDat.get(0) = " + motoLifeData.get(0));
//            if (motoLifeData.get(0) == "StartLeg" || motoLifeData.get(0) == "StartArm") {
//                Log.d(TAG, "is current film null: " + String.valueOf(currentFilm != null));
//                if (currentFilm != null) {
//                    Log.d(TAG, "currentFilm: " + currentFilm);
//                    holder.startFilm(currentFilm);
//                }
//            }
//        }
//    };

//    public void registerReceiver(Context context) {
//        if (context != null) {
//            LocalBroadcastManager.getInstance(context).registerReceiver(motoLifeDataReceiver,
//                    new IntentFilter("com.videostreamtest.MQTT_DATA_UPDATE"));
//        }
//    }
//
//    public void unregisterReceiver(Context context) {
//        LocalBroadcastManager.getInstance(context).unregisterReceiver(motoLifeDataReceiver);
//    }

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
            currentFilm = productList[position];
        }
    }

    @Override
    public int getItemCount() {
        return productList.length;
    }
}
