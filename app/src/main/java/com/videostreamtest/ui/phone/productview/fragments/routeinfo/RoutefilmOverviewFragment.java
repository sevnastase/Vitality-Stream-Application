package com.videostreamtest.ui.phone.productview.fragments.routeinfo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.productview.fragments.routefilmadapter.RoutefilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

public class RoutefilmOverviewFragment extends Fragment {
    private static final String TAG = RoutefilmOverviewFragment.class.getSimpleName();

    //ViewModel DataBinding
    private ProductViewModel productViewModel;
    //Views
    private RecyclerView routefilmOverview;
    //Data
    private RoutefilmsAdapter routefilmsAdapter;

    private Product fragmentProduct;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routefilm_overview, container, false);

        //Data model
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        //Views
        routefilmOverview = view.findViewById(R.id.recyclerview_available_routefilms);
        routefilmOverview.setHasFixedSize(true);
        routefilmOverview.setLayoutManager(new GridLayoutManager(view.getContext(),4));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String apikey = getActivity().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");

        productViewModel.getSelectedProduct().observe(getViewLifecycleOwner(), selectedProduct-> {
            if (selectedProduct != null) {
                Log.d(TAG, selectedProduct.getProductName());
                if(routefilmsAdapter == null) {
                    fragmentProduct = Product.fromProductEntity(selectedProduct);
                    routefilmsAdapter = new RoutefilmsAdapter(fragmentProduct, productViewModel);
                    routefilmOverview.setAdapter(routefilmsAdapter);
                }
            }
        });

        if (!apikey.equals("")) {
            productViewModel.getProductMovies(apikey)
                    .observe(getViewLifecycleOwner(), routefilms -> {

                        if(routefilms !=null && routefilmsAdapter!=null) {
                            routefilmsAdapter.updateRoutefilmList(routefilms);
//                            if (routefilmsAdapter.getItemCount() == routefilms.size() && routefilmsAdapter.getSelectedRoutefilm()==0) {
//                                PraxtourDatabase.databaseWriterExecutor.execute(()->{
//                                    PraxtourDatabase.getDatabase(requireActivity()).usageTrackerDao().setSelectedMovie(apikey, routefilmsAdapter.getRoutefilmList().get(0).getMovieId());
//                                });
//                                routefilmOverview.getAdapter().notifyDataSetChanged();
//                            }
                        }
                    });
        }

        Integer layoutHeightRecyclerView = 0;
        switch (getResources().getDisplayMetrics().densityDpi) {
            case 320:
                layoutHeightRecyclerView = 350;
                break;
            case 480:
                layoutHeightRecyclerView = 400;
                break;
            case 600:
                layoutHeightRecyclerView = 400;
                break;
            case 720:
                layoutHeightRecyclerView = 600;
                break;
            default:
                layoutHeightRecyclerView = 350;
        }

        if (layoutHeightRecyclerView!=null && layoutHeightRecyclerView.intValue()>0) {
            ViewGroup.LayoutParams params = routefilmOverview.getLayoutParams();
            //convert dpi to pixels
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, layoutHeightRecyclerView, getResources().getDisplayMetrics());
            params.height = height;
        }

        productViewModel.getSelectedRoutefilm().observe(getViewLifecycleOwner(), selectedRoutefilm -> {
            if (selectedRoutefilm != null) {
                Log.d(TAG, String.format("selectedRoutefilm: id>%d", selectedRoutefilm.getMovieId()));
                Log.d(TAG, String.format("selectedRoutefilmIntVal: id>%d", selectedRoutefilm.getMovieId().intValue()));
                setSelectedRoutefilm(selectedRoutefilm);
            }
        });
    }

    private void setSelectedRoutefilm(final Routefilm routefilm) {
        if (routefilmsAdapter != null && routefilmsAdapter.getItemCount() >0) {
            int oldPos = routefilmsAdapter.getSelectedRoutefilm();
            for (int pos = 0; pos < routefilmsAdapter.getRoutefilmList().size();pos++) {
                if (routefilmsAdapter.getRoutefilmList().get(pos).getMovieId().intValue() == routefilm.getMovieId().intValue() && oldPos!= pos) {
                    routefilmOverview.getLayoutManager().scrollToPosition(pos);
                    routefilmsAdapter.setSelectedRoutefilm(pos);
                    routefilmOverview.getAdapter().notifyDataSetChanged();
                }
            }
        }
    }
}
