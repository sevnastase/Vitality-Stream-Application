package com.videostreamtest.ui.phone.productpicker.fragments.settings.wifi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.service.wifi.WifiManager;
import com.videostreamtest.service.wifi.WifiStrength;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;

import java.util.ArrayList;

public class WifiAdapter extends RecyclerView.Adapter<WifiViewHolder> {

    private static final String TAG = WifiAdapter.class.getSimpleName();

    private ArrayList<ScanResult> availableNetworks;
    private Activity activity;

    /**
     * @param availableNetworks the list of networks to display as items
     * @param activity the activity the recyclerview will be contained in (important for text color)
     */
    public WifiAdapter(ArrayList<ScanResult> availableNetworks, Activity activity) {
        this.availableNetworks = availableNetworks;
        this.activity = activity;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.wifi_item_layout, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        Log.d(TAG, "Binding data for position " + position);
        ScanResult network = availableNetworks.get(position);
        holder.networkNameTextView.setText(network.SSID);
        WifiStrength signalStrength = WifiManager.getSignalStrengthFromLevel(network.level);
        setWifiSignalImage(signalStrength, holder);

        holder.toggleCredentialsButton.setOnClickListener(view -> toggleCredentialsLayout(holder));
        holder.networkHeaderLayout.setOnClickListener(view -> toggleCredentialsLayout(holder));

        holder.connectButton.setOnClickListener(view -> sendConnectCommand(
                network,
                holder.networkPasswordInputField.getText().toString(),
                view.getContext()
        ));

        if (activity.getClass() == LoginActivity.class) {
            ViewHelper.setTextColorToWhiteInViewAndChildren(holder.itemView, R.color.white);
            holder.toggleCredentialsButton.setImageResource(R.drawable.closed_list_symbol_white);
        }
    }

    @Override
    public int getItemCount() {
        return availableNetworks != null ? availableNetworks.size() : 0;
    }

    public void updateAvailableNetworks(ArrayList<ScanResult> availableNetworks) {
        this.availableNetworks = availableNetworks;
        notifyDataSetChanged();
    }

    private void setWifiSignalImage(WifiStrength strength, WifiViewHolder holder) {
        if (activity.getClass() != LoginActivity.class) {
            switch (strength) {
                case EXCELLENT:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_4_black);
                    break;
                case GOOD:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_3_black);
                    break;
                case FAIR:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_2_black);
                    break;
                case POOR:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_1_black);
                    break;
                default:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_0_black);
                    break;
            }
        } else {
            switch (strength) {
                case EXCELLENT:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_4_white);
                    break;
                case GOOD:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_3_white);
                    break;
                case FAIR:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_2_white);
                    break;
                case POOR:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_1_white);
                    break;
                default:
                    holder.networkStrengthImageView.setImageResource(R.drawable.wifi_strength_0_white);
                    break;
            }
        }
    }

    private void toggleCredentialsLayout(WifiViewHolder holder) {
        if (holder.wifiCredentialsLayout.getVisibility() == View.GONE) {
            holder.wifiCredentialsLayout.setVisibility(View.VISIBLE);
            openToggleCredentialsButtonSymbol(holder);
        } else {
            holder.wifiCredentialsLayout.setVisibility(View.GONE);
            closeToggleCredentialsButtonSymbol(holder);
        }
    }

    private void openToggleCredentialsButtonSymbol(WifiViewHolder holder) {
        if (activity.getClass() == LoginActivity.class) {
            holder.toggleCredentialsButton.setImageResource(R.drawable.opened_list_symbol_white);
        } else {
            holder.toggleCredentialsButton.setImageResource(R.drawable.opened_list_symbol_black);
        }
    }

    private void closeToggleCredentialsButtonSymbol(WifiViewHolder holder) {
        if (activity.getClass() == LoginActivity.class) {
            holder.toggleCredentialsButton.setImageResource(R.drawable.closed_list_symbol_white);
        } else {
            holder.toggleCredentialsButton.setImageResource(R.drawable.closed_list_symbol_black);
        }
    }

    private void sendConnectCommand(ScanResult network, String password, Context context) {
        Intent intent = new Intent("com.videostreamtest.wifi.ACTION_CONNECT");
        intent.putExtra("theNetwork", network);
        intent.putExtra("thePassword", password);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
