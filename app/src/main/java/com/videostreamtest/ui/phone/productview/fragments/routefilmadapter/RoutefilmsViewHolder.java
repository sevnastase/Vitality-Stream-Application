package com.videostreamtest.ui.phone.productview.fragments.routefilmadapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.productview.fragments.RouteDownloadInformationFragment;
import com.videostreamtest.ui.phone.productview.fragments.messagebox.errors.SpeedtestErrorFragment;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class RoutefilmsViewHolder extends RecyclerView.ViewHolder{
    private static final String TAG = RoutefilmsViewHolder.class.getSimpleName();

    //INIT VARIABLES
    private Product selectedProduct;
    private Movie movie;
    private int position = 0;
    private LinearLayout routeInformationBlock;
    private Flag selectedFlag;

    //VIEW ELEMENTS
    private ImageButton routefilmScenery;

    //APP APIKEY
    private String apikey;

    //PREP VIDEOPLAYER
    private Intent videoPlayer;

    public RoutefilmsViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
    }

    public void bindProduct(final Routefilm routefilm,
                            final Product selectedProduct,
                            final int position,
                            final LinearLayout routeinformationBlock,
                            final Flag selectedFlag) {
        this.movie = Movie.fromRoutefilm(routefilm);
        this.selectedProduct = selectedProduct;
        this.position = position;
        this.routeInformationBlock = routeinformationBlock;
        this.selectedFlag = selectedFlag;

        //Get APIKEY for write cloud log
        SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        apikey = sharedPreferences.getString("apikey", "");

        //LOAD VIEW ELEMENT
        routefilmScenery = itemView.findViewById(R.id.routeImageCoverButton);

        initMovie(selectedProduct);
        if (performStaticChecks(selectedProduct)) {
            //READY TO PLAY CLAUSE
            initVideoPlayer(generateBundleParameters());
            initStartListeners(selectedProduct);
        } else {
            //READY TO DOWNLOAD CLAUSE
            initDownloadListeners();
        }

        initMovieImages(selectedProduct);
        initOnFocusChangeListener(selectedProduct);
        initView();
        itemView.invalidate();
    }

    private void initView() {
        initBorders();

        if(itemView.isSelected()) {
            updateRouteInformationBlock();
        }
    }

    private void updateRouteInformationBlock() {
        if (movie == null || routeInformationBlock == null) {
            return;
        }
        //Link views
        TextView titleView = routeInformationBlock.findViewById(R.id.selected_route_title);
        TextView distanceView = routeInformationBlock.findViewById(R.id.selected_route_distance);
        ImageView routeInformationMap = routeInformationBlock.findViewById(R.id.selected_route_map);
        ImageView routeFlag = routeInformationBlock.findViewById(R.id.selected_route_flag);

        //Set flag
        if (selectedFlag != null) {
            routeFlag.setVisibility(View.VISIBLE);
            if (DownloadHelper.isFlagsLocalPresent(itemView.getContext().getApplicationContext())) {
                Picasso.get()
                        .load(DownloadHelper.getLocalFlag(itemView.getContext().getApplicationContext(), selectedFlag))
                        .placeholder(R.drawable.flag_placeholder)
                        .error(R.drawable.flag_placeholder)
                        .resize(150, 100)
                        .into(routeFlag);
            } else {
                Picasso.get()
                        .load(selectedFlag.getFlagUrl())
                        .placeholder(R.drawable.flag_placeholder)
                        .error(R.drawable.flag_placeholder)
                        .resize(150, 100)
                        .into(routeFlag);
            }
        } else {
            routeFlag.setVisibility(View.INVISIBLE);
        }

        //Set Title
        titleView.setText(toString().format(itemView.getContext().getString(R.string.catalog_selected_route_title), movie.getMovieTitle()));
        titleView.setVisibility(View.VISIBLE);

        //Set Distance/Duration of movie
        if (selectedProduct.getProductName().toLowerCase().contains("praxfilm")) {
            distanceView.setText(String.format("Duration: %d minutes", ((movie.getMovieLength()/movie.getRecordedFps())/60)));
        } else {
            float meters = movie.getMovieLength();
            int km = (int) (meters / 1000f);
            int hectometers = (int) ((meters - (km * 1000f)) / 100f);
            distanceView.setText(toString().format(itemView.getContext().getString(R.string.catalog_screen_distance), km, hectometers));
        }

        //Set route map
        if (selectedProduct.getSupportStreaming()==0 && performStaticChecks(selectedProduct)) {
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

    private void initOnFocusChangeListener(final Product selectedProduct) {
        routefilmScenery.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "hasFocus routefilmscenery");
                itemView.setSelected(true);
                if (hasFocus) {
                    if (selectedProduct.getSupportStreaming()==0) {
                        PraxtourDatabase.databaseWriterExecutor.execute(()->{
                            PraxtourDatabase.getDatabase(itemView.getContext()).usageTrackerDao().setSelectedMovie(apikey, movie.getId());
                        });
                    }
                    drawSelectionBorder();
                    updateRouteInformationBlock();
                } else {
                    undrawSelectionBorder();
                }
            }
        });
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

    private void initMovieImages(final Product selectedProduct) {
        //Set product image in button
        if (selectedProduct.getSupportStreaming()==0) {
            Picasso.get()
                    .load(new File(movie.getMovieImagepath()))
                    .resize(180, 242)
                    .placeholder(R.drawable.download_from_cloud_scenery)
                    .error(R.drawable.download_from_cloud_scenery)
                    .into(routefilmScenery);
        } else {
            Picasso.get()
                    .load(movie.getMovieImagepath())
                    .resize(180, 242)
                    .placeholder(R.drawable.download_from_cloud_scenery)
                    .error(R.drawable.download_from_cloud_scenery)
                    .into(routefilmScenery);
        }
    }

    private void initMovie(final Product selectedProduct) {
        if (selectedProduct.getSupportStreaming()==0) {
            DownloadHelper.setLocalMedia(itemView.getContext(), movie);
        }
    }

    private void initDownloadListeners() {
        routefilmScenery.setOnClickListener((onClickedView) -> {
            AppCompatActivity activity = (AppCompatActivity) onClickedView.getContext();
            NavHostFragment navHostFragment =
                    (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.downloadsFragment);
//            activity.getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container_view, RouteDownloadInformationFragment.class, generateBundleParameters())
//                    .addToBackStack("routeinfo")
//                    .commit();
        });
    }

    private void initStartListeners(final Product selectedProduct) {
        routefilmScenery.setOnClickListener((onClickedView) -> {
            updateRouteInformationBlock();
            PraxtourDatabase.databaseWriterExecutor.execute(()->{
                PraxtourDatabase.getDatabase(itemView.getContext()).usageTrackerDao().setSelectedMovie(apikey, movie.getId());
            });
            if (selectedProduct.getSupportStreaming()==0) {
                itemView.getContext().startActivity(videoPlayer);
            } else {
                performSpeedtestAndStartVideoPlayer();
            }
        });
    }

    private void initVideoPlayer(final Bundle arguments) {
        videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
        videoPlayer.putExtras(arguments);
    }

    private Bundle generateBundleParameters() {
        Bundle arguments = new Bundle();
        arguments.putString("movieObject", new GsonBuilder().create().toJson(movie, Movie.class));
        arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
        arguments.putString("communication_device", selectedProduct.getCommunicationType());
        arguments.putString("accountToken", apikey);
        arguments.putBoolean("localPlay", selectedProduct.getSupportStreaming()==0);
        return arguments;
    }

    /**
     * Perform synchronous checks
     * @param selectedProduct
     */
    private boolean performStaticChecks(final Product selectedProduct) {
        // StandAlone
        if (selectedProduct.getSupportStreaming()==0) {
            boolean isMovieOnDevice = DownloadHelper.isMoviePresent(itemView.getContext(), movie);
            boolean isSoundOnDevice = DownloadHelper.isSoundPresent(itemView.getContext());
            boolean isMovieSupportImagesOnDevice = DownloadHelper.isMovieImagesPresent(itemView.getContext(), movie);
            return (isMovieOnDevice&&isSoundOnDevice&&isMovieSupportImagesOnDevice);
        }
        // Streaming
        else {
            return true;
        }
    }

    private void performSpeedtestAndStartVideoPlayer() {
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
                            thread.getLooper().prepare();
                            LogHelper.WriteLogRule(itemView.getContext(), apikey, "[SPEED ERROR] Internet Speed to low at starting movie: "+movie.getMovieTitle(), "ERROR", "");

                            Bundle arguments = new Bundle();
                            arguments.putInt("needed_speed", ApplicationSettings.SPEEDTEST_MINIMUM_SPEED.intValue());
                            arguments.putInt("measured_speed", report.getTransferRateBit().intValue());
                            arguments.putString("communication_device", selectedProduct.getCommunicationType());
                            arguments.putString("product_object", new GsonBuilder().create().toJson(selectedProduct, Product.class));

                            AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                            NavHostFragment navHostFragment =
                                    (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
                            NavController navController = navHostFragment.getNavController();
                            navController.navigate(R.id.errorFragment);
                        }
                    }

                    @Override
                    public void onError(SpeedTestError speedTestError, String errorMessage) {
                        // called when a download/upload error occur
                        Log.d(getClass().getSimpleName(), "ERROR :: "+errorMessage);
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
//                        activity.getSupportFragmentManager()
//                                .beginTransaction()
//                                .add(R.id.fragment_container_view, SpeedtestErrorFragment.class, null)
//                                .disallowAddToBackStack()
//                                .commit();
                        NavHostFragment navHostFragment =
                                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
                        NavController navController = navHostFragment.getNavController();
                        navController.navigate(R.id.errorFragment);
                    }

                    @Override
                    public void onProgress(float percent, SpeedTestReport report) {
                        // called to notify download/upload progress
                        Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                        Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                        Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
                    }
                });

                speedTestSocket.startDownload("https://media.praxcloud.eu/1M.iso");
            }
        };
        speedtestHandler.postDelayed(runnableSpeedTest,0);
    }

}
