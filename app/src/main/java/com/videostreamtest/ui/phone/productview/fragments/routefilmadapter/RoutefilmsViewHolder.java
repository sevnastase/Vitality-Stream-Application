package com.videostreamtest.ui.phone.productview.fragments.routefilmadapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.constants.SharedPreferencesConstants;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoutefilmsViewHolder extends RecyclerView.ViewHolder{
    private static final String TAG = RoutefilmsViewHolder.class.getSimpleName();

    //INIT VARIABLES
    private Product selectedProduct;
    private Movie movie;
    private int position = 0;
    private LinearLayout routeInformationBlock;
    private RoutefilmsAdapter routefilmsAdapter;
    private boolean favorited;

    //VIEW ELEMENTS
    private ImageButton routefilmScenery;
    private ImageButton favoriteButton;
    private ImageView favoritedStar;
    private ImageView unfavoritedStar;

    //APP APIKEY
    private String apikey;

    //PREP VIDEOPLAYER
    private Intent videoPlayer;
    private Intent videoPlayerExo;

    public RoutefilmsViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
    }

    public void bindProduct(final Routefilm routefilm,
                            final Product selectedProduct,
                            final int position,
                            final LinearLayout routeinformationBlock,
                            final RoutefilmsAdapter routefilmsAdapter) {
        this.movie = Movie.fromRoutefilm(routefilm);
        this.selectedProduct = selectedProduct;
        this.position = position;
        this.routeInformationBlock = routeinformationBlock;
        this.routefilmsAdapter = routefilmsAdapter;

        //Get APIKEY for write cloud log
        apikey = AccountHelper.getAccountToken(itemView.getContext());

        //LOAD VIEW ELEMENT
        routefilmScenery = itemView.findViewById(R.id.routeImageCoverButton);
        favoriteButton = itemView.findViewById(R.id.favorite_button);
        favoritedStar = itemView.findViewById(R.id.favorite_button_filled);
        unfavoritedStar = itemView.findViewById(R.id.favorite_button_empty);

        initMovie(selectedProduct);
        if (performStaticChecks(selectedProduct)) {
            //READY TO PLAY CLAUSE
            initVideoPlayer(generateBundleParameters());
            initStreamVideoPlayer(generateBundleParameters());
            initStartListeners(selectedProduct);
        } else {
            //READY TO DOWNLOAD CLAUSE
            initDownloadListeners();
        }

        initMovieImages();
        initOnFocusChangeListener(selectedProduct);
        initView();
        initFavorite();
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
        if (movie.getMovieFlagUrl() != null && !movie.getMovieFlagUrl().isEmpty()) {
            routeFlag.setVisibility(View.VISIBLE);
            if (DownloadHelper.isFlagsLocalPresent(itemView.getContext().getApplicationContext())) {
                Picasso.get()
                        .load(new File(movie.getMovieFlagUrl()))
                        .placeholder(R.drawable.flag_placeholder)
                        .error(R.drawable.flag_placeholder)
                        .resize(150, 100)
                        .into(routeFlag);
            } else {
                Picasso.get()
                        .load(movie.getMovieFlagUrl())
                        .placeholder(R.drawable.flag_placeholder)
                        .error(R.drawable.flag_placeholder)
                        .resize(150, 100)
                        .into(routeFlag);
            }
        } else {
            routeFlag.setVisibility(View.INVISIBLE);
        }

        //Set Title
        Log.d(TAG, movie.getMovieTitle());
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(movie.getMovieTitle());

        //Set Distance/Duration of movie
        if (!isProductDistanceBased(selectedProduct.getProductName())) {
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

    private boolean isProductDistanceBased(String productName) {
        productName = productName.toLowerCase();
        return productName.contains("praxspin") || productName.contains("praxfit");
    }

    private void initOnFocusChangeListener(final Product selectedProduct) {
        routefilmScenery.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    itemView.setSelected(true);
                    if (routefilmsAdapter != null) {
                        routefilmsAdapter.setSelectedRoutefilm(position);
                    }
//                    if (selectedProduct.getSupportStreaming()==0) {
//                        PraxtourDatabase.databaseWriterExecutor.execute(()->{
//                            PraxtourDatabase.getDatabase(itemView.getContext()).usageTrackerDao().setSelectedMovie(apikey, movie.getId());
//                        });
//                    }
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

        if (itemView.isSelected() ) {
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

    private void initMovieImages() {
        //Set product image in button
        if(movie.getMovieImagepath().startsWith("/")) {
            Picasso.get()
                    .load(new File(movie.getMovieImagepath()))
                    .resize(180, 242)
                    .into(routefilmScenery, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            LogHelper.WriteLogRule(itemView.getContext(), apikey,"[LOCAL] Filepath: "+movie.getMovieImagepath()+" : Error> "+e.getLocalizedMessage(), "ERROR", "");
                        }
                    });
        } else {
            Picasso.get()
                    .load(movie.getMovieImagepath())
                    .resize(180, 242)
                    .into(routefilmScenery, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            LogHelper.WriteLogRule(itemView.getContext(), apikey,"[STREAM] Filepath: "+movie.getMovieImagepath()+" : Error> "+e.getLocalizedMessage(), "ERROR", "");
                        }
                    });
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
        });
    }

    private void initStartListeners(final Product selectedProduct) {
        routefilmScenery.setOnClickListener((onClickedView) -> {
            updateRouteInformationBlock();
            if (selectedProduct.getSupportStreaming()==0) {
                startVideoplayer();
            } else {
                performSpeedtestAndStartVideoPlayer();
            }
        });
    }

    private void initVideoPlayer(final Bundle arguments) {
        Log.d(TAG, "Context Viewholder VideoplayerActivity class: " + itemView.getContext());
        videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
        videoPlayer.putExtras(arguments);
    }

    private void initStreamVideoPlayer(final Bundle arguments) {
        Log.d(TAG, "Context Viewholder VideoplayerExoActivity class: " + itemView.getContext());
        videoPlayerExo = new Intent(itemView.getContext(), VideoplayerExoActivity.class);
        videoPlayerExo.putExtras(arguments);
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

    private void initFavorite() {
        favoriteButton.setOnClickListener(view -> {
            favoriteButton.setEnabled(false);

            if (favorited) {
                favoritedStar.setVisibility(View.GONE);
                unfavoritedStar.setVisibility(View.VISIBLE);
                unfavorite();
            } else {
                unfavoritedStar.setVisibility(View.GONE);
                favoritedStar.setVisibility(View.VISIBLE);
                favorite();
            }

            new Handler().postDelayed(() -> {
                favoriteButton.setEnabled(true);
            }, 100);

            arrangeMoviesByFavorites();
        });

        Set<String> favoritedMovieIds = itemView.getContext().getSharedPreferences("app", Context.MODE_PRIVATE).getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);
        if (favoritedMovieIds != null) {
            favorited = favoritedMovieIds.contains(Routefilm.fromMovie(movie, apikey).getMovieId().toString());
        }

        recolorFavoriteButton();
    }

    private void arrangeMoviesByFavorites() {
        Set<String> favoritedMovieIds = itemView.getContext().getSharedPreferences("app", Context.MODE_PRIVATE)
                .getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);

        if (favoritedMovieIds == null) {
            return;
        }

        List<Routefilm> routefilmList = routefilmsAdapter.getRoutefilms();
        List<Routefilm> sortedRoutefilmList = new ArrayList<>();

        // Add favorited movies first
        for (Routefilm routefilm : routefilmList) {
            if (favoritedMovieIds.contains(routefilm.getMovieId().toString())) {
                sortedRoutefilmList.add(routefilm);
            }
        }

        // Then add rest
        for (Routefilm routefilm : routefilmList) {
            if (! sortedRoutefilmList.contains(routefilm)) {
                sortedRoutefilmList.add(routefilm);
            }
        }

        routefilmsAdapter.rebuildRoutefilmList(sortedRoutefilmList);
    }

    private void recolorFavoriteButton() {
        if (favorited) {
            favoritedStar.setVisibility(View.VISIBLE);
            unfavoritedStar.setVisibility(View.GONE);
        } else {
            favoritedStar.setVisibility(View.GONE);
            unfavoritedStar.setVisibility(View.VISIBLE);
        }
    }

    private void favorite() {
        // Retrieve already favorited set
        SharedPreferences sp = super.itemView.getContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        Set<String> favoritedMovieIds = sp.getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);
        if (favoritedMovieIds == null) {
            favoritedMovieIds = new HashSet<>();
        }
        Set<String> updatedFavoritedMovieIds = new HashSet<>(favoritedMovieIds);

        // Add the new movie to it
        updatedFavoritedMovieIds.add(Routefilm.fromMovie(movie, apikey).getMovieId().toString());
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, updatedFavoritedMovieIds);
        editor.apply();
        favorited = true;
    }

    private void unfavorite() {
        SharedPreferences sp = super.itemView.getContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        Set<String> favoritedMovieIds = sp.getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);
        if (favoritedMovieIds == null) {
            return;
        }

        Set<String> updatedFavoritedMovieIds = new HashSet<>(favoritedMovieIds);
        updatedFavoritedMovieIds.remove(Routefilm.fromMovie(movie, apikey).getMovieId().toString());
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, updatedFavoritedMovieIds);
        editor.apply();

        favorited = false;
    }

    private void performSpeedtestAndStartVideoPlayer() {
        //Perform speed check
//        HandlerThread thread = new HandlerThread("Speedtest",
//                Process.THREAD_PRIORITY_MORE_FAVORABLE);
//        thread.start();
//
//        Handler speedtestHandler = new Handler(thread.getLooper());
//        Runnable runnableSpeedTest = new Runnable() {
//            @Override
//            public void run() {
//                SpeedTestSocket speedTestSocket = new SpeedTestSocket();
//
//                // add a listener to wait for speedtest completion and progress
//                speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
//
//                    @Override
//                    public void onCompletion(final SpeedTestReport report) {
//                        // called when download/upload is finished
//                        Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
//                        Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
//                        if (report.getTransferRateBit().intValue() > ApplicationSettings.SPEEDTEST_MINIMUM_SPEED.intValue()) {
////                            startVideoplayer();
//                        } else {
////                            thread.getLooper().prepare();
////                            LogHelper.WriteLogRule(itemView.getContext(), apikey, "[SPEED ERROR] Internet Speed to low at starting movie: "+movie.getMovieTitle(), "ERROR", "");
////
////                            Bundle arguments = new Bundle();
////                            arguments.putInt("needed_speed", ApplicationSettings.SPEEDTEST_MINIMUM_SPEED.intValue());
////                            arguments.putInt("measured_speed", report.getTransferRateBit().intValue());
////                            arguments.putString("communication_device", selectedProduct.getCommunicationType());
////                            arguments.putString("product_object", new GsonBuilder().create().toJson(selectedProduct, Product.class));
////
////                            //FIXME: navcontroller needs to be activated from main thread
////                            AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
////                            NavHostFragment navHostFragment =
////                                    (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
////                            NavController navController = navHostFragment.getNavController();
////                            navController.navigate(R.id.errorFragment);
//                        }
//                        startVideoplayer();
//                    }
//
//                    @Override
//                    public void onError(SpeedTestError speedTestError, String errorMessage) {
//                        // called when a download/upload error occur
//                        Log.d(getClass().getSimpleName(), "ERROR :: "+errorMessage);
//                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
////                        activity.getSupportFragmentManager()
////                                .beginTransaction()
////                                .add(R.id.fragment_container_view, SpeedtestErrorFragment.class, null)
////                                .disallowAddToBackStack()
////                                .commit();
//                        NavHostFragment navHostFragment =
//                                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
//                        NavController navController = navHostFragment.getNavController();
//                        navController.navigate(R.id.errorFragment);
//                    }
//
//                    @Override
//                    public void onProgress(float percent, SpeedTestReport report) {
//                        // called to notify download/upload progress
//                        Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
//                        Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                        Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
//                    }
//                });
//
//                speedTestSocket.startDownload(PRAXCLOUD_MEDIA_URL+"/1M.iso");
//            }
//        };
//        speedtestHandler.postDelayed(runnableSpeedTest,0);
        startVideoplayerExo();
    }

    public void startVideoplayer() {
        itemView.getContext().startActivity(videoPlayer);
    }

    public void startVideoplayerExo() {
        itemView.getContext().startActivity(videoPlayerExo);
    }
}
