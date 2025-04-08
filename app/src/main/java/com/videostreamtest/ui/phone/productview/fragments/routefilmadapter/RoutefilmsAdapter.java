package com.videostreamtest.ui.phone.productview.fragments.routefilmadapter;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.videostreamtest.constants.SharedPreferencesConstants;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RoutefilmsAdapter extends  RecyclerView.Adapter<RoutefilmsViewHolder> {
    private static final String TAG = RoutefilmsAdapter.class.getSimpleName();

    //Data Model
    private ProductViewModel productViewModel;

    //INFORMATION PARAMETERS
    private Product selectedProduct;
    private LinearLayout routeInformationBlock;

    //ELEMENTS FOR LIST WHICH IS ATTACHED TO THE ADAPTER
    private List<Routefilm> routefilmList = new ArrayList<>();

    private int selectedRoutefilm = 0;

    public RoutefilmsAdapter(final Product activeProduct,
                             final ProductViewModel productViewModel,
                             final LinearLayout routeInformationBlock) {
        this.selectedProduct = activeProduct;
        this.productViewModel = productViewModel;
        this.routeInformationBlock = routeInformationBlock;
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
        final ImageButton routeSceneryImage = holder.itemView.findViewById(R.id.routeImageCoverButton);
        holder.itemView.setSelected(selectedRoutefilm == position);
        if (routefilmList != null && routefilmList.size() > 0) {
            Log.d(TAG, "Binding view for "+routefilmList.get(position).getMovieTitle()+" on position: "+position);
            holder.bindProduct(routefilmList.get(position), selectedProduct, position, routeInformationBlock, this);
        }
        if (selectedRoutefilm == position) {
            if (!routeSceneryImage.hasFocus()) {
                routeSceneryImage.setFocusableInTouchMode(true);
                routeSceneryImage.setFocusable(true);
                routeSceneryImage.requestFocus();
            }
        }
        if (position % 4 == 0) {
            routeSceneryImage.setNextFocusLeftId(R.id.product_logout_button);
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
    }


    public int getCurrentSelectedRoutefilmPosition() {return this.selectedRoutefilm;}

    /**
     * Use the getCurrentSelectedRoutefilmPosition method to find the current routefilm at that
     * index
     *
     * @return currentFilm      The currently highlighted film
     */
    public Routefilm getCurrentSelectedRoutefilm() {
        Routefilm currentFilm = this.routefilmList.get(getCurrentSelectedRoutefilmPosition());
        Log.d(TAG, "Currently selected film: " + currentFilm);
        return currentFilm;
    }

    public void updateRoutefilmList(final List<Routefilm> requestedRoutefilmList) {
        if (requestedRoutefilmList != null && requestedRoutefilmList.size()>0) {
            for (final Routefilm routefilm: requestedRoutefilmList) {
                if (!isRoutefilmPresent(routefilm)) {
//                    if (selectedProduct.getSupportStreaming()==0 && DownloadHelper.isMoviePresent(productViewModel.getApplication(), Movie.fromRoutefilm(routefilm))) {
//                        this.routefilmList.add(routefilm);
//                    }
//                    if (selectedProduct.getSupportStreaming()==1) {
//                        this.routefilmList.add(routefilm);
//                    }
                    this.routefilmList.add(routefilm);
                }
                notifyDataSetChanged();
            }
            Log.d(TAG, "Update recyclerview");

//            try {
//                for (final Routefilm routefilm : this.routefilmList) {
//                    if (isRoutefilmRemoved(routefilm, requestedRoutefilmList)) {
//                        final int removedFilmPosition = this.routefilmList.indexOf(routefilm);
//                        this.routefilmList.remove(routefilm);
//                        notifyItemRemoved(removedFilmPosition);
//                    }
//                }
//            } catch (Exception concurrencyException) {
//                if (concurrencyException != null) {
//                    Log.e(TAG, ""+concurrencyException.getLocalizedMessage());
//                } else {
//                    Log.e(TAG, "ConcurrencyException");
//                }
//            }
        }
    }

    public void rebuildRoutefilmList(final List<Routefilm> requestedRoutefilmList) {
        if (requestedRoutefilmList == null || requestedRoutefilmList.isEmpty()) {
            return;
        }

        routefilmList.clear();
        updateRoutefilmList(requestedRoutefilmList);
    }

    private boolean isRoutefilmRemoved(final Routefilm routefilm, final List<Routefilm> requestedRoutefilmList) {
        if (requestedRoutefilmList!= null && requestedRoutefilmList.size()>0) {
            for (final Routefilm film: requestedRoutefilmList) {
                if (routefilm.getMovieId().intValue() == film.getMovieId().intValue()) {
                    return false;
                }
            }
        }
        return true;
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

    public List<Routefilm> getRoutefilms() {
        return this.routefilmList;
    }

}
