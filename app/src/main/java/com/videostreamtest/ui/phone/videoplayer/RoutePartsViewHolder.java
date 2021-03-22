package com.videostreamtest.ui.phone.videoplayer;

import android.graphics.drawable.Drawable;
import android.util.Log;
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
                .error(R.drawable.placeholder_movieparts)
                .into(moviePartCoverImage);

        initBorders();
        initOnFocusChangeListener();
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

}
