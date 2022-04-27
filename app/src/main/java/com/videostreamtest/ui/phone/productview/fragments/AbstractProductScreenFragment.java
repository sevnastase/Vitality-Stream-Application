package com.videostreamtest.ui.phone.productview.fragments;

import static android.content.Context.MODE_PRIVATE;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

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
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.productview.fragments.routefilmadapter.RoutefilmsAdapter;
import com.videostreamtest.ui.phone.productview.layoutmanager.CustomGridLayoutManager;
import com.videostreamtest.ui.phone.productview.layoutmanager.EndlessRecyclerViewScrollListener;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        routefilmOverview.setHasFixedSize(true);

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
            if (AccountHelper.getAccountType(getActivity().getApplicationContext()).equalsIgnoreCase("standalone")) {
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

        }
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