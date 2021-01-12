package com.videostreamtest.ui.phone.profiles;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
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
        profileName.setTextSize(20);
        profileName.setTextColor(Color.WHITE);
        profileName.setText(profile.getProfileName());

        profileImageButton = itemView.findViewById(R.id.profile_avatar);
        Picasso.get()
                .load(profile.getProfileImgPath())
                .placeholder(R.drawable.cast_album_art_placeholder)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(profileImageButton);
        profileImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(itemView.getContext(), "Profile "+profile.getProfileName()+" loading", Toast.LENGTH_LONG).show();
            }
        });
    }

}
