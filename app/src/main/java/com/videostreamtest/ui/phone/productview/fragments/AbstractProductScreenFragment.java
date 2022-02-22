package com.videostreamtest.ui.phone.productview.fragments;

import static android.content.Context.MODE_PRIVATE;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.productview.fragments.routefilmadapter.RoutefilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;

public class AbstractProductScreenFragment extends Fragment {
    private static final String TAG = AbstractProductScreenFragment.class.getSimpleName();

    //Static final Strings for navigation arrow urls
    private static final String NAVIGATION_LEFT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_left_blue.png";
    private static final String NAVIGATION_RIGHT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_right_blue.png";
    private static final String NAVIGATION_UP_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_up_blue.png";
    private static final String NAVIGATION_DOWN_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_down_blue.png";

    private int refreshOverviewCounter = 0;

    //Data Model
    private ProductViewModel productViewModel;

    //Data
    private Product selectedProduct;

    private String apikey;
    private int currentposition;
    private List<Routefilm> routefilmsList = new ArrayList<>();
    private List<Flag> flags = new ArrayList<>();
    private List<MovieFlag> movieFlags = new ArrayList<>();

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

        apikey = getActivity().getSharedPreferences("app", MODE_PRIVATE).getString("apikey","");

        Log.d(TAG, "View Tag :: "+view.getTag());
        LogHelper.WriteLogRule(view.getContext().getApplicationContext(),apikey,"Loaded density: "+view.getTag(), "DEBUG", "");

        //Routeinformation block
        routeInformationBlock = view.findViewById(R.id.overlay_route_information);
        //Routefilm overview
        routefilmOverview = view.findViewById(R.id.recyclerview_available_routefilms);
        routefilmOverview.setHasFixedSize(true);
        routefilmOverview.setLayoutManager(new GridLayoutManager(view.getContext(),4));

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
                loadFlags();
                loadMovieFlags();
            }
        });
        //load selected movie and show selected on screen.
        productViewModel.getSelectedRoutefilm().observe(getViewLifecycleOwner(), routefilm ->{
            if (routefilm != null && this.currentposition != getCurrentPosition(routefilm)) {
                this.currentposition = getCurrentPosition(routefilm);
            }
        });

        refreshRoutefilmOverView(4,500);
    }

    //NAVIGATION ARROWS
    private void loadNavigationArrows() {
        if (selectedProduct != null) {
            Picasso.get()
                    .load(NAVIGATION_LEFT_ARROW)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .fit()
                    .into(navigationLeftArrow);
            Picasso.get()
                    .load(NAVIGATION_RIGHT_ARROW)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .fit()
                    .into(navigationRightArrow);
            Picasso.get()
                    .load(NAVIGATION_UP_ARROW)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .fit()
                    .into(navigationUpArrow);
            Picasso.get()
                    .load(NAVIGATION_DOWN_ARROW)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
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
            productViewModel.getProductMovies(apikey)
                    .observe(getViewLifecycleOwner(), routefilms -> {
                if (routefilms != null) {
                    this.routefilmsList = routefilms;
                    if (routefilmsAdapter!=null) {
                        routefilmsAdapter.updateRoutefilmList(routefilms);
                        routefilmsLoaded = true;
                        showRoutefilmOverview();
                    }
                }
            });
        }
    }

    private void loadFlags() {
        productViewModel.getAllFlags().observe(getViewLifecycleOwner(), flags -> {
            if (flags != null) {
                this.flags = flags;
                if (routefilmsAdapter!= null) {
                    routefilmsAdapter.updateFlagList(flags);
                    flagsLoaded = true;
                    showRoutefilmOverview();
                }
            }
        });
    }

    private void loadMovieFlags() {
        productViewModel.getMovieFlags().observe(getViewLifecycleOwner(), movieFlags -> {
            if (movieFlags!= null) {
                this.movieFlags = movieFlags;
                if (routefilmsAdapter!=null) {
                    routefilmsAdapter.updateMovieFlagList(movieFlags);
                    movieFlagsLoaded = true;
                    showRoutefilmOverview();
                }
            }
        });
    }

    private int getCurrentPosition(final Routefilm routefilm) {
        int index = -1;
        if (routefilmsList != null && routefilmsList.size()>0) {
            for (int searchIndex = 0; searchIndex < routefilmsList.size();searchIndex++ ) {
                if (routefilm.getMovieId().intValue() == routefilmsList.get(searchIndex).getMovieId().intValue()) {
                    index = searchIndex;
                }
            }
        }
        return index;
    }

    private void showRoutefilmOverview() {
        if (routefilmsLoaded&&flagsLoaded&&movieFlagsLoaded) {
            final LinearLayout loadingMessage = getActivity().findViewById(R.id.loading_overview);
            loadingMessage.setVisibility(View.GONE);

            routeInformationBlock.setVisibility(View.VISIBLE);
            if (ViewHelper.isTouchScreen(getActivity())) {
                navigationPad.setVisibility(View.VISIBLE);
            }
            routefilmOverview.setVisibility(View.VISIBLE);
        }
    }

    private void refreshRoutefilmOverView(int howManyTimes, int howLongInMs) {
        refreshOverviewCounter = 0;
        Handler refreshTimer = new Handler(Looper.getMainLooper());
        Runnable refreshRoutefilmOverview = new Runnable() {
            public void run() {
                if (routefilmOverview !=null && routefilmOverview.getAdapter()!=null) {
                    routefilmOverview.getAdapter().notifyDataSetChanged();
                    if (routefilmsLoaded&&flagsLoaded&&movieFlagsLoaded) {
                        refreshOverviewCounter++;
                    }
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
}
