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

import java.util.ArrayList;
import java.util.List;

import okhttp3.Route;

public class PlainScreenRouteFilmsAdapter extends  RecyclerView.Adapter<PlainScreenRouteFilmsViewHolder>{
    private static final String TAG = PlainScreenRouteFilmsAdapter.class.getSimpleName();

    private int selectedMovie = 0;

    private List<Routefilm> routefilmList = new ArrayList<>();

    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    private boolean localPlay;

    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;
    private LinearLayout routeInformationBlock;

    public PlainScreenRouteFilmsAdapter(final Product activeProduct, final CommunicationDevice communicationDevice) {
        this.selectedProduct = activeProduct;
        this.communicationDevice = communicationDevice;
        localPlay = (activeProduct.getSupportStreaming()==0);
    }

    public void setCatalogRecyclerViewClickListener(final CatalogRecyclerViewClickListener catalogRecyclerViewClickListener) {
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
    }

    public void setRouteInformationBlock(final LinearLayout routeInformationBlock) {
        this.routeInformationBlock = routeInformationBlock;
    }

    public void updateRoutefilmList(final List<Routefilm> requestedRoutefilmList) {
        boolean hasUpdates = false;
        if (requestedRoutefilmList != null && requestedRoutefilmList.size()>0) {
            for (final Routefilm routefilm: requestedRoutefilmList) {
                if (!isRoutefilmPresent(routefilm)) {
                    this.routefilmList.add(routefilm);
                    hasUpdates = true;
                }
            }
            for (final Routefilm routefilm: this.routefilmList) {
                if (isRoutefilmRemoved(routefilm, requestedRoutefilmList)) {
                    this.routefilmList.remove(routefilm);
                    hasUpdates = true;
                }
            }
            if (hasUpdates) {
                notifyDataSetChanged();
            }
        }
    }

    private boolean isRoutefilmRemoved(final Routefilm routefilm, final List<Routefilm> requestedRoutefilmList) {
        boolean isRemoved = true;
        if (requestedRoutefilmList!= null && requestedRoutefilmList.size()>0) {
            for (final Routefilm film: requestedRoutefilmList) {
                if (routefilm.getMovieId().intValue() == film.getMovieId().intValue()) {
                    isRemoved = false;
                }
            }
        }
        return isRemoved;
    }

    private boolean isRoutefilmPresent(final Routefilm routefilm) {
        if (this.routefilmList.size()>0) {
            for (final Routefilm film: this.routefilmList) {
                if (routefilm.getMovieId().intValue() == film.getMovieId().intValue()) {
                    return routefilm.getMovieId().intValue() == film.getMovieId().intValue();
                }
            }
            return false;
        } else {
            return false;
        }
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

        if (routefilmList != null && routefilmList.size() > 0) {
            if(localPlay) {
                holder.bindStandaloneMode(routefilmList.get(position), position, selectedProduct, communicationDevice, routeInformationBlock, catalogRecyclerViewClickListener);
            } else {
                holder.bindStreamingMode(routefilmList.get(position), position, selectedProduct, communicationDevice, routeInformationBlock, catalogRecyclerViewClickListener);
            }
        }
    }

    @Override
    public int getItemCount() {
        return routefilmList.size();
    }
}
