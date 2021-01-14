package com.videostreamtest.ui.phone.catalog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

public class AvailableMediaViewHolder extends RecyclerView.ViewHolder {

    private ImageButton movieCoverImage;
    private TextView movieTitle;

    public AvailableMediaViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(Movie movie, int position) {
        movieCoverImage = itemView.findViewById(R.id.routeImageCoverButton);
        movieTitle = itemView.findViewById(R.id.movieTitle);

        //Set Title
        movieTitle.setTextSize(20);
        movieTitle.setTextColor(Color.WHITE);
        movieTitle.setText(movie.getMovieTitle());

        //Set Cover
        Picasso.get()
                .load(movie.getMovieImagepath())
                .resize(225, 302)
                .placeholder(R.drawable.cast_album_art_placeholder)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(movieCoverImage);

        //Set onclick on imagebutton
        movieCoverImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SharedPreferences myPreferences = v.getContext().getSharedPreferences("app",0);
                SharedPreferences.Editor editor = myPreferences.edit();
                editor.putString("selectedMovieUrl", movie.getMovieUrl());
                editor.putString("selectedMovieTitle", movie.getMovieTitle());
                editor.commit();

                final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                itemView.getContext().startActivity(videoPlayer);
            }
        });

    }
}
