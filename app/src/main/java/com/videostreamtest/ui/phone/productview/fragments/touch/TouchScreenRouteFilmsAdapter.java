package com.videostreamtest.ui.phone.productview.fragments.touch;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;

public class TouchScreenRouteFilmsAdapter extends  RecyclerView.Adapter<TouchScreenRouteFilmsViewHolder> {
    private static final String TAG = TouchScreenRouteFilmsAdapter.class.getSimpleName();

    private int selectedMovie = 0;

    private Routefilm[] routefilms;
    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    private boolean localPlay;
    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;
    private LinearLayout routeInformationBlock;

    public TouchScreenRouteFilmsAdapter(final Routefilm[] routefilms, final Product enteredProduct, final CommunicationDevice communicationDevice) {
        this.routefilms = routefilms;
        this.selectedProduct = enteredProduct;
        this.communicationDevice = communicationDevice;
        localPlay = (enteredProduct.getSupportStreaming()==0);
    }

    public void setCatalogRecyclerViewClickListener(final CatalogRecyclerViewClickListener catalogRecyclerViewClickListener) {
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
    }

    public void setRouteInformationBlock(final LinearLayout routeInformationBlock) {
        this.routeInformationBlock = routeInformationBlock;
    }

    @NonNull
    @Override
    public TouchScreenRouteFilmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_movie, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new TouchScreenRouteFilmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TouchScreenRouteFilmsViewHolder holder, int position) {
        if (selectedMovie == position) {
            ImageButton routeSceneryImage = holder.itemView.findViewById(R.id.routeImageCoverButton);
            routeSceneryImage.setFocusableInTouchMode(true);
            routeSceneryImage.setFocusable(true);
            routeSceneryImage.requestFocus();
        }
        holder.itemView.setSelected(selectedMovie == position);

        if (routefilms != null && routefilms.length > 0) {
            if (selectedProduct.getSupportStreaming()>0) {
                holder.bindStreaming(routefilms[position], selectedProduct, communicationDevice, position, routeInformationBlock, catalogRecyclerViewClickListener);
            } else {
                holder.bindStandalone(routefilms[position], selectedProduct, communicationDevice, position, routeInformationBlock, catalogRecyclerViewClickListener);
            }
        }
    }

    public void setSelectedMovie(int position) {
        this.selectedMovie = position;
    }

    public int getSelectedMovie() {
        return selectedMovie;
    }

    public Routefilm getItemAt(final int position) {
        return routefilms[position];
    }

    @Override
    public int getItemCount() {
        //Define maximum items to show
        return routefilms.length;
    }
}
