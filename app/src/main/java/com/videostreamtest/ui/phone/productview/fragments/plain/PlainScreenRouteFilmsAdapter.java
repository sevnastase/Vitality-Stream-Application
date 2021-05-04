package com.videostreamtest.ui.phone.productview.fragments.plain;

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
import com.videostreamtest.ui.phone.catalog.AvailableMediaViewHolder;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;

public class PlainScreenRouteFilmsAdapter extends  RecyclerView.Adapter<PlainScreenRouteFilmsViewHolder>{
    private static final String TAG = PlainScreenRouteFilmsAdapter.class.getSimpleName();

    private int selectedMovie = 0;

    private Routefilm[] routefilms;
    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    private boolean localPlay;
    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;
    private LinearLayout routeInformationBlock;

    public PlainScreenRouteFilmsAdapter(final Routefilm[] routefilms, final Product enteredProduct, final CommunicationDevice communicationDevice) {
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
    public PlainScreenRouteFilmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_movie, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new PlainScreenRouteFilmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlainScreenRouteFilmsViewHolder holder, int position) {
        if (selectedMovie == position) {
            ImageButton routeImagecover = holder.itemView.findViewById(R.id.routeImageCoverButton);
            routeImagecover.setFocusableInTouchMode(true);
            routeImagecover.setFocusable(true);
            routeImagecover.requestFocus();
        }
        holder.itemView.setSelected(selectedMovie == position);

        if (routefilms.length>0) {
            if(localPlay) {
                holder.bindStandaloneMode(routefilms[position], position, selectedProduct, communicationDevice, routeInformationBlock, catalogRecyclerViewClickListener);
            } else {
                holder.bindStreamingMode(routefilms[position], position, selectedProduct, communicationDevice, routeInformationBlock, catalogRecyclerViewClickListener);
            }
        }
    }

    @Override
    public int getItemCount() {
        return routefilms.length;
    }
}
