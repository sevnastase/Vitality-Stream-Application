package com.videostreamtest.ui.phone.productview.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.productview.fragments.touch.TouchScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import java.io.File;

public class TouchScreenFragment extends Fragment {

    private ProductViewModel productViewModel;

    private TouchScreenRouteFilmsAdapter touchScreenRouteFilmsAdapter;
    private RecyclerView recyclerView;

    private ImageButton navigationLeftArrow;
    private ImageButton navigationRightArrow;

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

        recyclerView = view.findViewById(R.id.recyclerview_available_routefilms);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),4));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String response = "";
        final TextView textView = view.findViewById(R.id.temp_text);
        textView.setText(response);

        loadAvailableMediaScenery();
    }

    private void loadNavigationArrows() {
        productViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), currentConfig -> {
            if (currentConfig != null) {
                Picasso.get()
                        .load("http://"+currentConfig.getPraxCloudMediaServerUrl()+"/media/arrow_left_blue.png")
                        .fit()
                        .into(navigationLeftArrow);
                Picasso.get()
                        .load("http://"+currentConfig.getPraxCloudMediaServerUrl()+"/media/arrow_right_blue.png")
                        .fit()
                        .into(navigationRightArrow);
            }
        });

    }

    private void loadAvailableMediaScenery() {
        productViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(getViewLifecycleOwner(), routefilms -> {
                    Log.d(getClass().getSimpleName(), "TEST COUNTER");
                    Product selectedProduct = new GsonBuilder().create().fromJson(getArguments().getString("product_object", "{}"), Product.class);
                    CommunicationDevice communicationDevice = ConfigurationHelper.getCommunicationDevice(getArguments().getString("communication_device"));
                    touchScreenRouteFilmsAdapter = new TouchScreenRouteFilmsAdapter(routefilms.toArray(new Routefilm[0]), selectedProduct, communicationDevice);
                    recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
                    recyclerView.getAdapter().notifyDataSetChanged();
                });
            }
        });
    }

}
