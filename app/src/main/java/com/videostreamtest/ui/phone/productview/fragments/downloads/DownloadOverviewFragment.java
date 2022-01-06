package com.videostreamtest.ui.phone.productview.fragments.downloads;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.productview.fragments.downloads.adapter.RoutefilmDownloadsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

public class DownloadOverviewFragment extends Fragment {
    private static final String TAG = DownloadOverviewFragment.class.getSimpleName();
    private ProductViewModel productViewModel;

    private RoutefilmDownloadsAdapter routefilmDownloadsAdapter;
    private RecyclerView routeDownloadsOverview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routefilm_downloads_overview, container, false);
        routeDownloadsOverview = view.findViewById(R.id.recyclerview_routefilm_downloads);

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        routeDownloadsOverview.setHasFixedSize(true);
        routeDownloadsOverview.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        routefilmDownloadsAdapter = new RoutefilmDownloadsAdapter();
        routeDownloadsOverview.setAdapter(routefilmDownloadsAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final String apikey = view.getContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");
        productViewModel.getProductMovies(apikey).observe(getViewLifecycleOwner(), routefilms -> {
            if (routefilms!=null && routefilmDownloadsAdapter != null) {
                routefilmDownloadsAdapter.updateRoutefilmList(routefilms);
            }
        });
        productViewModel.getAllActiveDownloadStatus().observe(getViewLifecycleOwner(), standAloneDownloadStatusList -> {
            if (standAloneDownloadStatusList != null && routefilmDownloadsAdapter != null) {
                routefilmDownloadsAdapter.updateStandaloneDownloadStatusList(standAloneDownloadStatusList);
            }
        });
    }
}
