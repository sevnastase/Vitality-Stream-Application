package com.videostreamtest.ui.phone.productview.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.productview.fragments.routefilmadapter.RoutefilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

public class AbstractProductScreenFragment extends Fragment implements CatalogRecyclerViewClickListener {
    private static final String TAG = AbstractProductScreenFragment.class.getSimpleName();

    //Static final Strings for navigation arrow urls
    private static final String NAVIGATION_LEFT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_left_blue.png";
    private static final String NAVIGATION_RIGHT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_right_blue.png";
    private static final String NAVIGATION_UP_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_up_blue.png";
    private static final String NAVIGATION_DOWN_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_down_blue.png";

    //Data Model
    private ProductViewModel productViewModel;

    //Passed arguments by Bundle
    private Product selectedProduct;
    private CommunicationDevice communicationDevice;

    //View elements
    private LinearLayout routeInformationBlock;
    private RecyclerView recyclerView;

    //Routefilms adapter
    private RoutefilmsAdapter routefilmsAdapter;

    //TouchScreen Elements
    private LinearLayout navigationPad;
    private ImageButton navigationLeftArrow;
    private ImageButton navigationRightArrow;
    private ImageButton navigationUpArrow;
    private ImageButton navigationDownArrow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_overview_touch, container, false);
        //Data model
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        //Bundle arguments
        selectedProduct = new GsonBuilder().create().fromJson(getArguments().getString("product_object", "{}"), Product.class);
        communicationDevice = ConfigurationHelper.getCommunicationDevice(getArguments().getString("communication_device"));

        //Views [GENERAL]
        routeInformationBlock = view.findViewById(R.id.overlay_route_information);

        routefilmsAdapter = new RoutefilmsAdapter(selectedProduct, communicationDevice, productViewModel, getActivity().getApplicationContext());
        routefilmsAdapter.setRouteInformationBlock(routeInformationBlock);
        routefilmsAdapter.setRouteInformationBlock(routeInformationBlock);

        recyclerView = view.findViewById(R.id.recyclerview_available_routefilms);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),4));
        recyclerView.setAdapter(routefilmsAdapter);

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
            routefilmsAdapter.setCatalogRecyclerViewClickListener(this);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadNavigationArrows();
        loadAvailableMediaScenery();
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        if (!ViewHelper.isTouchScreen(getActivity())) {
            routefilmsAdapter.setSelectedRoutefilm(position);
            recyclerView.getLayoutManager().scrollToPosition(position);
        }
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
        if (routefilmsAdapter != null) {
            int currentPosition = routefilmsAdapter.getSelectedRoutefilm();
            int nextPosition = 0;
            if (currentPosition == 0) {
                nextPosition = recyclerView.getAdapter().getItemCount() - 1;
            } else {
                nextPosition = currentPosition - 1;
            }
            routefilmsAdapter.setSelectedRoutefilm(nextPosition);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    private void setNavigationRightArrow() {
        if (routefilmsAdapter != null) {
            int currentPosition = routefilmsAdapter.getSelectedRoutefilm();
            int nextPosition = 0;
            if (currentPosition == (recyclerView.getAdapter().getItemCount() - 1)) {
                nextPosition = 0;
            } else {
                nextPosition = currentPosition + 1;
            }
            routefilmsAdapter.setSelectedRoutefilm(nextPosition);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    private void setNavigationUpArrow() {
        if (routefilmsAdapter != null) {
            int currentPosition = routefilmsAdapter.getSelectedRoutefilm();
            int nextPosition = 0;

            if (currentPosition <= 3) {
                nextPosition = currentPosition;
            } else {
                nextPosition = currentPosition - 4;
            }

            routefilmsAdapter.setSelectedRoutefilm(nextPosition);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    private void setNavigationDownArrow() {
        if (routefilmsAdapter != null) {
            int currentPosition = routefilmsAdapter.getSelectedRoutefilm();
            int nextPosition = 0;

            //TODO: SMOOTH CHECK BECAUSE LAGGY UX NOW
            if (currentPosition >= ((recyclerView.getAdapter().getItemCount() - 1) - 4)) {
                nextPosition = currentPosition;
            } else {
                nextPosition = currentPosition + 4;
            }

            routefilmsAdapter.setSelectedRoutefilm(nextPosition);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    //LOAD AVAILABLE SCENERY
    private void loadAvailableMediaScenery() {
        final String apikey = getActivity().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");
        if (!apikey.equals("")) {
            productViewModel.getProductMovies(apikey, selectedProduct.getId().intValue())
                    .observe(getViewLifecycleOwner(), routefilms -> {
                routefilmsAdapter.updateRoutefilmList(routefilms);
                if (routefilmsAdapter.getItemCount()>0 && routefilmsAdapter.getSelectedRoutefilm()==0) {
                    routefilmsAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}
