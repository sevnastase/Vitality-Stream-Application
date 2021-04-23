package com.videostreamtest.ui.phone.videoplayer.fragments.routeparts;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.data.model.MoviePart;

import java.util.List;

public class RoutePartsAdapter extends RecyclerView.Adapter<RoutePartsViewHolder>{
    final static String TAG = RoutePartsAdapter.class.getSimpleName();
    private MoviePart[] movieParts;

    private int selectedMoviePart = 0;

    public RoutePartsAdapter(MoviePart[] movieParts) {
        this.movieParts = movieParts;
    }

    public RoutePartsAdapter(List<Routepart> routeparts) {
        if (routeparts.size()>0) {
            movieParts = new MoviePart[routeparts.size()];
            for (int partIndex = 0; partIndex<routeparts.size();partIndex++) {
                movieParts[partIndex] = MoviePart.fromRoutepartEntity(routeparts.get(partIndex));
            }
        }
    }

    public void setSelectedMoviePart(final int frameNumber) {
        int position = 0;
        //movieparts array moet er zijn en gevuld
        if (movieParts != null && movieParts.length > 0) {
            //Loop door de parts heen
            for (int moviePartIndex = 0;moviePartIndex < movieParts.length; moviePartIndex++) {
                //check of het framenummer in deze serie past
                if (frameNumber >= movieParts[moviePartIndex].getFrameNumber()) {
                    position = moviePartIndex;
                }
            }
        }
        this.selectedMoviePart = position;
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
        Log.d(TAG, "Binding routeparts");
        if (selectedMoviePart == position) {
            ImageButton routePartCover = holder.itemView.findViewById(R.id.routepart_cover_button);
            routePartCover.setFocusableInTouchMode(true);
            routePartCover.setFocusable(true);
            routePartCover.requestFocus();
        }
        holder.itemView.setSelected(selectedMoviePart==position);
        if (movieParts.length > 0) {
            holder.bind(movieParts[position], position);
        }
    }

    @Override
    public int getItemCount() {
        return movieParts.length;
    }
}
