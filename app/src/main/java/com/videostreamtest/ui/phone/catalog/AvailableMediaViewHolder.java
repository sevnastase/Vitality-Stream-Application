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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.ant.AntPlusService;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.DownloadMovieServiceWorker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class AvailableMediaViewHolder extends RecyclerView.ViewHolder {
    final static String TAG = AvailableMediaViewHolder.class.getSimpleName();

    private CatalogRecyclerViewClickListener catalogRecyclerViewClickListener;
    private boolean localPlay;

    private ImageView routeInfoImageView;
    private LinearLayout routeInfoTextLayoutBlock;

    private ImageButton movieCoverImage;
    private TextView movieDescription;
    private int downloadProgress = 0;

    public AvailableMediaViewHolder(@NonNull View itemView, CatalogRecyclerViewClickListener catalogRecyclerViewClickListener, boolean localPlay) {
        super(itemView);
        this.catalogRecyclerViewClickListener = catalogRecyclerViewClickListener;
        this.localPlay = localPlay;
    }

    public void bind(Routefilm routefilm, int position, ImageView routeInfoImageView, LinearLayout routeInfoTextLayoutBlock, double progress, int movieId) {
        Movie movieDto = new Movie();
        movieDto.setId(routefilm.getMovieId());
        movieDto.setMinimalSpeed(routefilm.getMinimalSpeed());
        movieDto.setMovieImagepath(routefilm.getMovieImagepath());
        movieDto.setMovieLength(routefilm.getMovieLength());
        movieDto.setMovieImagepath(routefilm.getMovieImagepath());
        movieDto.setMovieRouteinfoPath(routefilm.getMovieRouteinfoPath());
        movieDto.setMovieTitle(routefilm.getMovieTitle());
        movieDto.setMovieUrl(routefilm.getMovieUrl());
        movieDto.setRecordedFps(routefilm.getRecordedFps());
        movieDto.setRecordedSpeed(routefilm.getRecordedSpeed());
        movieDto.setMovieFileSize(routefilm.getMovieFileSize());
        movieDto.setMapFileSize(routefilm.getMapFileSize());
        movieDto.setSceneryFileSize(routefilm.getSceneryFileSize());

        bind(movieDto, position, routeInfoImageView, routeInfoTextLayoutBlock, progress, movieId);
    }

    public void bind(Movie movie, int position, ImageView routeInfoImageView, LinearLayout routeInfoTextLayoutBlock, double progress, int movieId) {
        this.routeInfoImageView = routeInfoImageView;
        this.routeInfoTextLayoutBlock = routeInfoTextLayoutBlock;

        movieCoverImage = itemView.findViewById(R.id.routeImageCoverButton);
        movieDescription = itemView.findViewById(R.id.movie_description);

        movieDescription.setTextColor(Color.WHITE);
        movieDescription.setText("");

        if (localPlay) {
            Log.d(TAG, "LocalPlay activated");

            if (isMovieOnDevice(movie)) {
                setLocalMedia(movie);
                Log.d(TAG, "local media provided");
                //Set Cover
                Picasso.get()
                        .load(new File(movie.getMovieImagepath()))
                        .resize(180, 242)
                        .placeholder(R.drawable.cast_album_art_placeholder)
                        .error(R.drawable.cast_ic_notification_disconnect)
                        .into(movieCoverImage);

//                setStartOnClick(movieCoverImage);
            } else {
                //Set Cover
                Picasso.get()
                        .load(movie.getMovieImagepath())
                        .resize(180, 242)
                        .placeholder(R.drawable.cast_album_art_placeholder)
                        .error(R.drawable.cast_ic_notification_disconnect)
                        .into(movieCoverImage);
//                setDownloadMediaOnClick(movieCoverImage);
            }
        } else {
            //Set Cover
            Picasso.get()
                    .load(movie.getMovieImagepath())
                    .resize(180, 242)
                    .placeholder(R.drawable.cast_album_art_placeholder)
                    .error(R.drawable.cast_ic_notification_disconnect)
                    .into(movieCoverImage);
        }

        //init right size because of border
        selectMedia();
        unselectMedia();

        final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                itemView.setSelected(true);
                if (hasFocus) {
                    selectMedia();
                    setSelectedRouteInfo(movie, progress, movieId);
                    if(catalogRecyclerViewClickListener != null) {
                        catalogRecyclerViewClickListener.recyclerViewListClicked(itemView, position);
                    }
                } else {
                    unselectMedia();
                }
            }
        };

        movieCoverImage.setOnFocusChangeListener(focusChangeListener);

        Log.d(TAG, "itemView Selected: "+itemView.isSelected() + " Position: "+position+ " movieCoverImage Focus: "+movieCoverImage.isFocused() );
        if (itemView.isSelected()) {
           selectMedia();
           setSelectedRouteInfo(movie, progress, movieId);
        } else {
            unselectMedia();
        }

        final View.OnClickListener downloadOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(movie.getId() == movieId) {
                    Toast.makeText(v.getContext().getApplicationContext(), v.getContext().getString(R.string.catalog_already_downloading_message), Toast.LENGTH_LONG).show();
                    return;
                }
                //Build Constraint
                Constraints constraint = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                //Test Download media worker
                Data.Builder mediaDownloader = new Data.Builder();
                mediaDownloader.putString("INPUT_ROUTEFILM_JSON_STRING",  new GsonBuilder().create().toJson(movie, Movie.class));
                OneTimeWorkRequest downloadMediaRequest = new OneTimeWorkRequest.Builder(DownloadMovieServiceWorker.class)
                        .setConstraints(constraint)
                        .setInputData(mediaDownloader.build())
                        .addTag("media-downloader")
                        .build();

                //Start downloading
                WorkManager.getInstance(v.getContext().getApplicationContext())
                        .enqueueUniqueWork("movie-"+movie.getId(), ExistingWorkPolicy.KEEP, downloadMediaRequest);
            }
        };

        final View.OnClickListener startOnClick = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //Write values to params to pass to the videoPlayerActivity
                SharedPreferences myPreferences = view.getContext().getSharedPreferences("app",0);
                SharedPreferences.Editor editor = myPreferences.edit();
                if (localPlay) {
                    Log.d(TAG, "local url: " + movie.getMovieUrl());
                    Log.d(TAG, "local scenery url: " + movie.getMovieImagepath());
                    Log.d(TAG, "local map url: " + movie.getMovieRouteinfoPath());
                }
                editor.putString("selectedMovieUrl", movie.getMovieUrl());
                editor.putString("selectedMovieTitle", movie.getMovieTitle());
                editor.putString("selectedMovieObject", new GsonBuilder().create().toJson(movie, Movie.class));
                editor.putFloat("selectedMovieTotalDistance", movie.getMovieLength());
                editor.commit();

                Log.d(TAG, new GsonBuilder().create().toJson(movie, Movie.class));

                movieCoverImage.requestFocus();

                boolean abortEarly = false;
                if (!ApplicationSettings.DEVELOPER_MODE) {
                    switch (ApplicationSettings.SELECTED_COMMUNICATION_DEVICE) {
                        case ANT_PLUS:
                            //Check if ANT+ plugin is installed and available on Android device
                            if (!AntPlusService.isAntPlusDevicePresent(view.getContext().getApplicationContext())) {
                                abortEarly = true;
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
                            break;
                        case BLE:
                            //No check implemented if sensor available
                        default:
                    }
                }

                if (!abortEarly) {
                    final Intent videoPlayer = new Intent(itemView.getContext(), VideoplayerActivity.class);
                    itemView.getContext().startActivity(videoPlayer);
                }
            }
        };

        //Set onclick on imagebutton
//        movieCoverImage.setOnClickListener(startOnClick);
        if (localPlay) {
            if (isMovieOnDevice(movie)) {
                movieCoverImage.setOnClickListener(startOnClick);
            }else {
                movieCoverImage.setOnClickListener(downloadOnClick);
            }
        } else {
            movieCoverImage.setOnClickListener(startOnClick);
        }
    }

    public void setSelectedRouteInfo(final Movie movie, final double downloadProgress, final int movieId) {
        if (routeInfoImageView != null) {

            if (localPlay) {
                if (isMovieOnDevice(movie)) {
                    Log.d(TAG, "selected file >><<>><< "+movie.getMovieRouteinfoPath());
                    Log.d(TAG, "selected file exists >><<>><< "+ new File(movie.getMovieRouteinfoPath()).exists());
                    Log.d(TAG, "selected file size >><<>><< "+ new File(movie.getMovieRouteinfoPath()).length());
                    Log.d(TAG, "estimated file size >><<>><< "+ movie.getMapFileSize());

                    //Set Route Info
                    Picasso.get()
                            .load(new File(movie.getMovieRouteinfoPath()))
                            .fit()
                            .placeholder(R.drawable.placeholder_map)
                            .error(R.drawable.placeholder_map)
                            .into(routeInfoImageView);
                } else {
                    //Set Route Info
                    Picasso.get()
                            .load(R.drawable.download_from_cloud_scenery)
                            .fit()
                            .placeholder(R.drawable.placeholder_map)
                            .error(R.drawable.placeholder_map)
                            .into(routeInfoImageView);
                }
            } else {
                //Set Route Info
                Picasso.get()
                        .load(movie.getMovieRouteinfoPath())
                        .fit()
                        .placeholder(R.drawable.placeholder_map)
                        .error(R.drawable.placeholder_map)
                        .into(routeInfoImageView);
            }
        }
        if(routeInfoTextLayoutBlock != null) {
            TextView title = routeInfoTextLayoutBlock.findViewById(R.id.selected_route_title);
            TextView distance = routeInfoTextLayoutBlock.findViewById(R.id.selected_route_distance);

            title.setText(toString().format(itemView.getContext().getString(R.string.catalog_selected_route_title), movie.getMovieTitle()));

            float meters = movie.getMovieLength();
            int km = (int) (meters / 1000f);
            int hectometers = (int) ((meters - (km * 1000f)) / 100f);
            distance.setText(toString().format(itemView.getContext().getString(R.string.catalog_screen_distance), km, hectometers));

            title.setVisibility(View.VISIBLE);

            if(localPlay) {
                if (movie.getId() == movieId) {
                    distance.setText(toString().format(itemView.getContext().getString(R.string.catalog_download_status_message), downloadProgress));
                }
            }
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

    /**
     * Check if movie folder with content is located on any local storage device
     * @param movie
     * @return boolean
     */
    private boolean isMovieOnDevice(final Movie movie) {
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(itemView.getContext().getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                long totalSizeOnDisk = 0;

                for (File file: possibleMovieLocation.listFiles()) {
                    totalSizeOnDisk += file.length();
                }

                long totalEstimatedSize = movie.getMapFileSize()+movie.getSceneryFileSize()+movie.getMovieFileSize();

                if (totalSizeOnDisk >= totalEstimatedSize) {
                    Log.d(TAG, "Movie is availble for use!");
                    return true;
                }

            }
        }
        Log.d(TAG, "Movie is not (YET) availble for use !");
        return false;
    }

    /**
     * Adjust object url's to local storage paths
     * @param movie
     */
    private void setLocalMedia(final Movie movie) {
        String movieFileName = "";
        String sceneryFileName = "";
        String mapFilename = "";
        try {
            URL movieUrl = new URL(movie.getMovieUrl());
            movieFileName = new File(movieUrl.getFile()).getName();
            URL sceneryUrl = new URL(movie.getMovieImagepath());
            sceneryFileName = new File(sceneryUrl.getFile()).getName();
            URL mapUrl = new URL(movie.getMovieRouteinfoPath());
            mapFilename = new File(mapUrl.getFile()).getName();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return;
        }
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(itemView.getContext().getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                movie.setMovieUrl(pathname+"/"+movieFileName);
                movie.setMovieImagepath(pathname+"/"+sceneryFileName);
                movie.setMovieRouteinfoPath(pathname+"/"+mapFilename);
            }
        }

    }
}
