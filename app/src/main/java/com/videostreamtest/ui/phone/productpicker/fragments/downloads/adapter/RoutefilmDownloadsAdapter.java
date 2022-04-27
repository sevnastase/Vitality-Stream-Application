package com.videostreamtest.ui.phone.productpicker.fragments.downloads.adapter;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
            SeekBar itemProgressbar = holder.itemView.findViewById(R.id.routefilm_download_item_progressbar);
            itemProgressbar.setTag(routefilm.getMovieId());
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
                    //Make more efficient
                    //notifyAll keeps rebinding the viewholders setting the requestfocus triggers
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
                if (downloadStatus.getDownloadStatus() == 100) {
                    StandAloneDownloadStatus existingDownloadStatus = getDownloadStatus(downloadStatus.getMovieId());
                    if (existingDownloadStatus!=null) {
                        int downloadStatusIndex = getDownloadStatusPosition(existingDownloadStatus.getMovieId().intValue());
                        if (downloadStatusIndex >=0) {
                            standAloneDownloadStatusList.remove(downloadStatusIndex);
                        }
                    }
                    continue;
                }
                StandAloneDownloadStatus existingDownloadStatus = getDownloadStatus(downloadStatus.getMovieId());
                if (isRoutefilmPresent(downloadStatus.getDownloadMovieId())) {
                    if (existingDownloadStatus == null) {
                        standAloneDownloadStatusList.add(downloadStatus);
                    } else {
                        existingDownloadStatus.setDownloadStatus(downloadStatus.getDownloadStatus());
                    }
                } else {
                    if (existingDownloadStatus != null) {
                        int downloadStatusIndex = getDownloadStatusPosition(existingDownloadStatus.getMovieId().intValue());
                        if (downloadStatusIndex >=0) {
                            standAloneDownloadStatusList.remove(downloadStatusIndex);
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    this.standAloneDownloadStatusList = standAloneDownloadStatusList.stream()
                            .sorted(Comparator.comparingInt(StandAloneDownloadStatus::getDownloadStatus).reversed())
                            .collect(Collectors.toList());
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

    private boolean isRoutefilmPresent(final int routefilmId) {
        if (this.routefilmList.size()>0) {
            for (final Routefilm film: this.routefilmList) {
                if (routefilmId == film.getMovieId().intValue()) {
                    return routefilmId == film.getMovieId().intValue();
                }
            }
            return false;
        } else {
            return false;
        }
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

    private int getDownloadStatusPosition(final int routefilmId) {
        if (this.standAloneDownloadStatusList != null && this.standAloneDownloadStatusList.size()>0) {
            for (int sadsIndex = 0; sadsIndex < this.standAloneDownloadStatusList.size();sadsIndex++) {
                if (standAloneDownloadStatusList.get(sadsIndex).getMovieId().intValue() == routefilmId) {
                    return sadsIndex;
                }
            }
        }
        return -1;
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