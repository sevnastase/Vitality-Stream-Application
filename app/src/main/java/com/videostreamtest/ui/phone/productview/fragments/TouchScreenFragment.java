package com.videostreamtest.ui.phone.productview.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.enums.CommunicationDevice;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.productview.fragments.messagebox.BleDeviceInformationBoxFragment;
import com.videostreamtest.ui.phone.productview.fragments.touch.TouchScreenRouteFilmsAdapter;
import com.videostreamtest.ui.phone.productview.layoutmanager.PreCachingLayoutManager;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.utils.ApplicationSettings;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BLUETOOTH_SERVICE;

public class TouchScreenFragment extends Fragment {
    private static final String NAVIGATION_LEFT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_left_blue.png";
    private static final String NAVIGATION_RIGHT_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_right_blue.png";
    private static final String NAVIGATION_UP_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_up_blue.png";
    private static final String NAVIGATION_DOWN_ARROW = "http://188.166.100.139:8080/api/dist/img/buttons/arrow_down_blue.png";

    private ProductViewModel productViewModel;

    private TouchScreenRouteFilmsAdapter touchScreenRouteFilmsAdapter;
    private LinearLayout routeInformationBlock;
    private LinearLayout deviceConnectionInformationBlock;
    private RecyclerView recyclerView;

    private Button deviceConnectionbutton;
    private TextView deviceConnectionStrengthLabel;
    private TextView deviceBatterylevelLabel;
    private TextView deviceNameLabel;

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

        deviceConnectionbutton = view.findViewById(R.id.current_connected_device_connect_button);
        deviceConnectionStrengthLabel = view.findViewById(R.id.current_connected_device_connection_strength_label);
        deviceBatterylevelLabel = view.findViewById(R.id.current_connected_device_battery_label);
        deviceNameLabel = view.findViewById(R.id.current_connected_device_label);

        routeInformationBlock = view.findViewById(R.id.overlay_route_information);
        deviceConnectionInformationBlock = view.findViewById(R.id.overlay_connection_info_box);


        recyclerView = view.findViewById(R.id.recyclerview_available_routefilms);
        recyclerView.setHasFixedSize(true);
        PreCachingLayoutManager preCachingLayoutManager = new PreCachingLayoutManager(view.getContext(),4);
        preCachingLayoutManager.setItemPrefetchEnabled(true);
        preCachingLayoutManager.setExtraLayoutSpace(60);
        recyclerView.setLayoutManager(preCachingLayoutManager);

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

        deviceConnectionbutton.setOnClickListener(onClickView -> {
            openDeviceConnectionBlock();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadNavigationArrows();
        loadAvailableMediaScenery();
        loadBluetoothDefaultDeviceInformation();
        initOnFocusChangeDeviceConnectionButtonListener();
    }


    @Override
    public void onStart() {
        super.onStart();
        refreshRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        if (recyclerView != null && recyclerView.getAdapter()!=null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
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

                if (selectedProduct.getCommunicationType().toLowerCase().contains("none")) {
                    deviceConnectionInformationBlock.setVisibility(View.GONE);
                }

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
                        } else {
                            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.product_view_loading_data_message), Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        });
    }

    private void setNavigationLeftArrow() {
        if (touchScreenRouteFilmsAdapter != null) {
            int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
            int nextPosition = 0;
            if (currentPosition == 0) {
                nextPosition = recyclerView.getAdapter().getItemCount() - 1;
            } else {
                nextPosition = currentPosition - 1;
            }
            touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

            recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    private void setNavigationRightArrow() {
        if (touchScreenRouteFilmsAdapter != null) {
            int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
            int nextPosition = 0;
            if (currentPosition == (recyclerView.getAdapter().getItemCount() - 1)) {
                nextPosition = 0;
            } else {
                nextPosition = currentPosition + 1;
            }
            touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

            recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    private void setNavigationUpArrow() {
        if (touchScreenRouteFilmsAdapter != null) {
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
    }

    private void setNavigationDownArrow() {
        if (touchScreenRouteFilmsAdapter != null) {
            int currentPosition = touchScreenRouteFilmsAdapter.getSelectedMovie();
            int nextPosition = 0;

            //TODO: SMOOTH CHECK BECAUSE LAGGY UX NOW
            if (currentPosition >= ((recyclerView.getAdapter().getItemCount() - 1) - 4)) {
                nextPosition = currentPosition;
            } else {
                nextPosition = currentPosition + 4;
            }

            touchScreenRouteFilmsAdapter.setSelectedMovie(nextPosition);

            recyclerView.setAdapter(touchScreenRouteFilmsAdapter);
            recyclerView.getAdapter().notifyDataSetChanged();

            recyclerView.getLayoutManager().scrollToPosition(nextPosition);
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

    private void showCurrentConnectedDevice(final TextView deviceLabel) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("app" , Context.MODE_PRIVATE);
        String deviceName = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_NAME_KEY,"");
        String deviceConnectionStrength = sharedPreferences.getString(ApplicationSettings.DEFAULT_BLE_DEVICE_CONNECTION_STRENGTH_KEY, "");
        if (deviceName != null && !deviceName.isEmpty()) {
            deviceLabel.setText(deviceName);
            deviceConnectionbutton.setText("Switch device");
            if (deviceConnectionStrength != null && !deviceConnectionStrength.isEmpty()) {
                deviceConnectionStrengthLabel.setText(deviceConnectionStrength);
            }
        } else {
            deviceLabel.setText("No device");
            deviceConnectionbutton.setText("Connect device");
            deviceConnectionStrengthLabel.setText("No device");
        }
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
