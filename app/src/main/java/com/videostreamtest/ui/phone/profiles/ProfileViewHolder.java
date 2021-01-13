package com.videostreamtest.ui.phone.profiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.ui.phone.catalog.CatalogActivity;

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

        if (position == 0) {
            final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_blue_border);
            profileImageButton.setBackground(border);
            profileImageButton.setAlpha(1.0f);
        }
        profileImageButton.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return false;
            }
        });
        profileImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get sharedPreferences
                SharedPreferences myPreferences = v.getContext().getSharedPreferences("app",0);
                SharedPreferences.Editor editor = myPreferences.edit();
                editor.putString("profileKey", profile.getProfileKey());
                editor.putInt("profileId", profile.getProfileId());
                editor.commit();

                Intent catalog = new Intent(itemView.getContext(), CatalogActivity.class);
                itemView.getContext().startActivity(catalog);
                Toast.makeText(itemView.getContext(), "Profile "+profile.getProfileName()+" loading", Toast.LENGTH_LONG).show();
            }
        });
    }

}
