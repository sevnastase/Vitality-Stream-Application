package com.videostreamtest.ui.phone.profiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
    final static String TAG = ProfileViewHolder.class.getSimpleName();

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

        //init right size because of border
        drawSelectionborder();
        undrawSelectionborder();

        final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                itemView.setSelected(true);
                if (hasFocus) {
                    drawSelectionborder();
                } else {
                    undrawSelectionborder();
                }
            }
        };

        profileImageButton.setOnFocusChangeListener(focusChangeListener);

        if (itemView.isSelected() ) {
            drawSelectionborder();
        } else {
            undrawSelectionborder();
        }

        if (profile.getProfileKey().equals("new-profile")) {
            profileImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    profileImageButton.requestFocus();

                    Intent addProfile = new Intent(itemView.getContext(), AddProfileActivity.class);
                    itemView.getContext().startActivity(addProfile);
                }
            });
        } else {
            profileImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Get sharedPreferences
                    SharedPreferences myPreferences = v.getContext().getSharedPreferences("app", 0);
                    SharedPreferences.Editor editor = myPreferences.edit();
                    editor.putString("profileKey", profile.getProfileKey());
                    editor.putString("profileName", profile.getProfileName());
                    editor.putInt("profileId", profile.getProfileId());
                    editor.commit();

                    profileImageButton.requestFocus();

//                    Intent catalog = new Intent(itemView.getContext(), CatalogActivity.class);
//                    catalog.putExtra("profileName", profile.getProfileName());
//                    catalog.putExtra("profileKey", profile.getProfileKey());
//                    catalog.putExtra("profileId", profile.getProfileId());
//                    itemView.getContext().startActivity(catalog);
                    Toast.makeText(itemView.getContext(), toString().format(itemView.getContext().getString(R.string.loading_profile_message), profile.getProfileName()), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void drawSelectionborder() {
        final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_blue_border);
        profileImageButton.setBackground(border);
        profileImageButton.setAlpha(1.0f);
    }

    private void undrawSelectionborder() {
        profileImageButton.setBackground(null);
        profileImageButton.setAlpha(0.7f);
    }

}
