package com.videostreamtest.ui.phone.productpicker.fragments.productpicker;

import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerAdapter;
import com.videostreamtest.ui.phone.productpicker.ProductPickerViewModel;
import com.videostreamtest.ui.phone.productview.ProductActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProductPickerFragment extends Fragment {
    private final static String TAG = ProductPickerFragment.class.getSimpleName();

    private ProductPickerViewModel productPickerViewModel;
    private RecyclerView productOverview;
    private ProductPickerAdapter productPickerAdapter;


    // FOR CHINESPORT
    /**
     * When a new MQTT message comes in, the MQTTService sends out a broadcast intent message signifying
     * that a new message has arrived. The BroadcastReceiver receives this message (either the
     * StartLeg or StartArm message), and starts the currently selected film. The starting of the film
     * doesn't fully work yet.
     */
    private BroadcastReceiver startFilmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startCurrentFilm();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_productpicker, container, false);

        productPickerViewModel = new ViewModelProvider(requireActivity()).get(ProductPickerViewModel.class);

        productOverview = view.findViewById(R.id.recyclerview_products);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        productPickerViewModel.getAccountProducts(AccountHelper.getAccountToken(getActivity()), !AccountHelper.getAccountType(getActivity()).equalsIgnoreCase("standalone"))
                .observe(getViewLifecycleOwner(), products ->{

            List<Product> productList = new ArrayList<>();
            if (products.size()>0) {
                for (com.videostreamtest.config.entity.Product extProd : products) {
                    Product addProd = new Product();
                    addProd.setId(extProd.getUid());
                    addProd.setDefaultSettingsId(0);
                    addProd.setProductLogoButtonPath(extProd.getProductLogoButtonPath());
                    addProd.setSupportStreaming(extProd.getSupportStreaming());
                    addProd.setProductName(extProd.getProductName());
                    addProd.setBlocked(extProd.getBlocked());
                    addProd.setCommunicationType(extProd.getCommunicationType());
                    productList.add(addProd);
                }
            }

            productPickerAdapter = new ProductPickerAdapter(productList.toArray(new Product[0]));
            //set adapter to recyclerview
            productOverview.setAdapter(productPickerAdapter);
            //set recyclerview visible
            productOverview.setVisibility(View.VISIBLE);

            // Initialise BroadcastReceiver
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(startFilmReceiver,
                    new IntentFilter("com.videostreamtest.ACTION_START_FILM"));

            //For UI alignment in center with less then 5 products
            int spanCount = 5;
            if (products.size() < 5) {
                spanCount = products.size();
            }
            if (spanCount ==0) {
                spanCount =1;
            }
            //Grid Layout met een max 5 kolommen breedte
            final GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(),spanCount);
            //Zet de layoutmanager erin
            productOverview.setLayoutManager(gridLayoutManager);
        });

    }

    /**
     * The broadcast receiver is initialised in the onviewcreated method and so is the productpickeradapter.
     * Once the productpickeradapter is initalised, we can access the currently selected product via the
     * getSelectedProduct() method from the productpickeradapter class. Then, start this film.
     */
    private void startCurrentFilm() {

        if (productPickerAdapter != null) {
            Product selectedProduct = productPickerAdapter.getSelectedProduct();

            if (selectedProduct != null) {
                // Start the film
                Bundle arguments = new Bundle();
                arguments.putString("product_object", new GsonBuilder().create().toJson(selectedProduct, Product.class));

                Intent startFilmView = new Intent(getContext(), ProductActivity.class);
                startFilmView.putExtras(arguments);

                SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences("app", Context.MODE_PRIVATE);
                String accounttoken = sharedPreferences.getString("apikey", "");

                PraxtourDatabase.databaseWriterExecutor.execute(()->{
                    PraxtourDatabase.getDatabase(getContext().getApplicationContext()).usageTrackerDao().setSelectedProduct(accounttoken, selectedProduct.getId());
                    Log.d(TAG, "Written to db: "+accounttoken+" , "+selectedProduct.getId());
                });

                getContext().startActivity(startFilmView);
            } else {
                Toast.makeText(getContext(), "No product selected", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Adapter is null. This should not happen if the broadcast is received after onViewCreated.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(startFilmReceiver);
    }
}
