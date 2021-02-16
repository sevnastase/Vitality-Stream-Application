package com.videostreamtest.ui.phone.catalog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;

public class AvailableMediaViewHolder extends RecyclerView.ViewHolder {
    final static String TAG = AvailableMediaViewHolder.class.getSimpleName();

    private ImageView routeInfoImageView;
    private LinearLayout routeInfoTextLayoutBlock;

    private ImageButton movieCoverImage;
//    private TextView movieTitle;
    private TextView movieLength;

    public AvailableMediaViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(Movie movie, int position, ImageView routeInfoImageView, LinearLayout routeInfoTextLayoutBlock) {
        this.routeInfoImageView = routeInfoImageView;
        this.routeInfoTextLayoutBlock =routeInfoTextLayoutBlock;

        movieCoverImage = itemView.findViewById(R.id.routeImageCoverButton);
//        movieTitle = itemView.findViewById(R.id.movieTitle);
        movieLength = itemView.findViewById(R.id.movieLength);

        //Set Title
//        movieTitle.setTextSize(20);
//        movieTitle.setTextColor(Color.WHITE);
//        movieTitle.setText(movie.getMovieTitle());

        movieLength.setTextColor(Color.WHITE);
//        movieLength.setText(movie.getMovieLength()/1000+" km");
        movieLength.setText("");

        //Set Cover
        Picasso.get()
                .load(movie.getMovieImagepath())
                .resize(180, 242)
                .placeholder(R.drawable.cast_album_art_placeholder)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(movieCoverImage);

        //init right size because of border
        selectMedia();
        unselectMedia();

        final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                itemView.setSelected(true);
                if (hasFocus) {
                    selectMedia();
                    setSelectedRouteInfo(movie);
                } else {
                    unselectMedia();
                }
            }
        };

        movieCoverImage.setOnFocusChangeListener(focusChangeListener);

        if (movieCoverImage.isSelected()) {
           selectMedia();
            setSelectedRouteInfo(movie);
        } else {
            unselectMedia();
        }

        //Set onclick on imagebutton
        movieCoverImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Check if ANT+ plugin is installed and available on Android device
                if (AntPlusService.isAntPlusDevicePresent(v.getContext().getApplicationContext()) || ApplicationSettings.DEVELOPER_MODE) {
                    //Write values to params
                    SharedPreferences myPreferences = v.getContext().getSharedPreferences("app",0);
                    SharedPreferences.Editor editor = myPreferences.edit();
                    editor.putString("selectedMovieUrl", movie.getMovieUrl());
                    editor.putString("selectedMovieTitle", movie.getMovieTitle());
                    editor.putInt("selectedMovieId", movie.getId());
                    editor.putFloat("selectedMovieTotalDistance", movie.getMovieLength());
                    editor.commit();

                    movieCoverImage.requestFocus();

                    if (!ApplicationSettings.DEVELOPER_MODE) {
                        //Start AntPlus service to connect with garmin cadence sensor
                        Intent antplusService = new Intent(itemView.getContext().getApplicationContext(), AntPlusService.class);
                        itemView.getContext().startService(antplusService);
                    }

                    final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                    itemView.getContext().startActivity(videoPlayer);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                    builder.setMessage(itemView.getContext().getString(R.string.ant_error_message)).setTitle(itemView.getContext().getString(R.string.ant_error_title));
                    builder.setPositiveButton(itemView.getContext().getString(R.string.positive_button), new DialogInterface.OnClickListener() {
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

    public void setSelectedRouteInfo(final Movie movie) {
        if (routeInfoImageView != null) {
            //Set Route Info
            Picasso.get()
                    .load(movie.getMovieRouteinfoPath())
//                    .resize(150, 150)
                    .fit()
                    .placeholder(R.drawable.placeholder_map)
                    .error(R.drawable.cast_ic_notification_disconnect)
                    .into(routeInfoImageView);
        }
        if(routeInfoTextLayoutBlock != null) {
            TextView title = routeInfoTextLayoutBlock.findViewById(R.id.selected_route_title);
            TextView distance = routeInfoTextLayoutBlock.findViewById(R.id.selected_route_distance);
            title.setText(movie.getMovieTitle());
            float meters = movie.getMovieLength();
            int km = (int) (meters / 1000f);
            int hectometers = (int) ((meters - ( km * 1000f)) / 100f);
            distance.setText(toString().format(itemView.getContext().getString(R.string.catalog_screen_distance), km, hectometers));
            Log.d(TAG, "Test");
        }
    }

    public void selectMedia() {
        final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_blue_border);
        movieCoverImage.setBackground(border);
        movieCoverImage.setAlpha(1.0f);
    }

    public void unselectMedia() {
        movieCoverImage.setBackground(null);
        movieCoverImage.setAlpha(0.7f);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public float convertPxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public float convertDpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
