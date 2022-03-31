package com.videostreamtest.ui.phone.productview.fragments.routefilmadapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RoutefilmsAdapter extends  RecyclerView.Adapter<RoutefilmsViewHolder> {
    private static final String TAG = RoutefilmsAdapter.class.getSimpleName();

    //Data Model
    private ProductViewModel productViewModel;

    //INFORMATION PARAMETERS
    private Product selectedProduct;
    private LinearLayout routeInformationBlock;

    //ELEMENTS FOR LIST WHICH IS ATTACHED TO THE ADAPTER
    //TODO: Put Lists in MutableLiveData objects for concurrency issues
    private List<Routefilm> routefilmList = new ArrayList<>();
    private List<Flag> flags = new ArrayList<>();
    private List<MovieFlag> movieFlags = new ArrayList<>();

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
        holder.itemView.setSelected(selectedRoutefilm == position);
        if (selectedRoutefilm == position) {
            final ImageButton routeSceneryImage = holder.itemView.findViewById(R.id.routeImageCoverButton);
            routeSceneryImage.setFocusableInTouchMode(true);
            routeSceneryImage.setFocusable(true);
            routeSceneryImage.requestFocus();
        }
        if (routefilmList != null && routefilmList.size() > 0 &&
            flags != null && flags.size()>0 &&
            movieFlags != null && movieFlags.size()>0) {
            final Flag selectedFlag = getFlagOfRoutefilm(routefilmList.get(position));
            holder.bindProduct(routefilmList.get(position), selectedProduct, position, routeInformationBlock, selectedFlag);
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

    public List<Routefilm> getRoutefilmList() {
        return this.routefilmList;
    }

    public int getCurrentSelectedRoutefilmPosition() {return this.selectedRoutefilm;}

    public int getSelectedRoutefilmPosition(final Routefilm routefilm) {
        if (routefilmList != null && routefilmList.size()>0) {
            for (int filmIndex = 0; filmIndex<routefilmList.size();filmIndex++) {
                if (routefilm.getMovieId().intValue()==routefilmList.get(filmIndex).getMovieId().intValue()) {
                    return filmIndex;
                }
            }
        }
        return 0;
    }

    public void updateRoutefilmList(final List<Routefilm> requestedRoutefilmList) {
        if (requestedRoutefilmList != null && requestedRoutefilmList.size()>0) {
            for (final Routefilm routefilm: requestedRoutefilmList) {
                if (!isRoutefilmPresent(routefilm)) {
                    if (selectedProduct.getSupportStreaming()==0 && DownloadHelper.isMoviePresent(productViewModel.getApplication(), Movie.fromRoutefilm(routefilm))) {
                        this.routefilmList.add(routefilm);
                    }
                    if (selectedProduct.getSupportStreaming()==1) {
                        this.routefilmList.add(routefilm);
                    }
                }
            }
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

    public void updateFlagList(final List<Flag> flagList) {
        if (flagList != null && flagList.size()>0) {
            for (final Flag flag: flagList) {
                if (!isFlagPresent(flag)) {
                    this.flags.add(flag);
                    notifyDataSetChanged();
                }
            }
        }
    }

    private boolean isFlagPresent(final Flag requestedFlag) {
        if (this.flags.size()>0) {
            for (final Flag flag: this.flags) {
                if (requestedFlag.getId().intValue() == flag.getId().intValue()) {
                    return requestedFlag.getId().intValue() == flag.getId().intValue();
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public void updateMovieFlagList(final List<MovieFlag> movieFlagList) {
        if (movieFlagList != null && movieFlagList.size()>0) {
            for (final MovieFlag movieFlag: movieFlagList) {
                if (!isMovieFlagPresent(movieFlag)) {
                    this.movieFlags.add(movieFlag);
                    notifyDataSetChanged();
                }
            }
        }
    }

    private boolean isMovieFlagPresent(final MovieFlag requestedMovieFlag) {
        if (this.movieFlags.size()>0) {
            for (final MovieFlag movieFlag: this.movieFlags) {
                if (requestedMovieFlag.getId().intValue() == movieFlag.getId().intValue()) {
                    return requestedMovieFlag.getId().intValue() == movieFlag.getId().intValue();
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private Flag getFlagOfRoutefilm(final Routefilm routefilm) {
        if (routefilm != null && movieFlags!=null && flags != null) {
            //select movieflag based on routefilm id
            for (final MovieFlag movieFlag: movieFlags) {
                if (routefilm.getMovieId().intValue() == movieFlag.getMovieId().intValue()) {
                    //select flag based on movieFlag flagId
                    for (final Flag flag: flags) {
                        if (movieFlag.getFlagId().intValue() == flag.getId().intValue()) {
                            return flag;
                        }
                    }
                }
            }
        }
        return null;
    }

}
