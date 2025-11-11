package com.videostreamtest.ui.phone.videoplayer.fragments.routeparts;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.helpers.AccountHelper;
import com.videostreamtest.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerExoActivity;
import com.videostreamtest.ui.phone.videoplayer.viewmodel.VideoPlayerViewModel;

public class RoutePartsViewHolder extends RecyclerView.ViewHolder{

    final static String TAG = RoutePartsViewHolder.class.getSimpleName();

    private ImageButton moviePartCoverImage;
    private VideoPlayerViewModel videoPlayerViewModel;
    private LifecycleOwner lifecycleOwner;
    private int distanceOffset;

//    public void setVideoPlayerViewModel(VideoPlayerViewModel videoPlayerViewModel) {
//        this.videoPlayerViewModel = videoPlayerViewModel;
//    }
//
//    public void setLifecycleOwner(LifecycleOwner lifecycleOwner) {
//        this.lifecycleOwner = lifecycleOwner;
//    }
    public RoutePartsViewHolder(@NonNull View itemView, VideoPlayerViewModel videoPlayerViewModel, LifecycleOwner lifecycleOwner) {
        super(itemView);
        this.videoPlayerViewModel = videoPlayerViewModel;
        this.lifecycleOwner = lifecycleOwner;

        videoPlayerViewModel.getDistanceOffset().observe(lifecycleOwner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer offset) {
                distanceOffset = offset;
            }
        });
    }

    public void bind(MoviePart moviePart, boolean isLocalPlay, int position) {
        moviePartCoverImage = itemView.findViewById(R.id.routepart_cover_button);

        if (isTouchScreen()) {
            if (isLocalPlay) {
                if (DownloadHelper.getLocalMediaRoutepart(itemView.getContext(), moviePart).exists()) {
                    //Set routepart cover
                    Picasso.get()
                            .load(DownloadHelper.getLocalMediaRoutepart(itemView.getContext(), moviePart))
                            .resize(180, 120)
                            .placeholder(R.drawable.placeholder_movieparts)
                            .error(R.drawable.placeholder_movieparts)
                            .into(moviePartCoverImage, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(itemView.getContext(), "[ERROR][LOCAL] "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    Toast.makeText(itemView.getContext(), "[Local] routepart image not found: "+DownloadHelper.getLocalMediaRoutepart(itemView.getContext(), moviePart).getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            } else {
                //Set routepart cover
                Picasso.get()
                        .load(moviePart.getMoviepartImagepath())
                        .resize(180, 120)
                        .placeholder(R.drawable.placeholder_movieparts)
                        .error(R.drawable.placeholder_movieparts)
                        .into(moviePartCoverImage, new Callback() {

                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {
                                String errorMessage = "[ERROR][EXTERNAL] " + e.getLocalizedMessage();
                                Toast.makeText(itemView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                                Log.e("Picasso Error test" + errorMessage, errorMessage);
//                                Toast.makeText(itemView.getContext(), "[ERROR][EXTERNAL] "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        } else {
            //PLAIN SCREEN

            //PRAXFILM SPECIFIC
            if (moviePart.getMoviepartImagepath().contains("nummer_")) {
                int partNumber = Integer.parseInt(moviePart.getMoviepartImagepath().replaceAll("\\D+",""));
                switch (partNumber) {
                    case 1:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_1_wit));
                        break;
                    case 2:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_2_wit));
                        break;
                    case 3:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_3_wit));
                        break;
                    case 4:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_4_wit));
                        break;
                    case 5:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_5_wit));
                        break;
                    case 6:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_6_wit));
                        break;
                    default:
                        moviePartCoverImage.setImageDrawable(itemView.getContext().getDrawable(R.drawable.nummer_1_wit));
                }

            } else {
                //PRAXSPIN && PRAXFIT
                if (isLocalPlay) {
                    //Set routepart cover
                    Picasso.get()
                            .load(DownloadHelper.getLocalMediaRoutepart(itemView.getContext(), moviePart))
                            .resize(130, 70)
                            .placeholder(R.drawable.placeholder_movieparts)
                            .error(R.drawable.placeholder_movieparts)
                            .into(moviePartCoverImage);
                } else {
                    //Set routepart cover
                    Picasso.get()
                            .load(moviePart.getMoviepartImagepath())
                            .resize(130, 70)
                            .placeholder(R.drawable.placeholder_movieparts)
                            .error(R.drawable.placeholder_movieparts)
                            .into(moviePartCoverImage);
                }
            }
        }

        initBorders();
        if (isTouchScreen()) {
            initTouchBorders();
        } else {
            initOnFocusChangeListener();
        }
        initOnClickListener(moviePart);
    }

    private void initBorders() {
        drawSelectionBorder();
        undrawSelectionBorder();

        if (moviePartCoverImage.isSelected() ) {
            drawSelectionBorder();
        } else {
            undrawSelectionBorder();
        }
    }

    private void initTouchBorders() {
        drawSelectionBorder();
    }

    public void initOnClickListener(final MoviePart moviePart) {
        videoPlayerViewModel.getSelectedMovie().observe(lifecycleOwner, selectedMovie -> {
            moviePartCoverImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moviePartCoverImage.requestFocus();
                    if (AccountHelper.isLocalPlay(itemView.getContext())) {
                        VideoplayerActivity.getInstance().goToFrameNumber(moviePart.getFrameNumber().intValue());
                    } else {
                        VideoplayerExoActivity.getInstance().goToFrameNumber(moviePart.getFrameNumber().intValue());
                    }
                    LocalBroadcastManager.getInstance(itemView.getContext())
                            .sendBroadcast(new Intent("hideRoutepartsLayout"));
                    Log.d(TAG, "moviePart frame as int = " + moviePart.getFrameNumber().intValue());
                    videoPlayerViewModel.resetDistance(moviePart, selectedMovie);
                }
            });
        });
//        videoPlayerViewModel.getSelectedMovie().observe(lifecycleOwner, selectedMovie -> {
//            if (selectedMovie != null) {
//                videoPlayerViewModel.getMovieTotalDurationSeconds().observe(lifecycleOwner, movieTotalDurationSeconds -> {
//                    if (movieTotalDurationSeconds != null) {
//                        videoPlayerViewModel.getMovieSpendDurationSeconds().observe(lifecycleOwner, movieSpendDurationSeconds -> {
//                            if (movieSpendDurationSeconds != null) {
//                                final float mps = DistanceLookupTable.getMeterPerSecond(selectedMovie.getMovieLength(), movieTotalDurationSeconds / 1000);
//                                int currentMetersDone = (int) (mps * (movieSpendDurationSeconds / 1000)) - distanceOffset;
//                                Log.d(TAG, "currentMetersDone = " + currentMetersDone);
//                                Log.d(TAG, "distanceOffset = " + distanceOffset);
//                                if (currentMetersDone < 0) currentMetersDone = 0;
//                                videoPlayerViewModel.setCurrentMetersDone(currentMetersDone);
//
//                                final int metersToGo = selectedMovie.getMovieLength() - currentMetersDone - distanceOffset;
//                                videoPlayerViewModel.setMetersToGo(metersToGo);
//
//                                //Set onclick on imagebutton
//                                moviePartCoverImage.setOnClickListener(new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
//                                        moviePartCoverImage.requestFocus();
//                                        if (AccountHelper.isLocalPlay(itemView.getContext())) {
//                                            VideoplayerActivity.getInstance().goToFrameNumber(moviePart.getFrameNumber().intValue());
//                                        } else {
//                                            VideoplayerExoActivity.getInstance().goToFrameNumber(moviePart.getFrameNumber().intValue());
//                                        }
//                                        Log.d(TAG, "moviePart frame as int = " + moviePart.getFrameNumber().intValue());
//
//                                        resetDistance(moviePart, selectedMovie, mps);
//                                    }
//                                });
//                            }
//                        });
//                    }
//                });
//            }
//        });
    }

    private void initOnFocusChangeListener() {
        moviePartCoverImage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "Selected MoviePart: "+getAdapterPosition()+" hasFocus: "+hasFocus);
                itemView.setSelected(true);
                if (hasFocus) {
                    drawSelectionBorder();
                } else {
                    undrawSelectionBorder();
                }
            }
        });
    }

    private void drawSelectionBorder() {
        final Drawable border = itemView.getContext().getDrawable(R.drawable.imagebutton_blue_border);
//        moviePartCoverImage.setBackground(border);
        moviePartCoverImage.setAlpha(1.0f);
    }

    private void undrawSelectionBorder() {
        moviePartCoverImage.setBackground(null);
        moviePartCoverImage.setAlpha(0.7f);
    }

    private boolean isTouchScreen() {
        return itemView.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

//    private void resetDistance(MoviePart moviePart, Movie selectedMovie, float mps) {
//        int seekBarPartFrameNumber = moviePart.getFrameNumber().intValue();
//        int seekBarPartDurationSeconds = (1000 * seekBarPartFrameNumber) / selectedMovie.getRecordedFps().intValue();
//        int newDistanceOffset = (int) (mps * (seekBarPartDurationSeconds / 1000));
//
//        videoPlayerViewModel.setDistanceOffset(newDistanceOffset);
//    }

}
