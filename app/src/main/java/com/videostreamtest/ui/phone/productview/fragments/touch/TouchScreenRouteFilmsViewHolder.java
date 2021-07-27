package com.videostreamtest.ui.phone.productview.fragments.touch;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.productview.fragments.RouteInformationFragment;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

import java.io.File;

public class TouchScreenRouteFilmsViewHolder extends RecyclerView.ViewHolder {
    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;
    private LinearLayout routeInformationBlock;
    private Product selectedProduct;
    private Movie movie;
    private int position =0;
    private boolean localPlay;

    private ImageButton routefilmScenery;
    private boolean isMovieOnDevice = false;
    private boolean isSoundOnDevice = false;
    private boolean isMovieSupportImagesOnDevice = false;

    public TouchScreenRouteFilmsViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bindStandalone(Routefilm routefilm, Product selectedProduct, CommunicationDevice communicationDevice, int position, LinearLayout routeInformationBlock, CatalogRecyclerViewClickListener catalogRecyclerViewClickListener) {
        this.movie = Movie.fromRoutefilm(routefilm);
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
        this.selectedProduct = selectedProduct;
        this.position = position;
        this.routeInformationBlock = routeInformationBlock;
        this.localPlay = selectedProduct.getSupportStreaming()==0;

        routefilmScenery = itemView.findViewById(R.id.routeImageCoverButton);

        isMovieOnDevice = DownloadHelper.isMoviePresent(itemView.getContext(), movie);
        isSoundOnDevice = DownloadHelper.isSoundPresent(itemView.getContext());
        isMovieSupportImagesOnDevice = DownloadHelper.isMovieImagesPresent(itemView.getContext(), movie.getId());

        if (isMovieSupportImagesOnDevice) {
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

        initView();

        routefilmScenery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMovieOnDevice) {
                    Bundle arguments = new Bundle();
                    arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                    arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
                    arguments.putString("communication_device", communicationDevice.name());
                    arguments.putString("accountToken", routefilm.getAccountToken());
                    arguments.putBoolean("localPlay", localPlay);

                    final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                    videoPlayer.putExtras(arguments);

                    itemView.getContext().startActivity(videoPlayer);
                } else {
                    AppCompatActivity activity = (AppCompatActivity) v.getContext();

                    Bundle arguments = new Bundle();
                    arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                    arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
                    arguments.putString("communication_device", communicationDevice.name());
                    arguments.putBoolean("localPlay", localPlay);

                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container_view, RouteInformationFragment.class, arguments)
                            .addToBackStack("routeinfo")
                            .commit();
                }
            }
        });
    }

    public void bindStreaming(Routefilm routefilm, Product selectedProduct, CommunicationDevice communicationDevice, int position, LinearLayout routeInformationBlock, CatalogRecyclerViewClickListener catalogRecyclerViewClickListener) {
        this.movie = Movie.fromRoutefilm(routefilm);
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
        this.selectedProduct = selectedProduct;
        this.position = position;
        this.routeInformationBlock = routeInformationBlock;
        this.localPlay = selectedProduct.getSupportStreaming()==0;

        routefilmScenery = itemView.findViewById(R.id.routeImageCoverButton);

        //Set product image in button
        Picasso.get()
                .load(movie.getMovieImagepath())
                .resize(180, 242)
                .placeholder(R.drawable.download_from_cloud_scenery)
                .error(R.drawable.download_from_cloud_scenery)
                .into(routefilmScenery);

        initView();

        //TEST CASES MPEG DASH ADAPTIVE STREAMING
        if (routefilm.getMovieTitle().toLowerCase().contains("amsterdam 1")) {
            routefilm.setMovieUrl("http://178.62.194.237/movies/Amsterdam_1/mpd/amsterdam_1.mpd");
        }

        if (routefilm.getMovieTitle().toLowerCase().contains("amersfoort")) {
            routefilm.setMovieUrl("http://178.62.194.237/movies/Amersfoort/mpd/amersfoort.mpd");
        }

        if (routefilm.getMovieTitle().toLowerCase().contains("groningen")) {
            routefilm.setMovieUrl("http://178.62.194.237/movies/Groningen/mpd/groningen.mpd");
        }

        routefilmScenery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle arguments = new Bundle();
                arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
                arguments.putString("communication_device", communicationDevice.name());
                arguments.putString("accountToken", routefilm.getAccountToken());
                arguments.putBoolean("localPlay", localPlay);

                final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                videoPlayer.putExtras(arguments);

                itemView.getContext().startActivity(videoPlayer);
            }
        });
    }

    private void initView()
    {
        initBorders();
        initOnFocusChangeListener();
        if(itemView.isSelected()) {
            updateRouteInformationBlock();
        }
    }

    private void initBorders() {
        drawSelectionBorder();
        undrawSelectionBorder();

        if (routefilmScenery.isSelected() ) {
            drawSelectionBorder();
        } else {
            undrawSelectionBorder();
        }
    }

    private void drawSelectionBorder() {
        final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_blue_border);
        routefilmScenery.setBackground(border);
        routefilmScenery.setAlpha(1.0f);
    }

    private void undrawSelectionBorder() {
        routefilmScenery.setBackground(null);
        routefilmScenery.setAlpha(0.7f);
    }

    private void initOnFocusChangeListener() {
        routefilmScenery.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                itemView.setSelected(true);
                if (hasFocus) {
                    drawSelectionBorder();
                    updateRouteInformationBlock();
                    if(catalogRecyclerViewClickListener != null) {
                        catalogRecyclerViewClickListener.recyclerViewListClicked(itemView, position);
                    }
                } else {
                    undrawSelectionBorder();
                }
            }
        });
    }

    private void updateRouteInformationBlock() {
        if(movie == null || routeInformationBlock == null) {
            return;
        }

        TextView titleView = routeInformationBlock.findViewById(R.id.selected_route_title);
        TextView distanceView = routeInformationBlock.findViewById(R.id.selected_route_distance);
        ImageView routeInformationMap = routeInformationBlock.findViewById(R.id.selected_route_infomap_two);

        titleView.setText(toString().format(itemView.getContext().getString(R.string.catalog_selected_route_title), movie.getMovieTitle()));
        titleView.setVisibility(View.VISIBLE);

        float meters = movie.getMovieLength();
        int km = (int) (meters / 1000f);
        int hectometers = (int) ((meters - (km * 1000f)) / 100f);
        distanceView.setText(toString().format(itemView.getContext().getString(R.string.catalog_screen_distance), km, hectometers));

        //Set Route Information map
        if (localPlay && isMovieSupportImagesOnDevice) {
            Picasso.get()
                    .load(new File(movie.getMovieRouteinfoPath()))
                    .fit()
                    .placeholder(R.drawable.placeholder_map)
                    .error(R.drawable.placeholder_map)
                    .into(routeInformationMap);
        } else {
            Picasso.get()
                    .load(movie.getMovieRouteinfoPath())
                    .fit()
                    .placeholder(R.drawable.placeholder_map)
                    .error(R.drawable.placeholder_map)
                    .into(routeInformationMap);
        }
    }
}
