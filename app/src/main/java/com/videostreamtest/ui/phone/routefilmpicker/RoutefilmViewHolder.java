package com.videostreamtest.ui.phone.routefilmpicker;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;

public class RoutefilmViewHolder extends RecyclerView.ViewHolder {
    private final Drawable onSelectedOutline;
    ImageButton routefilmCoverPhotoImageButton;
    ImageButton favoriteImageButton;
    FrameLayout routefilmCoverPhotoHolder;

    public RoutefilmViewHolder(@NonNull View itemView) {
        super(itemView);

        routefilmCoverPhotoImageButton = itemView.findViewById(R.id.routefilm_cover_photo_imagebutton);
        routefilmCoverPhotoHolder = itemView.findViewById(R.id.routefilm_cover_photo_layout);
        onSelectedOutline = AppCompatResources.getDrawable(itemView.getContext(), R.drawable.imagebutton_blue_border);
        favoriteImageButton = itemView.findViewById(R.id.favorite_button);
        favoriteImageButton.setVisibility(View.GONE);
    }

    public void setSelected(boolean value) {
        toggleBorder(value);
    }

    private void toggleBorder(boolean value) {
        if (value) {
            routefilmCoverPhotoHolder.setForeground(onSelectedOutline);
            routefilmCoverPhotoImageButton.setAlpha(1.0f);
        } else {
            routefilmCoverPhotoHolder.setForeground(null);
            routefilmCoverPhotoImageButton.setAlpha(0.7f);
        }
    }
}
