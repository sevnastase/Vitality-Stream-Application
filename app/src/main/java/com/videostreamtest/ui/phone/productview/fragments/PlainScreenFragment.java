package com.videostreamtest.ui.phone.productview.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.productview.fragments.plain.PlainScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.fragments.touch.TouchScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

public class PlainScreenFragment extends Fragment implements CatalogRecyclerViewClickListener {
    private ProductViewModel productViewModel;

    private RecyclerView recyclerView;
    private LinearLayout routeInformationBlock;
    private PlainScreenRouteFilmsAdapter plainScreenRouteFilmsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_overview_plain, container, false);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        routeInformationBlock = view.findViewById(R.id.overlay_route_information);

        recyclerView = view.findViewById(R.id.recyclerview_available_routefilms);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),4));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadAvailableMediaScenery();
    }

    private void loadAvailableMediaScenery() {
        productViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), currentConfig -> {
            if (currentConfig != null) {
                productViewModel.getRoutefilms(currentConfig.getAccountToken()).observe(getViewLifecycleOwner(), routefilms -> {
                    Product selectedProduct = new GsonBuilder().create().fromJson(getArguments().getString("product_object", "{}"), Product.class);
                    CommunicationDevice communicationDevice = ConfigurationHelper.getCommunicationDevice(getArguments().getString("communication_device"));

                    plainScreenRouteFilmsAdapter = new PlainScreenRouteFilmsAdapter(routefilms.toArray(new Routefilm[0]), selectedProduct, communicationDevice);
                    plainScreenRouteFilmsAdapter.setRouteInformationBlock(routeInformationBlock);
                    plainScreenRouteFilmsAdapter.setCatalogRecyclerViewClickListener(PlainScreenFragment.this);

                    recyclerView.setAdapter(plainScreenRouteFilmsAdapter);
                    recyclerView.getAdapter().notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        recyclerView.getLayoutManager().scrollToPosition(position);
    }
}
