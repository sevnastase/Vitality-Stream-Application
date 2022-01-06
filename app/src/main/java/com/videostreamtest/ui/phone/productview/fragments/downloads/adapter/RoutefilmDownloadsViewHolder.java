package com.videostreamtest.ui.phone.productview.fragments.downloads.adapter;

import android.view.DragEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;

import java.io.File;

public class RoutefilmDownloadsViewHolder extends RecyclerView.ViewHolder{
    private StandAloneDownloadStatus standAloneDownloadStatus;
    private int position = 0;

    public RoutefilmDownloadsViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bindRoutefilm(final Routefilm routefilm, final StandAloneDownloadStatus downloadStatus,
                              final RoutefilmDownloadsAdapter routefilmDownloadsAdapter, final int position) {
        this.standAloneDownloadStatus = downloadStatus;
        this.position = position;

        ImageButton routefilmScenery = itemView.findViewById(R.id.routefilm_download_item_scenery);
        SeekBar downloadProgressBar = itemView.findViewById(R.id.routefilm_download_item_progressbar);
        TextView downloadProgressBarLabel = itemView.findViewById(R.id.routefilm_download_item_progressbar_label);
        TextView downloadProgressBarTitleLabel = itemView.findViewById(R.id.routefilm_download_item_progressbar_title_label);
        downloadProgressBar.setMax(100);
        downloadProgressBar.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return true;
            }
        });

        final LinearLayout layout = itemView.findViewById(R.id.routefilm_viewholder_item);
        if (itemView.isSelected()) {
            layout.getBackground().setTint(itemView.getResources().getColor(R.color.blue_grey_light_overlay, null));
        } else {
            layout.getBackground().setTint(itemView.getResources().getColor(R.color.black_overlay, null));
        }

        routefilmScenery.setOnFocusChangeListener((focusedView, hasFocus)-> {
            if (hasFocus) {
                routefilmDownloadsAdapter.setSelectedDownloadStatus(position);
                layout.getBackground().setTint(itemView.getResources().getColor(R.color.blue_grey_light_overlay, null));
            } else {
                layout.getBackground().setTint(itemView.getResources().getColor(R.color.black_overlay, null));
            }
        });


        DownloadHelper.setLocalMedia(itemView.getContext(), routefilm);

        downloadProgressBarTitleLabel.setText(routefilm.getMovieTitle());

        Picasso.get()
                .load(new File(routefilm.getMovieImagepath()))
                .noPlaceholder()
                .resize(90, 121)
                .error(R.drawable.download_from_cloud_scenery)
                .into(routefilmScenery);

        if (standAloneDownloadStatus != null) {
            String status = "Download is pending.";
            if (standAloneDownloadStatus.getDownloadStatus() == -1) {
                status = "Download is in queue";
            }
            if (standAloneDownloadStatus.getDownloadStatus() == -2) {
                status = "Download impossible, not enough space to download.";
            }
            if (standAloneDownloadStatus.getDownloadStatus() >0) {
                status = "Download Progress: " + standAloneDownloadStatus.getDownloadStatus().intValue() + "%";
                downloadProgressBar.setProgress(standAloneDownloadStatus.getDownloadStatus().intValue());
            }
            downloadProgressBarLabel.setText(status);
        } else {
            downloadProgressBarLabel.setText("No information available.");
        }
    }
}
