package com.videostreamtest.ui.phone.productview.fragments.touch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.productview.fragments.RouteInformationFragment;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

import java.io.File;

public class TouchScreenRouteFilmsViewHolder extends RecyclerView.ViewHolder {

    private ImageButton routefilmScenery;
    private boolean isMovieOnDevice = false;
    private boolean isSoundOnDevice = false;

    public TouchScreenRouteFilmsViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bindStandalone(Routefilm routefilm, Product selectedProduct, CommunicationDevice communicationDevice, int position) {
        routefilmScenery = itemView.findViewById(R.id.routefilm_cover_button);
        final Movie movie = Movie.fromRoutefilm(routefilm);
        isMovieOnDevice = DownloadHelper.isMoviePresent(itemView.getContext(), movie);
        isSoundOnDevice = DownloadHelper.isSoundPresent(itemView.getContext());

        Log.d(this.getClass().getSimpleName(), "Movie "+movie.getMovieTitle()+" is on device > "+isMovieOnDevice);

        if (isMovieOnDevice) {
            DownloadHelper.setLocalMedia(itemView.getContext(), movie);
            Log.d(this.getClass().getSimpleName(), "LOCAL :: " + movie.getMovieImagepath());

            Picasso.get()
                    .load(new File(movie.getMovieImagepath()))
                    .resize(180, 242)
                    .placeholder(R.drawable.download_from_cloud_scenery)
                    .error(R.drawable.download_from_cloud_scenery)
                    .into(routefilmScenery);
        } else {
            Log.d(this.getClass().getSimpleName(), "PRAXCLOUD :: " + routefilm.getMovieImagepath());
            //Set product image in button
            Picasso.get()
                    .load(routefilm.getMovieImagepath())
                    .resize(180, 242)
                    .placeholder(R.drawable.download_from_cloud_scenery)
                    .error(R.drawable.download_from_cloud_scenery)
                    .into(routefilmScenery);
        }

            routefilmScenery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isMovieOnDevice) {
                        Bundle arguments = new Bundle();
                        arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                        arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
                        arguments.putString("communication_device", communicationDevice.name());
                        arguments.putString("accountToken", routefilm.getAccountToken());

                        final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                        videoPlayer.putExtras(arguments);

                        itemView.getContext().startActivity(videoPlayer);
                    } else {
                        AppCompatActivity activity = (AppCompatActivity) v.getContext();

                        Bundle arguments = new Bundle();
                        arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                        arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
                        arguments.putString("communication_device", communicationDevice.name());

                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container_view, RouteInformationFragment.class, arguments)
                                .addToBackStack("routeinfo")
                                .commit();
                    }
                }
            });
    }

    public void bindStreaming(Routefilm routefilm, Product selectedProduct, CommunicationDevice communicationDevice, int position) {
        routefilmScenery = itemView.findViewById(R.id.routefilm_cover_button);
        final Movie movie = Movie.fromRoutefilm(routefilm);

        //Set product image in button
        Picasso.get()
                .load(movie.getMovieImagepath())
                .resize(180, 242)
                .placeholder(R.drawable.download_from_cloud_scenery)
                .error(R.drawable.download_from_cloud_scenery)
                .into(routefilmScenery);

        routefilmScenery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle arguments = new Bundle();
                arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
                arguments.putString("communication_device", communicationDevice.name());
                arguments.putString("accountToken", routefilm.getAccountToken());

                final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                videoPlayer.putExtras(arguments);

                itemView.getContext().startActivity(videoPlayer);
            }
        });
    }
}
