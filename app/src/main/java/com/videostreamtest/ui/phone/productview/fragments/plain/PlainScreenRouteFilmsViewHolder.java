package com.videostreamtest.ui.phone.productview.fragments.plain;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.productview.fragments.RouteInformationFragment;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

public class PlainScreenRouteFilmsViewHolder extends RecyclerView.ViewHolder{
    private static final String TAG = PlainScreenRouteFilmsViewHolder.class.getSimpleName();

    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;
    private boolean localPlay;
    private Movie movie;
    private Routefilm routefilm;
    private int position =0;
    private LinearLayout routeInformationBlock;
    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    private boolean isMovieOnDevice = false;

    private ImageButton routefilmScenery;

    public PlainScreenRouteFilmsViewHolder(@NonNull View itemView) {
        super(itemView);
        routefilmScenery = itemView.findViewById(R.id.routeImageCoverButton);
    }

    public void bindStreamingMode(Routefilm routefilm, int position, Product selectedProduct, CommunicationDevice communicationDevice, LinearLayout routeInformationBlock, CatalogRecyclerViewClickListener catalogRecyclerViewClickListener){
        this.routefilm = routefilm;
        this.movie = Movie.fromRoutefilm(routefilm);
        this.position = position;
        this.selectedProduct = selectedProduct;
        this.communicationDevice = communicationDevice;
        this.routeInformationBlock = routeInformationBlock;
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
        localPlay = false;

        //PreSet Cover for size
        Picasso.get()
                .load(movie.getMovieImagepath())
                .resize(180, 242)
                .placeholder(R.drawable.cast_album_art_placeholder)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(routefilmScenery);

        initView();
        initOnClickListener(routefilm);
    }

    public void bindStandaloneMode(Routefilm routefilm, int position, Product selectedProduct, CommunicationDevice communicationDevice, LinearLayout routeInformationBlock, CatalogRecyclerViewClickListener catalogRecyclerViewClickListener){
        this.routefilm = routefilm;
        this.movie = Movie.fromRoutefilm(routefilm);
        this.position = position;
        this.selectedProduct = selectedProduct;
        this.communicationDevice = communicationDevice;
        this.routeInformationBlock = routeInformationBlock;
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
        localPlay = true;
        isMovieOnDevice = DownloadHelper.isMoviePresent(itemView.getContext(), movie);

        if (isMovieOnDevice) {
            //Set local movie paths
            DownloadHelper.setLocalMedia(itemView.getContext(), this.movie);
        }

        SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("app", MODE_PRIVATE);
        String apikey = sharedPreferences.getString("apikey", "");
        LogHelper.WriteLogRule(itemView.getContext(), apikey, "[DEBUG] "+movie.getMovieTitle()+": isMovieOndevice: "+isMovieOnDevice, "DEBUG", "");

        //PreSet Cover for size
        if (localPlay && isMovieOnDevice) {
            Picasso.get()
                    .load(new File(movie.getMovieImagepath()))
                    .resize(180, 242)
                    .placeholder(R.drawable.cast_album_art_placeholder)
                    .error(R.drawable.cast_ic_notification_disconnect)
                    .into(routefilmScenery);
        } else {
            Picasso.get()
                    .load(movie.getMovieImagepath())
                    .resize(180, 242)
                    .placeholder(R.drawable.cast_album_art_placeholder)
                    .error(R.drawable.cast_ic_notification_disconnect)
                    .into(routefilmScenery);
        }

        initView();
        initOnClickListener(routefilm);
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

    private void initOnClickListener(final Routefilm routefilm) {
        //Set onclick on imagebutton
        routefilmScenery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                routefilmScenery.requestFocus();
                loadNextStep();
            }
        });
    }

    private void loadNextStep() {
        if (localPlay) {
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
                AppCompatActivity activity = (AppCompatActivity) itemView.getContext();

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
        } else {
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

        if (selectedProduct.getProductName().toLowerCase().contains("praxfilm")) {
            distanceView.setText(String.format("Duration: %d minutes", ((movie.getMovieLength()/movie.getRecordedFps())/60)));
        } else {
            float meters = movie.getMovieLength();
            int km = (int) (meters / 1000f);
            int hectometers = (int) ((meters - (km * 1000f)) / 100f);
            distanceView.setText(toString().format(itemView.getContext().getString(R.string.catalog_screen_distance), km, hectometers));
        }

        //Set Route Information map
        if (localPlay && isMovieOnDevice) {
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
