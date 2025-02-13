package com.videostreamtest.ui.phone.productpicker.fragments.settings.wifi;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;

public class WifiViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = WifiViewHolder.class.getSimpleName();
    TextView networkNameTextView;
    ImageView networkStrengthImageView;
    ImageButton toggleCredentialsButton;
    Button connectButton;
    ConstraintLayout wifiCredentialsLayout;
    ConstraintLayout networkHeaderLayout;
    EditText networkPasswordInputField;

    public WifiViewHolder(@NonNull View itemView) {
        super(itemView);

        networkNameTextView = itemView.findViewById(R.id.network_name_textview);
        networkStrengthImageView = itemView.findViewById(R.id.connected_network_strength_imageview);
        toggleCredentialsButton = itemView.findViewById(R.id.show_enter_credentials_button);
        connectButton = itemView.findViewById(R.id.connect_to_network_button);
        wifiCredentialsLayout = itemView.findViewById(R.id.wifi_credentials_layout);
        networkHeaderLayout = itemView.findViewById(R.id.network_header_layout);
        networkPasswordInputField = itemView.findViewById(R.id.wifi_password_input);
    }
}
