package com.videostreamtest.ui.phone.videoplayer.fragments.routeparts;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;

import java.io.File;

public class RoutePartsViewHolder extends RecyclerView.ViewHolder{

    final static String TAG = RoutePartsViewHolder.class.getSimpleName();

    private ImageButton moviePartCoverImage;

    public RoutePartsViewHolder(@NonNull View itemView) {
        super(itemView);
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
                                Toast.makeText(itemView.getContext(), "[ERROR][EXTERNAL] "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        } else {
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

    private void initOnClickListener(final MoviePart moviePart) {
        //Set onclick on imagebutton
        moviePartCoverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moviePartCoverImage.requestFocus();
                VideoplayerActivity.getInstance().goToFrameNumber(moviePart.getFrameNumber());
            }
        });
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
        moviePartCoverImage.setBackground(border);
        moviePartCoverImage.setAlpha(1.0f);
    }

    private void undrawSelectionBorder() {
        moviePartCoverImage.setBackground(null);
        moviePartCoverImage.setAlpha(0.7f);
    }

    private boolean isTouchScreen() {
        return itemView.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

}
