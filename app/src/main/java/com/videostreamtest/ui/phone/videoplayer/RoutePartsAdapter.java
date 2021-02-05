package com.videostreamtest.ui.phone.videoplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.catalog.AvailableMediaViewHolder;

public class RoutePartsAdapter extends RecyclerView.Adapter<RoutePartsViewHolder>{
    final static String TAG = RoutePartsAdapter.class.getSimpleName();
    private MoviePart[] movieParts;

    public RoutePartsAdapter(MoviePart[] movieParts) {
        this.movieParts = movieParts;
    }

    @NonNull
    @Override
    public RoutePartsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_moviepart, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new RoutePartsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutePartsViewHolder holder, int position) {
        if (movieParts.length > 0) {
            holder.bind(movieParts[position], position);
        }
    }

    @Override
    public int getItemCount() {
        return movieParts.length;
    }
}
