package com.videostreamtest.ui.phone.productview.fragments.messagebox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.repository.BluetoothDefaultDeviceRepository;
import com.videostreamtest.data.model.BleDeviceInfo;
import com.videostreamtest.service.ble.BleService;
import com.videostreamtest.ui.phone.helpers.BleHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;
import com.videostreamtest.utils.ApplicationSettings;

import org.jetbrains.annotations.NotNull;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BleDeviceInformationViewHolder extends RecyclerView.ViewHolder {
    final static String TAG = BleDeviceInformationViewHolder.class.getSimpleName();

    private ProductViewModel productViewModel;

    public BleDeviceInformationViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
    }

    public void bind(BleDeviceInfo bleDeviceInfo, ProductViewModel productViewModel, int position) {
        this.productViewModel = productViewModel;

        if (isTouchScreen()) {
            initTouchBorders();
        } else {
            initBorders();
            initOnFocusChangeListener();
        }
        initOnClickListener(bleDeviceInfo);

        ImageView iconImage = itemView.findViewById(R.id.single_ble_icon);
        if (bleDeviceInfo.getDeviceType().toLowerCase().equals("running")) {
            Drawable walkingIcon = itemView.getContext().getDrawable(R.drawable.walk_sensor_icon_small);
            iconImage.setImageDrawable(walkingIcon);
        }
        if (bleDeviceInfo.getDeviceType().toLowerCase().equals("cycling")) {
            Drawable cyclingIcon = itemView.getContext().getDrawable(R.drawable.bike_sensor_icon_small);
            iconImage.setImageDrawable(cyclingIcon);
        }

        TextView deviceNameText = itemView.findViewById(R.id.single_ble_device_name);
        deviceNameText.setText(bleDeviceInfo.getBluetoothDevice().getName());

        TextView deviceConnectionStrengthText = itemView.findViewById(R.id.single_ble_device_connection_strength);
        deviceConnectionStrengthText.setText(BleHelper.getRssiStrengthIndicator(itemView.getContext().getApplicationContext(), bleDeviceInfo.getConnectionStrength()));
    }

    private void initBorders() {
        drawSelectionBorder();
        undrawSelectionBorder();

        if (itemView.isSelected() ) {
            drawSelectionBorder();
        } else {
            undrawSelectionBorder();
        }
    }

    private boolean isTouchScreen() {
        return itemView.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    private void initTouchBorders() {
//        drawSelectionBorder();
    }

    private void drawSelectionBorder() {
        final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_red_border);
        itemView.setBackground(border);
        itemView.setAlpha(1.0f);
    }

    private void undrawSelectionBorder() {
//        itemView.setBackground(null);
        itemView.setAlpha(0.6f);
    }

    private void initOnFocusChangeListener() {
        itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "Selected BleDeviceInfo: "+getAdapterPosition()+" hasFocus: "+hasFocus);
                itemView.setSelected(true);
                if (hasFocus) {
                    drawSelectionBorder();
                } else {
                    undrawSelectionBorder();
                }
            }
        });
    }

    private void initOnClickListener(final BleDeviceInfo bleDeviceInfo) {
        itemView.setOnClickListener(onClickedView -> {
            itemView.requestFocus();
            Log.d(TAG, "CLICKED ON DEVICE ITEMVIEW : " + bleDeviceInfo.getBluetoothDevice().getName());
            saveDefaultSelectedDevice(bleDeviceInfo);
            restartBleService();
            showConnectedMessage(bleDeviceInfo);
            closeMessageBox();
        });
    }

    private void saveDefaultSelectedDevice(final BleDeviceInfo bleDeviceInfo) {
        SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_KEY, bleDeviceInfo.getBluetoothDevice().getAddress());
        editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_NAME_KEY, bleDeviceInfo.getBluetoothDevice().getName());
        editor.putString(ApplicationSettings.DEFAULT_BLE_DEVICE_CONNECTION_STRENGTH_KEY, BleHelper.getRssiStrengthIndicator(itemView.getContext().getApplicationContext(), bleDeviceInfo.getConnectionStrength()));
        editor.commit();

        if (productViewModel==null) {
            return;
        }

        BluetoothDefaultDevice bluetoothDefaultDevice = new BluetoothDefaultDevice();
        bluetoothDefaultDevice.setBleId(1);
        bluetoothDefaultDevice.setBleAddress(bleDeviceInfo.getBluetoothDevice().getAddress());
        bluetoothDefaultDevice.setBleName(bleDeviceInfo.getBluetoothDevice().getName());
        bluetoothDefaultDevice.setBleSensorType(bleDeviceInfo.getDeviceType());
        bluetoothDefaultDevice.setBleSignalStrength(BleHelper.getRssiStrengthIndicator(itemView.getContext().getApplicationContext(), bleDeviceInfo.getConnectionStrength()));
        bluetoothDefaultDevice.setBleBatterylevel("--");
        productViewModel.insertBluetoothDefaultDevice(bluetoothDefaultDevice);

    }

    private void restartBleService() {
        itemView.getContext().stopService(new Intent(itemView.getContext().getApplicationContext(), BleService.class));

        Intent bleServiceIntent = new Intent(itemView.getContext().getApplicationContext(), BleService.class);
        itemView.getContext().startService(bleServiceIntent);
    }

    private void showConnectedMessage(final BleDeviceInfo bleDeviceInfo) {
        Toast.makeText(itemView.getContext(), "Succesfully connected to "+bleDeviceInfo.getBluetoothDevice().getName()+"!", Toast.LENGTH_LONG).show();
    }

    private void closeMessageBox() {
        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
        Fragment searchFragment = activity.getSupportFragmentManager().findFragmentByTag("device-information");
        if (searchFragment != null) {
            activity.getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
        }
    }
}
