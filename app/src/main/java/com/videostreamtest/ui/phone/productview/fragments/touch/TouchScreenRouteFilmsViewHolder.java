package com.videostreamtest.ui.phone.productview.fragments.touch;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

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
import com.videostreamtest.utils.ApplicationSettings;

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
        if (selectedProduct.getProductName().toLowerCase().contains("praxfilm")) {
            isMovieSupportImagesOnDevice = true;
        } else {
            isMovieSupportImagesOnDevice = DownloadHelper.isMovieImagesPresent(itemView.getContext(), movie.getId());
        }

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
                .resize((int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 120, itemView.getContext()
                                .getResources().getDisplayMetrics()), (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 164, itemView.getContext()
                                .getResources().getDisplayMetrics()))
//                .resize(180, 242)
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

                //Perform speed check

                HandlerThread thread = new HandlerThread("Speedtest",
                        Process.THREAD_PRIORITY_MORE_FAVORABLE);
                thread.start();

                Handler speedtestHandler = new Handler(thread.getLooper());
                Runnable runnableSpeedTest = new Runnable() {
                    @Override
                    public void run() {
                        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

                        // add a listener to wait for speedtest completion and progress
                        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                            @Override
                            public void onCompletion(final SpeedTestReport report) {
                                // called when download/upload is finished
                                Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                                Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                                if (report.getTransferRateBit().intValue() > ApplicationSettings.SPEEDTEST_MINIMUM_SPEED.intValue()) {
                                    itemView.getContext().startActivity(videoPlayer);
                                } else {
                                    LogHelper.WriteLogRule(itemView.getContext(), routefilm.getAccountToken(), "Internet Speed to low at starting movie: "+movie.getMovieTitle(), "ERROR", "");
                                    Toast.makeText(itemView.getContext(), "Internet speed not sufficient enough" , Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onError(SpeedTestError speedTestError, String errorMessage) {
                                // called when a download/upload error occur
                                Log.d(TouchScreenRouteFilmsViewHolder.class.getSimpleName(), "ERROR :: "+errorMessage);
                                Toast.makeText(itemView.getContext(), "An error occured! "+errorMessage , Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onProgress(float percent, SpeedTestReport report) {
                                // called to notify download/upload progress
                                Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                                Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                                Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
                            }
                        });

                        speedTestSocket.startDownload("http://praxmedia.praxtour.com/1M.iso");
                    }
                };
                speedtestHandler.postDelayed(runnableSpeedTest,0);

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

        if (selectedProduct.getProductName().toLowerCase().contains("praxfilm")) {
            distanceView.setText(String.format("Duration: %d minutes", ((movie.getMovieLength()/movie.getRecordedFps())/60)));
        } else {
            float meters = movie.getMovieLength();
            int km = (int) (meters / 1000f);
            int hectometers = (int) ((meters - (km * 1000f)) / 100f);
            distanceView.setText(toString().format(itemView.getContext().getString(R.string.catalog_screen_distance), km, hectometers));
        }

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
