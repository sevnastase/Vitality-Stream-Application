package com.videostreamtest.ui.phone.routefilmpicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.constants.SharedPreferencesConstants;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RoutefilmAdapter extends RecyclerView.Adapter<RoutefilmViewHolder> {
    private final static String TAG = RoutefilmAdapter.class.getSimpleName();

    // Dataset in the RecyclerView
    private ArrayList<Routefilm> routefilms;

    private int selectedRoutefilmPosition;
    private Product selectedProduct;

    public interface SelectedRoutefilmListener {
        void onSelected(Routefilm routefilm);
    }

    private final SelectedRoutefilmListener listener;

    //PREP VIDEOPLAYER
    private Intent videoPlayerActivityIntent;
    private Activity hostActivity;
    private boolean loading = false;

    public RoutefilmAdapter(Routefilm[] routefilms,
                            Product selectedProduct,
                            SelectedRoutefilmListener listener,
                            Activity hostActivity) {
        this.routefilms = new ArrayList<>(Arrays.asList(routefilms));
        this.selectedProduct = selectedProduct;
        this.listener = listener;
        this.hostActivity = hostActivity;

        initFavorites();
        setSelectedRoutefilmPosition(RoutefilmPickerActivity.DEFAULT_SELECTED_POSITION);
    }

    @NonNull
    @Override
    public RoutefilmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_routefilm, parent, false);

        return new RoutefilmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutefilmViewHolder holder, int position) {
        Log.d(TAG, String.format("Position %d has movie %s", position, routefilms.get(position).getMovieTitle()));

        Callback favoriteButtonCallback = new Callback() {
            @Override
            public void onSuccess() {
                holder.favoriteImageButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                holder.favoriteImageButton.setVisibility(View.GONE);
            }
        };

        // Set product image in button
        Movie movie = Movie.fromRoutefilm(routefilms.get(position));
        if(movie.getMovieImagepath().startsWith("/")) {
            Picasso.get()
                    .load(new File(movie.getMovieImagepath()))
                    .resize(180, 242)
                    .into(holder.routefilmCoverPhotoImageButton, favoriteButtonCallback);
        } else {
            Picasso.get()
                    .load(movie.getMovieImagepath())
                    .resize(180, 242)
                    .into(holder.routefilmCoverPhotoImageButton, favoriteButtonCallback);
        }

        holder.itemView.setSelected(position == selectedRoutefilmPosition);
        holder.setSelected(position == selectedRoutefilmPosition);

        holder.routefilmCoverPhotoImageButton.setOnClickListener(view -> {
            // Re-initialize the video player intent
            if (selectedRoutefilmPosition == position) {
                startVideoPlayer(view);
            } else {
                final int prevPosition = selectedRoutefilmPosition;
                setSelectedRoutefilmPosition(position);
                notifyItemChanged(prevPosition);
                notifyItemChanged(position);
            }
        });

        holder.favoriteImageButton.setOnClickListener(view -> {
            SharedPreferences sp = hostActivity.getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            Set<String> favoritedMovieIDs = sp.getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);
            // Avoid null-set
            if (favoritedMovieIDs == null) favoritedMovieIDs = new HashSet<>();

            Set<String> updatedFavoritedMovieIds = new HashSet<>(favoritedMovieIDs);
            if (routefilms.get(position).isFavorite()) {
                holder.favoriteImageButton.setImageResource(R.drawable.empty_star_favorite);
                updatedFavoritedMovieIds.remove(routefilms.get(position).getMovieId().toString());
            } else {
                holder.favoriteImageButton.setImageResource(R.drawable.filled_star_favorite_2);
                updatedFavoritedMovieIds.add(routefilms.get(position).getMovieId().toString());
            }
            editor.putStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, updatedFavoritedMovieIds);
            editor.apply();
            routefilms.get(position).setFavorite(!routefilms.get(position).isFavorite());
            sortByFavorites();
        });

        if (routefilms.get(position).isFavorite()) {
            holder.favoriteImageButton.setImageResource(R.drawable.filled_star_favorite_2);
        } else {
            holder.favoriteImageButton.setImageResource(R.drawable.empty_star_favorite);
        }
    }

    @Override
    public int getItemCount() {
        return routefilms == null ? 0 : routefilms.size();
    }

    public int getSelectedRoutefilmPosition() {
        return selectedRoutefilmPosition;
    }

    public void setSelectedRoutefilmPosition(int selectedRoutefilmPosition) {
        if (routefilms.size() - 1 < selectedRoutefilmPosition) {
            return;
        }
        this.selectedRoutefilmPosition = selectedRoutefilmPosition;
        if (listener != null) {
            listener.onSelected(routefilms.get(selectedRoutefilmPosition));
        }
        initVideoPlayer(generateBundleParameters());
    }

    public void sortByFavorites() {
        Collections.sort(routefilms, (a, b) -> Boolean.compare(b.isFavorite(), a.isFavorite()));
        notifyDataSetChanged(); // we have reordered the entire dataset
    }

    private void initVideoPlayer(final Bundle arguments) {
        if (selectedProduct.getSupportStreaming() == 0) {
            videoPlayerActivityIntent = new Intent(hostActivity, VideoplayerActivity.class);
        } else {
            videoPlayerActivityIntent = new Intent(hostActivity, VideoplayerExoActivity.class);
        }

        videoPlayerActivityIntent.putExtras(arguments);
    }

    private void startVideoPlayer(View view) {
        view.getContext().startActivity(videoPlayerActivityIntent);
    }

    private Bundle generateBundleParameters() {
        Bundle arguments = new Bundle();
        try {
            arguments.putString("movieObject", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilms.get(selectedRoutefilmPosition)), Movie.class));
            arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
            arguments.putString("communication_device", selectedProduct.getCommunicationType());
            arguments.putString("accountToken", AccountHelper.getAccountToken(PraxtourApplication.getAppContext()));
            arguments.putBoolean("localPlay", selectedProduct.getSupportStreaming() == 0);
        } catch (NullPointerException ignored) {}
        return arguments;
    }

    private void initFavorites() {
        Set<String> favoritedMovieIds = hostActivity.getApplicationContext()
                .getSharedPreferences("app", Context.MODE_PRIVATE)
                .getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);

        if (favoritedMovieIds == null) return;

        for (Routefilm routefilm : routefilms) {
            if (favoritedMovieIds.contains(routefilm.getMovieId().toString())) {
                routefilm.setFavorite(true);
            }
        }
    }
}
