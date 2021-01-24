package com.videostreamtest.ui.phone.catalog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

public class AvailableMediaViewHolder extends RecyclerView.ViewHolder {

    private ImageButton movieCoverImage;
    private TextView movieTitle;
    private TextView movieLength;

    public AvailableMediaViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(Movie movie, int position) {
        movieCoverImage = itemView.findViewById(R.id.routeImageCoverButton);
        movieTitle = itemView.findViewById(R.id.movieTitle);
        movieLength = itemView.findViewById(R.id.movieLength);

        //Set Title
        movieTitle.setTextSize(20);
        movieTitle.setTextColor(Color.WHITE);
        movieTitle.setText(movie.getMovieTitle());

        movieLength.setTextColor(Color.WHITE);
        movieLength.setText(movie.getMovieLength()/1000+" km");

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
                //Check if ANT+ plugin is installed and available on Android device
                if (AntPlusService.isAntPlusDevicePresent(v.getContext().getApplicationContext())) {
                    //Write values to params
                    SharedPreferences myPreferences = v.getContext().getSharedPreferences("app",0);
                    SharedPreferences.Editor editor = myPreferences.edit();
                    editor.putString("selectedMovieUrl", movie.getMovieUrl());
                    editor.putString("selectedMovieTitle", movie.getMovieTitle());
                    editor.commit();

                    //Start AntPlus service to connect with garmin cadence sensor
                    Intent antplusService = new Intent(itemView.getContext().getApplicationContext(), AntPlusService.class);
                    itemView.getContext().startService(antplusService);

                    AlertDialog startPaddlingMessage = new AlertDialog.Builder(itemView.getContext()).create();
                    startPaddlingMessage.setMessage("Please start paddling slowly for the sensor to connect.");
                    startPaddlingMessage.setTitle("Please start slowly");
                    startPaddlingMessage.show();

                    Runnable dismissStartPaddlingMessage = new Runnable() {
                        public void run() {
                            if (startPaddlingMessage != null)
                                startPaddlingMessage.dismiss();
                                //Start route
                                final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                                itemView.getContext().startActivity(videoPlayer);
                        }
                    };
                    new Handler(Looper.getMainLooper()).postDelayed( dismissStartPaddlingMessage, 8000 );

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                    builder.setMessage("Ant+ device not found!").setTitle("Ant+ plugin error");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                }
            }
        });

    }
}
