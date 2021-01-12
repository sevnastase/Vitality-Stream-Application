package com.videostreamtest.ui.phone.profiles;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.Profile;

public class ProfileViewHolder extends RecyclerView.ViewHolder {
    private ImageButton profileImageButton;
    private TextView profileName;

    public ProfileViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(Profile profile, int position) {
        profileName = itemView.findViewById(R.id.profile_name);
        profileName.setText(profile.getProfileName());
    }

}
