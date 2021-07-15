package com.videostreamtest.ui.phone.productview.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Operation;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.productview.fragments.plain.PlainScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.fragments.touch.TouchScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TouchScreenFragment extends Fragment {
    private static final String NAVIGATION_LEFT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_left_blue.png";
    private static final String NAVIGATION_RIGHT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_right_blue.png";
    private static final String NAVIGATION_UP_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_up_blue.png";
    private static final String NAVIGATION_DOWN_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_down_blue.png";

    private ProductViewModel productViewModel;

    private TouchScreenRouteFilmsAdapter touchScreenRouteFilmsAdapter;
    private LinearLayout routeInformationBlock;
    private RecyclerView recyclerView;

//    private List<Routefilm> supportedRoutefilms;

    private ImageButton navigationLeftArrow;
    private ImageButton navigationRightArrow;
    private ImageButton navigationUpArrow;
    private ImageButton navigationDownArrow;

     /*
    TODO: create a viewHolder for a plain scenery representation and navigation arrow left/right
     - Number of items is maxed on 12 at a time.
     - Adapter is refreshed after each navigation arrow is pressed and the next or previous 10 movies are loaded
     - All the movies are in the local room database
     - all the movies are synched with the internet after logging in or loading the app when user is still logged in
     */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_overview_touch, container, false);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        navigationLeftArrow = view.findViewById(R.id.left_navigation_arrow);
        navigationRightArrow = view.findViewById(R.id.right_navigation_arrow);
        navigationUpArrow = view.findViewById(R.id.up_navigation_arrow);
        navigationDownArrow = view.findViewById(R.id.down_navigation_arrow);

        routeInformationBlock = view.findViewById(R.id.overlay_route_information);

        recyclerView = view.findViewById(R.id.recyclerview_available_routefilms);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),4));

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

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadNavigationArrows();
        loadAvailableMediaScenery();
    }

    private void loadNavigationArrows() {
        productViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), currentConfig -> {
            if (currentConfig != null) {
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
        });

    }

    private void loadAvailableMediaScenery() {
        productViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), currentConfig -> {
            if (currentConfig != null) {
                Product selectedProduct = new GsonBuilder().create().fromJson(getArguments().getString("product_object", "{}"), Product.class);

                productViewModel.getPMS(selectedProduct.getId()).observe(getViewLifecycleOwner(), pmsList -> {
                    productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(getViewLifecycleOwner(), allRoutefilms -> {
                        List<Routefilm> filteredRoutefilmList = new ArrayList<>();
                        if (pmsList.size()>0 && allRoutefilms.size()>0) {
                            for (Routefilm routefilm: allRoutefilms) {
                                for (ProductMovie productMovie: pmsList) {
                                    if (routefilm.getMovieId() == productMovie.getMovieId()) {
                                        filteredRoutefilmList.add(routefilm);
                                    }
                                }
                            }

                            CommunicationDevice communicationDevice = ConfigurationHelper.getCommunicationDevice(getArguments().getString("communication_device"));

                            touchScreenRouteFilmsAdapter = new TouchScreenRouteFilmsAdapter(filteredRoutefilmList.toArray(new Routefilm[0]), selectedProduct, communicationDevice);
                            touchScreenRouteFilmsAdapter.setRouteInformationBlock(routeInformationBlock);

                            recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    });
                });

//                productViewModel.getProductMovies(currentConfig.getAccountToken(), selectedProduct.getId()).observe(getViewLifecycleOwner(), routefilms -> {
//                    CommunicationDevice communicationDevice = ConfigurationHelper.getCommunicationDevice(getArguments().getString("communication_device"));
//
//                    touchScreenRouteFilmsAdapter = new TouchScreenRouteFilmsAdapter(routefilms.toArray(new Routefilm[0]), selectedProduct, communicationDevice);
//                    touchScreenRouteFilmsAdapter.setRouteInformationBlock(routeInformationBlock);
//
//                    recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
//                    recyclerView.getAdapter().notifyDataSetChanged();
//                });
            }
        });
    }

    private void setNavigationLeftArrow() {
        int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
        int nextPosition = 0;
        if (currentPosition == 0) {
            nextPosition = recyclerView.getAdapter().getItemCount() -1;
        } else {
            nextPosition = currentPosition -1;
        }
        touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

        recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
        recyclerView.getAdapter().notifyDataSetChanged();

        recyclerView.getLayoutManager().scrollToPosition(nextPosition);
    }

    private void setNavigationRightArrow() {
        int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
        int nextPosition = 0;
        if (currentPosition == (recyclerView.getAdapter().getItemCount() -1)) {
            nextPosition = 0;
        } else {
            nextPosition = currentPosition +1;
        }
        touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

        recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
        recyclerView.getAdapter().notifyDataSetChanged();

        recyclerView.getLayoutManager().scrollToPosition(nextPosition);
    }

    private void setNavigationUpArrow() {
        int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
        int nextPosition = 0;

        if (currentPosition <= 3) {
            nextPosition = currentPosition;
        } else {
            nextPosition = currentPosition - 4;
        }

        touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

        recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
        recyclerView.getAdapter().notifyDataSetChanged();

        recyclerView.getLayoutManager().scrollToPosition(nextPosition);
    }

    private void setNavigationDownArrow() {
        int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
        int nextPosition = 0;

        //TODO: SMOOTH CHECK BECAUSE LAGGY UX NOW
        if (currentPosition >= ((recyclerView.getAdapter().getItemCount() -1)-4)) {
            nextPosition = currentPosition;
        } else {
            nextPosition = currentPosition + 4;
        }

        touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

        recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
        recyclerView.getAdapter().notifyDataSetChanged();

        recyclerView.getLayoutManager().scrollToPosition(nextPosition);
    }

    private Routefilm getSupportedRoutefilm(List<Routefilm> routefilms, Integer movieId) {
        if (routefilms.size()>0) {
            for (Routefilm routefilm: routefilms) {
                if (routefilm.getMovieId() == movieId) {
                    return routefilm;
                }
            }
        }
        return null;
    }

}
