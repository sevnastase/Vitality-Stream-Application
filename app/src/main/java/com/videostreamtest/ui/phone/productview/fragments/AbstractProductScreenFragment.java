package com.videostreamtest.ui.phone.productview.fragments;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.constants.SharedPreferencesConstants;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.helpers.LogHelper;
import com.videostreamtest.helpers.ViewHelper;
import com.videostreamtest.ui.phone.productview.fragments.routefilmadapter.RoutefilmsAdapter;
import com.videostreamtest.ui.phone.productview.layoutmanager.CustomGridLayoutManager;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AbstractProductScreenFragment extends Fragment {
    private static final String TAG = AbstractProductScreenFragment.class.getSimpleName();

    //Static final Strings for navigation arrow urls
    private static final String NAVIGATION_LEFT_ARROW = PRAXCLOUD_MEDIA_URL+"/media/arrow_left_blue.png";
    private static final String NAVIGATION_RIGHT_ARROW = PRAXCLOUD_MEDIA_URL+"/media/arrow_right_blue.png";
    private static final String NAVIGATION_UP_ARROW = PRAXCLOUD_MEDIA_URL+"/media/arrow_up_blue.png";
    private static final String NAVIGATION_DOWN_ARROW = PRAXCLOUD_MEDIA_URL+"/media/arrow_down_blue.png";

    private int refreshOverviewCounter = 0;

    //Data Model
    private ProductViewModel productViewModel;

    //Data
    private Product selectedProduct;

    private String apikey;
    private List<Routefilm> routefilmsList = new ArrayList<>();

    //Routeinformation block
    private LinearLayout routeInformationBlock;

    //Routefilms overview
    private RoutefilmsAdapter routefilmsAdapter;
    private RecyclerView routefilmOverview;

    //TouchScreen Elements
    private LinearLayout navigationPad;
    private ImageButton navigationLeftArrow;
    private ImageButton navigationRightArrow;
    private ImageButton navigationUpArrow;
    private ImageButton navigationDownArrow;

    private boolean routefilmsLoaded = false;
    private boolean flagsLoaded = false;
    private boolean movieFlagsLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_overview_touch, container, false);
        //Data model
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        apikey = AccountHelper.getAccountToken(getActivity());

        Log.d(TAG, "View Tag :: "+view.getTag());
        LogHelper.WriteLogRule(view.getContext().getApplicationContext(),apikey,"Loaded density: "+view.getTag(), "DEBUG", "");

        //Routeinformation block
        routeInformationBlock = view.findViewById(R.id.overlay_route_information);
        //Routefilm overview
        routefilmOverview = view.findViewById(R.id.recyclerview_available_routefilms);

        CustomGridLayoutManager gridLayoutManager = new CustomGridLayoutManager(getActivity(), 4);
        gridLayoutManager.setItemPrefetchEnabled(true);
        gridLayoutManager.setInitialPrefetchItemCount(50);

        routefilmOverview.setLayoutManager(gridLayoutManager);

        //Views [TOUCH-SCREEN SPECIFIC]
        navigationPad = view.findViewById(R.id.navigation_pad);
        navigationLeftArrow = view.findViewById(R.id.left_navigation_arrow);
        navigationRightArrow = view.findViewById(R.id.right_navigation_arrow);
        navigationUpArrow = view.findViewById(R.id.up_navigation_arrow);
        navigationDownArrow = view.findViewById(R.id.down_navigation_arrow);

        //INITIALIZE LISTENERS BASED ON SCREEN TYPE
        if (ViewHelper.isTouchScreen(getActivity())) {
            navigationLeftArrow.setOnClickListener(onClickView ->{
                setNavigationLeftArrow();
            });
            navigationRightArrow.setOnClickListener(onClickView -> {
                setNavigationRightArrow();
            });
            navigationUpArrow.setOnClickListener(onClickView -> {
                setNavigationUpArrow();
            });
            navigationDownArrow.setOnClickListener(onClickView -> {
                setNavigationDownArrow();
            });
        } else {
            navigationPad.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // FOR CHINESPORT
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mqttMessageReceiver,
                new IntentFilter("com.videostreamtest.ACTION_START_FILM"));

        //set selected product en load navigation arrows
        productViewModel.getSelectedProduct().observe(getViewLifecycleOwner(), selectedProduct-> {
            if (selectedProduct != null && this.selectedProduct == null) {
                this.selectedProduct = Product.fromProductEntity(selectedProduct);
                loadNavigationArrows();

                routefilmsAdapter = new RoutefilmsAdapter(this.selectedProduct, productViewModel, routeInformationBlock);
                routefilmOverview.setAdapter(routefilmsAdapter);

                loadProductMovies();
            }
        });
    }

    /**
     * Interface signalling to start the current film when the mqtt message is received.
     */
    // FOR CHINESPORT
    BroadcastReceiver mqttMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startSelectedMovie();
        }
    };

    // FOR CHINESPORT
    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mqttMessageReceiver);
        super.onDestroyView();
    }

    // FOR CHINESPORT
    /**
     * This method starts the currently selected movie. It first retrieves the currently selected
     * route film via the routefilms adapter, after which we pass the bundle to the videplayerintent,
     * so that it can retrieve the current product, and film within that product, along with tying
     * the apikey to the currently logged in account.
     */
    private void startSelectedMovie() {
        if (routefilmsAdapter != null) {
            Routefilm selectedRoutefilm = routefilmsAdapter.getCurrentSelectedRoutefilm();
            if (selectedRoutefilm != null) {
                Bundle arguments = generateBundleParameters(selectedRoutefilm, selectedProduct, apikey);

                Intent videoPlayerIntent;
                if (selectedProduct.getSupportStreaming() == 0) {
                    Log.d(TAG, "Context Fragment VideoplayerActivity class: " + getContext());
                    videoPlayerIntent = new Intent(getContext(), VideoplayerActivity.class);
                    videoPlayerIntent.putExtras(arguments);
                } else {
                    Log.d(TAG, "Context Fragment VideoplayerExoActivity class: " + getContext());
                    videoPlayerIntent = new Intent(getContext(), VideoplayerExoActivity.class);
                    videoPlayerIntent.putExtras(arguments);
                }

                videoPlayerIntent.putExtras(arguments);
                startActivity(videoPlayerIntent);
            }
        }
    }

    // FOR CHINESPORT
    /**
     * Generate bundle parameters analagous to how it is done in RoutefilmsViewHolder. Don't fully
     * understand the motivation behind this yet, but it is needed to start the correct film.
     *
     * @param routefilm         The currently highlighted film
     * @param selectedProduct   The current product (PraxFit/PraxSpin/PraxFit)
     * @param apikey            The unique token for this account
     * @return                  Return the bundle of arguments
     */
    private Bundle generateBundleParameters(Routefilm routefilm, Product selectedProduct, String apikey) {
        Bundle arguments = new Bundle();
        arguments.putString("movieObject", new GsonBuilder().create().toJson(Movie.fromRoutefilm(routefilm), Movie.class));
        arguments.putString("productObject", new GsonBuilder().create().toJson(selectedProduct, Product.class));
        arguments.putString("communication_device", selectedProduct.getCommunicationType());
        arguments.putString("accountToken", apikey);
        arguments.putBoolean("localPlay", selectedProduct.getSupportStreaming() == 0);
        return arguments;
    }


    //NAVIGATION ARROWS
    private void loadNavigationArrows() {
        if (selectedProduct != null) {
            Picasso.get()
                    .load(NAVIGATION_LEFT_ARROW)
                    .fit()
                    .into(navigationLeftArrow);
            Picasso.get()
                    .load(NAVIGATION_RIGHT_ARROW)
                    .fit()
                    .into(navigationRightArrow);
            Picasso.get()
                    .load(NAVIGATION_UP_ARROW)
                    .fit()
                    .into(navigationUpArrow);
            Picasso.get()
                    .load(NAVIGATION_DOWN_ARROW)
                    .fit()
                    .into(navigationDownArrow);
        }
    }

    private void setNavigationLeftArrow() {
        if (routefilmsAdapter!=null && routefilmsAdapter.getCurrentSelectedRoutefilmPosition() >0) {
            routefilmsAdapter.setSelectedRoutefilm(routefilmsAdapter.getCurrentSelectedRoutefilmPosition()-1);
            routefilmOverview.getAdapter().notifyDataSetChanged();
            routefilmOverview.getLayoutManager().scrollToPosition(routefilmsAdapter.getCurrentSelectedRoutefilmPosition());
        }
    }

    private void setNavigationRightArrow() {
        if (routefilmsAdapter!=null &&
                routefilmsAdapter.getCurrentSelectedRoutefilmPosition() <routefilmsAdapter.getItemCount()-1) {
            routefilmsAdapter.setSelectedRoutefilm(routefilmsAdapter.getCurrentSelectedRoutefilmPosition()+1);
            routefilmOverview.getAdapter().notifyDataSetChanged();
            routefilmOverview.getLayoutManager().scrollToPosition(routefilmsAdapter.getCurrentSelectedRoutefilmPosition());
        }
    }

    private void setNavigationUpArrow() {
        final int nextPosition = routefilmsAdapter.getCurrentSelectedRoutefilmPosition() - 4;
        if (routefilmsAdapter!=null &&
                nextPosition >= 0 ) {
            routefilmsAdapter.setSelectedRoutefilm(nextPosition);
            routefilmOverview.getAdapter().notifyDataSetChanged();
            routefilmOverview.getLayoutManager().scrollToPosition(routefilmsAdapter.getCurrentSelectedRoutefilmPosition());
        }
        if (routefilmsAdapter!=null &&
                nextPosition < 0 ) {
            routefilmsAdapter.setSelectedRoutefilm(0);
            routefilmOverview.getAdapter().notifyDataSetChanged();
            routefilmOverview.getLayoutManager().scrollToPosition(routefilmsAdapter.getCurrentSelectedRoutefilmPosition());
        }
    }

    private void setNavigationDownArrow() {
        final int nextPosition = routefilmsAdapter.getCurrentSelectedRoutefilmPosition() + 4;
        if (routefilmsAdapter!=null &&
                nextPosition > routefilmsAdapter.getItemCount()-1 ) {
            routefilmsAdapter.setSelectedRoutefilm(routefilmsAdapter.getItemCount()-1);
            routefilmOverview.getAdapter().notifyDataSetChanged();
            routefilmOverview.getLayoutManager().scrollToPosition(routefilmsAdapter.getCurrentSelectedRoutefilmPosition());
        }
        if (routefilmsAdapter!=null &&
                nextPosition <= routefilmsAdapter.getItemCount()-1 ) {
            routefilmsAdapter.setSelectedRoutefilm(nextPosition);
            routefilmOverview.getAdapter().notifyDataSetChanged();
            routefilmOverview.getLayoutManager().scrollToPosition(routefilmsAdapter.getCurrentSelectedRoutefilmPosition());
        }
    }

    private void loadProductMovies() {
        if (!apikey.equals("")) {
            if (AccountHelper.isLocalPlay(getContext())) {
                productViewModel.getStandaloneProductMovies(apikey)
                        .observe(getViewLifecycleOwner(), routefilms -> {
                            if (routefilms != null) {
                                preLoadImages(routefilms);
                                if (routefilmsAdapter!=null) {
                                    Log.d(TAG, "routefilms loaded");
                                    routefilmsAdapter.updateRoutefilmList(routefilms);
                                    routefilmOverview.getAdapter().notifyDataSetChanged();
                                    Log.d(TAG, "childCount: "+routefilmOverview.getAdapter().getItemCount());
                                    Log.d(TAG, "routefilms size: "+routefilms.size());
                                    if (routefilmOverview.getAdapter().getItemCount() == routefilms.size()) {
                                        showRoutefilmOverview();
                                        refreshRoutefilmOverView(4, 500);
                                    }
                                }
                            }
                        });
            }
            if (AccountHelper.getAccountType(getActivity().getApplicationContext()).equalsIgnoreCase("streaming")) {
                productViewModel.getProductMovies(apikey)
                        .observe(getViewLifecycleOwner(), routefilms -> {
                            if (routefilms != null) {
                                preLoadImages(routefilms);
                                if (routefilmsAdapter!=null) {
                                    Log.d(TAG, "routefilms loaded");
                                    routefilmsAdapter.updateRoutefilmList(routefilms);
                                    routefilmOverview.getAdapter().notifyDataSetChanged();
                                    Log.d(TAG, "childCount: "+routefilmOverview.getAdapter().getItemCount());
                                    Log.d(TAG, "routefilms size: "+routefilms.size());
                                    if (routefilmOverview.getAdapter().getItemCount() == routefilms.size()) {
                                        showRoutefilmOverview();
                                        refreshRoutefilmOverView(4, 500);
                                    }
                                }
                            }
                        });
            }

            new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
            new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
            new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);

        }
    }

    private void arrangeMoviesByFavorites() {
        Set<String> favoritedMovieIds;
        try {
            favoritedMovieIds = getContext().getSharedPreferences("app", Context.MODE_PRIVATE)
                    .getStringSet(SharedPreferencesConstants.FAVORITE_MOVIES, null);
        } catch (NullPointerException e) {
            return;
        }

        if (favoritedMovieIds == null) {
            return;
        }
        // FIXME getRoutefilms() returns nothing yet, ig its not initialized
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

    private void showRoutefilmOverview() {
        final LinearLayout loadingMessage = getActivity().findViewById(R.id.loading_overview);
        loadingMessage.setVisibility(View.GONE);

        routeInformationBlock.setVisibility(View.VISIBLE);
        if (ViewHelper.isTouchScreen(getActivity())) {
            navigationPad.setVisibility(View.VISIBLE);
        }
        routefilmOverview.setVisibility(View.VISIBLE);
    }

    private void refreshRoutefilmOverView(int howManyTimes, int howLongInMs) {
        refreshOverviewCounter = 0;
        Handler refreshTimer = new Handler(Looper.getMainLooper());
        Runnable refreshRoutefilmOverview = new Runnable() {
            public void run() {
                if (routefilmOverview !=null && routefilmOverview.getAdapter()!=null) {
                    routefilmOverview.getAdapter().notifyDataSetChanged();

                    refreshOverviewCounter++;

                    if (refreshOverviewCounter >= howManyTimes) {
                        refreshTimer.removeCallbacks(null);
                    } else {
                        refreshTimer.postDelayed(this, howLongInMs);
                    }
                }
            }
        };
        refreshTimer.postDelayed(refreshRoutefilmOverview, howLongInMs);
    }

    private void preLoadImages(List<Routefilm> routefilmList) {
        for (final Routefilm routefilm: routefilmList) {
            //Load local paths if standalone
            if (selectedProduct!= null && selectedProduct.getSupportStreaming()==0) {
                DownloadHelper.setLocalMedia(getActivity().getApplicationContext(), routefilm);
            }
            //preload scenery
            if (routefilm.getMovieRouteinfoPath().startsWith("/")) {
                Picasso.get().load(new File(routefilm.getMovieRouteinfoPath())).fetch();
            } else {
                Picasso.get().load(routefilm.getMovieRouteinfoPath()).fetch();
            }
            //preload maps
            if (routefilm.getMovieImagepath().startsWith("/")) {
                Picasso.get().load(new File(routefilm.getMovieImagepath())).fetch();
            } else {
                Picasso.get().load(routefilm.getMovieImagepath()).fetch();
            }
            //preload flags
            if (routefilm.getMovieFlagUrl() != null && !routefilm.getMovieFlagUrl().isEmpty()) {
                if (routefilm.getMovieFlagUrl().startsWith("/")) {
                    Picasso.get().load(new File(routefilm.getMovieFlagUrl())).fetch();
                } else {
                    Picasso.get().load(routefilm.getMovieFlagUrl()).fetch();
                }
            }
        }
    }
}
