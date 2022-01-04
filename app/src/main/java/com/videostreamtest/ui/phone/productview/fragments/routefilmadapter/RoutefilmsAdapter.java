package com.videostreamtest.ui.phone.productview.fragments.routefilmadapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Route;

public class RoutefilmsAdapter extends  RecyclerView.Adapter<RoutefilmsViewHolder> {
    private static final String TAG = RoutefilmsAdapter.class.getSimpleName();

    //Data Model
    private ProductViewModel productViewModel;

    //INFORMATION PARAMETERS
    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    //ELEMENTS FOR LIST WHICH IS ATTACHED TO THE ADAPTER
    private List<Routefilm> routefilmList = new ArrayList<>();
    private int selectedRoutefilm = 0;

    public RoutefilmsAdapter(final Product activeProduct, final ProductViewModel productViewModel) {
        this.selectedProduct = activeProduct;
        this.productViewModel = productViewModel;
    }

    @NonNull
    @NotNull
    @Override
    public RoutefilmsViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_movie, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new RoutefilmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RoutefilmsViewHolder holder, int position) {
        holder.itemView.setSelected(selectedRoutefilm == position);
        if (selectedRoutefilm == position) {
            final ImageButton routeSceneryImage = holder.itemView.findViewById(R.id.routeImageCoverButton);
            routeSceneryImage.setFocusableInTouchMode(true);
            routeSceneryImage.setFocusable(true);
            routeSceneryImage.requestFocus();
        }
        if (routefilmList != null && routefilmList.size() > 0) {
            holder.bindProduct(routefilmList.get(position), selectedProduct, position);
        }
    }

    @Override
    public int getItemCount() {
        if (routefilmList == null) {
            return 0;
        } else {
            return routefilmList.size();
        }
    }

    //ROUTEFILMS ADAPTER LIST MUTATION METHODS
    public void setSelectedRoutefilm(int position) {
        this.selectedRoutefilm = position;
//        notifyDataSetChanged();
    }

    public List<Routefilm> getRoutefilmList() {
        return this.routefilmList;
    }

    public int getSelectedRoutefilm() {return this.selectedRoutefilm;}

    //TODO: CHANGE TO BleDeviceInformationAdapter like behaviour
    public void updateRoutefilmList(final List<Routefilm> requestedRoutefilmList) {
        if (requestedRoutefilmList != null && requestedRoutefilmList.size()>0) {
            for (final Routefilm routefilm: requestedRoutefilmList) {
                if (!isRoutefilmPresent(routefilm)) {
                    this.routefilmList.add(routefilm);
//                    notifyItemInserted(routefilmList.indexOf(routefilm));
//                    if (this.routefilmList.size() == (requestedRoutefilmList.size()-1)) {
                        notifyDataSetChanged();
//                    }
                }
            }
            for (final Routefilm routefilm: this.routefilmList) {
                if (isRoutefilmRemoved(routefilm, requestedRoutefilmList)) {
                    final int removedFilmPosition = this.routefilmList.indexOf(routefilm);
                    this.routefilmList.remove(routefilm);
                    notifyItemRemoved(removedFilmPosition);
                }
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
}
