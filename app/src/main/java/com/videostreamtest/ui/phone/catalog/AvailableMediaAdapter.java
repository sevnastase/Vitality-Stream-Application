package com.videostreamtest.ui.phone.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;

public class AvailableMediaAdapter extends RecyclerView.Adapter<AvailableMediaViewHolder> {
    final static String TAG = AvailableMediaAdapter.class.getSimpleName();
    private Movie[] movieList;
    private Routefilm[] routefilms;
    private boolean localPlay;

    private int selectedMovie = 0;

    //Passing values for routeInfoLayout
    private ImageView routeInfoImageView;
    private LinearLayout routeInfoTextLayoutBlock;

    //Passing values for downloading movie
    private double progress = 0;
    private int movieId = 0;

    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;

    public AvailableMediaAdapter(Movie[] movieList) {
        this.movieList = movieList;
    }

    public AvailableMediaAdapter(Routefilm[] routefilms) {
        this.routefilms = routefilms;
    }

    public AvailableMediaAdapter(Routefilm[] routefilms, boolean isLocalPlay) {
        this.routefilms = routefilms;
        localPlay = isLocalPlay;
    }

    public void setCatalogRecyclerViewClickListener(CatalogRecyclerViewClickListener catalogRecyclerViewClickListener) {
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
    }

    public void setRouteInfoImageView(final ImageView routeInfoImageView) {
        this.routeInfoImageView = routeInfoImageView;
    }

    public void setRouteInfoTextView(final LinearLayout routeInfoTextLayoutBlock) {
        this.routeInfoTextLayoutBlock = routeInfoTextLayoutBlock;
    }

    public void updateDownloadProgress(final double progress, final int movieId) {
        this.progress = progress;
        this.movieId = movieId;
    }

    @NonNull
    @Override
    public AvailableMediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_movie, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new AvailableMediaViewHolder(view, catalogRecyclerViewClickListener, localPlay);
    }

    @Override
    public void onBindViewHolder(@NonNull AvailableMediaViewHolder holder, int position) {
        //Activate Focuslistener on selected movie
        if(selectedMovie == position) {
            ImageButton routeImagecover = holder.itemView.findViewById(R.id.routeImageCoverButton);
            routeImagecover.setFocusableInTouchMode(true);
            routeImagecover.setFocusable(true);
            routeImagecover.requestFocus();
        }
        //Mark itemView as selected of selected movie
        holder.itemView.setSelected(selectedMovie == position);
        if (movieList != null && movieList.length > 0) {
            holder.bind(movieList[position], position, routeInfoImageView, routeInfoTextLayoutBlock, progress, movieId);
        }
        if (routefilms != null && routefilms.length > 0) {
            holder.bind(routefilms[position], position, routeInfoImageView, routeInfoTextLayoutBlock, progress, movieId);
        }
    }

    @Override
    public int getItemCount() {
        if (movieList != null) {
            return movieList.length;
        }
        if (routefilms != null) {
            return routefilms.length;
        }
        return 0;
    }
}
