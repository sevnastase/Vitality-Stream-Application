package com.videostreamtest.ui.phone.videoplayer;

import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;

public class RoutePartsViewHolder extends RecyclerView.ViewHolder{

    final static String TAG = RoutePartsViewHolder.class.getSimpleName();

    private ImageButton moviePartCoverImage;

    public RoutePartsViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(MoviePart moviePart, int position) {
        moviePartCoverImage = itemView.findViewById(R.id.routepart_cover_button);

        //Set Cover
        Picasso.get()
                .load(moviePart.getMoviepartImagepath())
                .resize(130, 70)
                .placeholder(R.drawable.placeholder_movieparts)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(moviePartCoverImage);

        initBorders();
        initOnClickListener(moviePart);

    }

    private void initBorders() {

    }

    private void initOnClickListener(final MoviePart moviePart) {
        //Set onclick on imagebutton
        moviePartCoverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoplayerActivity.getInstance().goToFrameNumber(moviePart.getFrameNumber());
            }
        });
    }

}
