package com.videostreamtest.ui.phone.productview.fragments.touch;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;

public class TouchScreenRouteFilmsAdapter extends  RecyclerView.Adapter<TouchScreenRouteFilmsViewHolder> {

    private static final String TAG = TouchScreenRouteFilmsAdapter.class.getSimpleName();

    private Routefilm[] routefilms;
    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    public TouchScreenRouteFilmsAdapter(final Routefilm[] routefilms, final Product enteredProduct, final CommunicationDevice communicationDevice) {
        this.routefilms = routefilms;
        this.selectedProduct = enteredProduct;
        this.communicationDevice = communicationDevice;
        Log.d(TAG, "Routefilms found in local database: "+routefilms.length);
    }

    @NonNull
    @Override
    public TouchScreenRouteFilmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_movie_touch, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new TouchScreenRouteFilmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TouchScreenRouteFilmsViewHolder holder, int position) {
        if (routefilms != null && routefilms.length > 0) {
            if (selectedProduct.getSupportStreaming()>0) {
                holder.bindStreaming(routefilms[position], selectedProduct, communicationDevice, position);
            } else {
                holder.bindStandalone(routefilms[position], selectedProduct, communicationDevice, position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return routefilms.length;
    }
}
