package com.videostreamtest.ui.phone.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;

public class AvailableMediaAdapter extends RecyclerView.Adapter<AvailableMediaViewHolder> {
    final static String TAG = AvailableMediaAdapter.class.getSimpleName();
    private Movie[] movieList;

    public AvailableMediaAdapter(Movie[] movieList) {
        this.movieList = movieList;
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
        if (movieList.length > 0) {
            holder.bind(movieList[position], position);
        }
    }

    @Override
    public int getItemCount() {
        return movieList.length;
    }
}
