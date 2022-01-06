package com.videostreamtest.ui.phone.productview.fragments.downloads.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;

import java.util.ArrayList;
import java.util.List;

public class RoutefilmDownloadsAdapter extends RecyclerView.Adapter<RoutefilmDownloadsViewHolder> {

    private List<StandAloneDownloadStatus> standAloneDownloadStatusList = new ArrayList<>();
    private List<Routefilm> routefilmList = new ArrayList<>();
    private int selectedDownloadStatus = 0;

    @NonNull
    @Override
    public RoutefilmDownloadsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.viewholder_routefilm_download_item, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new RoutefilmDownloadsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutefilmDownloadsViewHolder holder, int position) {
        holder.itemView.setSelected(selectedDownloadStatus == position);

        if (standAloneDownloadStatusList != null && standAloneDownloadStatusList.size() > 0 ) {
            final Routefilm routefilm = getRoutefilm(standAloneDownloadStatusList.get(position).getMovieId().intValue());
            holder.bindRoutefilm(routefilm, standAloneDownloadStatusList.get(position), this, position);
        }
        if (selectedDownloadStatus == position) {
            final ImageButton routeSceneryImage = holder.itemView.findViewById(R.id.routefilm_download_item_scenery);
            routeSceneryImage.setFocusableInTouchMode(true);
            routeSceneryImage.setFocusable(true);
            routeSceneryImage.requestFocus();
        }
    }

    @Override
    public int getItemCount() {
        if (standAloneDownloadStatusList== null) {
            return 0;
        } else {
            return standAloneDownloadStatusList.size();
        }
    }

    public void setSelectedDownloadStatus(int position) {
        this.selectedDownloadStatus = position;
    }

    public void updateRoutefilmList(final List<Routefilm> requestedRoutefilmList) {
        if (requestedRoutefilmList != null && requestedRoutefilmList.size()>0) {
            for (final Routefilm routefilm: requestedRoutefilmList) {
                if (!isRoutefilmPresent(routefilm)) {
                    this.routefilmList.add(routefilm);
                    notifyDataSetChanged();
                }
            }
            for (final Routefilm routefilm: this.routefilmList) {
                if (isRoutefilmRemoved(routefilm, requestedRoutefilmList)) {
                    final int removedFilmPosition = this.routefilmList.indexOf(routefilm);
                    this.routefilmList.remove(routefilm);
                    notifyItemRemoved(removedFilmPosition);
                }
            }
        }
    }

    public void updateStandaloneDownloadStatusList(final List<StandAloneDownloadStatus> newDownloadStatusList) {
        if (newDownloadStatusList!=null && newDownloadStatusList.size()>0) {
            for (final StandAloneDownloadStatus downloadStatus: newDownloadStatusList) {
                StandAloneDownloadStatus existingDownloadStatus = getDownloadStatus(downloadStatus.getMovieId());
                if (existingDownloadStatus == null ) {
                    standAloneDownloadStatusList.add(downloadStatus);
                } else {
                    existingDownloadStatus.setDownloadStatus(downloadStatus.getDownloadStatus());
                }
                notifyDataSetChanged();
            }
        }
    }

    private boolean isRoutefilmRemoved(final Routefilm routefilm, final List<Routefilm> requestedRoutefilmList) {
        boolean isRemoved = true;
        if (requestedRoutefilmList!= null && requestedRoutefilmList.size()>0) {
            for (final Routefilm film: requestedRoutefilmList) {
                if (routefilm.getMovieId().intValue() == film.getMovieId().intValue()) {
                    isRemoved = false;
                }
            }
        }
        return isRemoved;
    }

    private boolean isRoutefilmPresent(final Routefilm routefilm) {
        if (this.routefilmList.size()>0) {
            for (final Routefilm film: this.routefilmList) {
                if (routefilm.getMovieId().intValue() == film.getMovieId().intValue()) {
                    return routefilm.getMovieId().intValue() == film.getMovieId().intValue();
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private StandAloneDownloadStatus getDownloadStatus(final int routefilmId) {
        if (this.standAloneDownloadStatusList != null && this.standAloneDownloadStatusList.size()>0) {
            for (final StandAloneDownloadStatus downloadStatus: this.standAloneDownloadStatusList) {
                if (routefilmId == downloadStatus.getMovieId().intValue()) {
                    return downloadStatus;
                }
            }
        }
        return null;
    }

    private Routefilm getRoutefilm(final int routefilmId) {
        if (this.routefilmList != null && this.routefilmList.size()>0) {
            for (final Routefilm routefilm: this.routefilmList) {
                if (routefilmId == routefilm.getMovieId().intValue()) {
                    return routefilm;
                }
            }
        }
        return null;
    }
}
