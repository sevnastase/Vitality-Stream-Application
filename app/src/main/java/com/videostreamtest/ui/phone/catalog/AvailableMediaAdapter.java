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
import com.videostreamtest.data.model.Movie;

public class AvailableMediaAdapter extends RecyclerView.Adapter<AvailableMediaViewHolder> {
    final static String TAG = AvailableMediaAdapter.class.getSimpleName();
    private Movie[] movieList;

    private int selectedMovie = 0;

    private ImageView routeInfoImageView;
    private LinearLayout routeInfoTextLayoutBlock;

    public AvailableMediaAdapter(Movie[] movieList) {
        this.movieList = movieList;
    }

    public void setRouteInfoImageView(final ImageView routeInfoImageView) {
        this.routeInfoImageView = routeInfoImageView;
    }

    public void setRouteInfoTextView(final LinearLayout routeInfoTextLayoutBlock) {
        this.routeInfoTextLayoutBlock = routeInfoTextLayoutBlock;
    }

    @NonNull
    @Override
    public AvailableMediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_movie, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new AvailableMediaViewHolder(view);
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
        if (movieList.length > 0) {
            holder.bind(movieList[position], position, routeInfoImageView, routeInfoTextLayoutBlock);
        }
    }

    @Override
    public int getItemCount() {
        return movieList.length;
    }
}
