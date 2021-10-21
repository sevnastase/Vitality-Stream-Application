package com.videostreamtest.ui.phone.productview.fragments.messagebox;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.BleDeviceInfo;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BleDeviceInformationAdapter extends RecyclerView.Adapter<BleDeviceInformationViewHolder> {
    private final static String TAG = BleDeviceInformationAdapter.class.getSimpleName();

    private List<BleDeviceInfo> bleDeviceInfoList;
    private int selectedBleDeviceInfo = 0;

    private ProductViewModel productViewModel;

    public BleDeviceInformationAdapter(final ProductViewModel productViewModel) {
        this.productViewModel = productViewModel;
        this.bleDeviceInfoList = new ArrayList<>();
    }

    @NonNull
    @NotNull
    @Override
    public BleDeviceInformationViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_single_ble_device_information, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new BleDeviceInformationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull BleDeviceInformationViewHolder holder, int position) {
        Log.d(TAG, "Binding ble device info to viewholders");
        holder.itemView.setSelected(selectedBleDeviceInfo==position);
        Log.d(TAG, "selectedBleDeviceInfoId: "+selectedBleDeviceInfo+", position: "+position);

        if (bleDeviceInfoList != null && bleDeviceInfoList.size() > 0) {
            Log.d(TAG, "position intended: "+position);
            holder.bind(bleDeviceInfoList.get(position), productViewModel, this, position);
        }

        if (selectedBleDeviceInfo == position) {
            holder.itemView.requestFocus();
            Button connectButton = holder.itemView.findViewById(R.id.single_ble_device_connect_button);
            connectButton.requestFocus();
        }
    }

    @Override
    public int getItemCount() {
        if (bleDeviceInfoList == null) {
            return 0;
        } else {
            return bleDeviceInfoList.size();
        }
    }

    public void setSelectedBleDeviceId(final int position) {
        this.selectedBleDeviceInfo = position;
    }

    public List<BleDeviceInfo> getAllBleDeviceInfo() {
        return bleDeviceInfoList;
    }

    public BleDeviceInfo getItemFromPosition(final int position) {
        if (bleDeviceInfoList==null) {
            return null;
        } else {
            return bleDeviceInfoList.get(position);
        }
    }

    public void addBleDeviceInfo(final BleDeviceInfo bleDeviceInfo) {
        if (bleDeviceInfoList==null) {
            bleDeviceInfoList = new ArrayList<>();
            bleDeviceInfoList.add(bleDeviceInfo);
        } else {
            bleDeviceInfoList.add(bleDeviceInfo);
        }
        notifyDataSetChanged();
    }
}
