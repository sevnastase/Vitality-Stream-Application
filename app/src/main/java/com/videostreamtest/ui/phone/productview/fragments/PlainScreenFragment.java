package com.videostreamtest.ui.phone.productview.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.catalog.CatalogRecyclerViewClickListener;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.productview.fragments.messagebox.BleDeviceInformationBoxFragment;
import com.videostreamtest.ui.phone.productview.fragments.plain.PlainScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.fragments.touch.TouchScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BLUETOOTH_SERVICE;

public class PlainScreenFragment extends Fragment implements CatalogRecyclerViewClickListener {
    private ProductViewModel productViewModel;

    private Button deviceConnectionbutton;
    private TextView deviceConnectionStrengthLabel;
    private TextView deviceBatterylevelLabel;
    private TextView deviceNameLabel;

    private RecyclerView recyclerView;
    private LinearLayout routeInformationBlock;

    private PlainScreenRouteFilmsAdapter plainScreenRouteFilmsAdapter;

    private List<Routefilm> supportedRoutefilms;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_overview_plain, container, false);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        deviceConnectionbutton = view.findViewById(R.id.current_connected_device_connect_button);
        deviceConnectionStrengthLabel = view.findViewById(R.id.current_connected_device_connection_strength_label);
        deviceBatterylevelLabel = view.findViewById(R.id.current_connected_device_battery_label);
        deviceNameLabel = view.findViewById(R.id.current_connected_device_label);

        routeInformationBlock = view.findViewById(R.id.overlay_route_information);

        recyclerView = view.findViewById(R.id.recyclerview_available_routefilms);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),4));

        deviceConnectionbutton.setOnClickListener(onClickView -> {
            openDeviceConnectionBlock();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadAvailableMediaScenery();
        loadBluetoothDefaultDeviceInformation();
        initOnFocusChangeDeviceConnectionButtonListener();
    }

    private void loadAvailableMediaScenery() {
        productViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), currentConfig -> {
            if (currentConfig != null) {
                Product selectedProduct = new GsonBuilder().create().fromJson(getArguments().getString("product_object", "{}"), Product.class);
                productViewModel.getProductMovies(currentConfig.getAccountToken(), selectedProduct.getId()).observe(getViewLifecycleOwner(), routefilms -> {
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

    private void loadBluetoothDefaultDeviceInformation() {
        BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        LinearLayout linearLayoutConnectionDeviceSummary = getView().findViewById(R.id.overlay_connection_info_box);
        if (bluetoothAdapter!= null) {
            bluetoothAdapter.enable();
            linearLayoutConnectionDeviceSummary.setVisibility(View.VISIBLE);

            productViewModel.getBluetoothDefaultDevices().observe(getViewLifecycleOwner(), bluetoothDefaultDevices -> {
                if (bluetoothDefaultDevices != null && bluetoothDefaultDevices.size()>0) {
                    BluetoothDefaultDevice bluetoothDefaultDevice =  bluetoothDefaultDevices.get(0);
                    if (bluetoothDefaultDevice.getBleName() != null && !bluetoothDefaultDevice.getBleName().isEmpty()) {
                        deviceNameLabel.setText(bluetoothDefaultDevice.getBleName());
                        if (!bluetoothDefaultDevice.getBleBatterylevel().isEmpty() && bluetoothDefaultDevice.getBleBatterylevel()!="") {
                            deviceBatterylevelLabel.setText(bluetoothDefaultDevice.getBleBatterylevel() + "%");
                        }
                        if (bluetoothDefaultDevice.getBleSignalStrength() != null && !bluetoothDefaultDevice.getBleSignalStrength().isEmpty()) {
                            deviceConnectionStrengthLabel.setText(bluetoothDefaultDevice.getBleSignalStrength());
                        }
                    } else {
                        deviceNameLabel.setText("No device");
                        deviceConnectionStrengthLabel.setText("No device");
                        deviceBatterylevelLabel.setText("0%");
                    }
                }
            });
        } else {
            linearLayoutConnectionDeviceSummary.setVisibility(View.GONE);
        }
    }

    private void openDeviceConnectionBlock() {
        Fragment searchFragment = getActivity().getSupportFragmentManager().findFragmentByTag("device-information");
        if (searchFragment == null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, new BleDeviceInformationBoxFragment(), "device-information")
                    .commit();
        }
    }

    private void initOnFocusChangeDeviceConnectionButtonListener() {
        deviceConnectionbutton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                deviceConnectionbutton.setSelected(true);
                if (hasFocus) {
                    final Drawable border = getContext().getDrawable(R.drawable.imagebutton_blue_border);
                    deviceConnectionbutton.setBackground(border);
                    deviceConnectionbutton.setBackgroundTintMode(PorterDuff.Mode.ADD);
                } else {
                    final Drawable border = getContext().getDrawable(R.drawable.imagebutton_red_border);
                    deviceConnectionbutton.setBackground(border);
                    deviceConnectionbutton.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER);
                }
            }
        });
    }
}
