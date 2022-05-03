package com.videostreamtest.ui.phone.productpicker.fragments.downloads;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_MEDIA_URL;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.productpicker.fragments.downloads.adapter.RoutefilmDownloadsAdapter;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class DownloadOverviewFragment extends Fragment {
    private static final String TAG = DownloadOverviewFragment.class.getSimpleName();
    private ProductViewModel productViewModel;

    private RoutefilmDownloadsAdapter routefilmDownloadsAdapter;
    private RecyclerView routeDownloadsOverview;

    private HandlerThread thread;
    private Handler speedtestHandler;

    private TextView downloadspeedInformation;
    private TextView errorDownloadInformationTitle;
    private TextView errorDownloadInformationContent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routefilm_downloads_overview, container, false);
        routeDownloadsOverview = view.findViewById(R.id.recyclerview_routefilm_downloads);

        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        downloadspeedInformation = view.findViewById(R.id.general_overlay_download_speed_information_content);
        errorDownloadInformationContent = view.findViewById(R.id.general_overlay_download_information_content);
        errorDownloadInformationTitle = view.findViewById(R.id.general_overlay_download_information_title);

        routeDownloadsOverview.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        routefilmDownloadsAdapter = new RoutefilmDownloadsAdapter();
        routeDownloadsOverview.setAdapter(routefilmDownloadsAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final String apikey = view.getContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");
        performSpeedtest();

        productViewModel.getRoutefilms(apikey).observe(getViewLifecycleOwner(), routefilms -> {
            if (routefilms!=null && routefilmDownloadsAdapter != null) {

                long totalDownloadableMovieFileSizeOnDisk = 0L;
                for (Routefilm routefilm: routefilms) {
                    if (!DownloadHelper.isMoviePresent(view.getContext(), Movie.fromRoutefilm(routefilm))) {
                        totalDownloadableMovieFileSizeOnDisk += routefilm.getMovieFileSize();
                    }
                }
                if (!DownloadHelper.canFileBeCopiedToLargestVolume(view.getContext(), totalDownloadableMovieFileSizeOnDisk)) {
                    errorDownloadInformationTitle.setVisibility(View.VISIBLE);
                    errorDownloadInformationContent.setVisibility(View.VISIBLE);
                    errorDownloadInformationContent.setText(R.string.warning_need_more_disk_capacity);
                }
                routefilmDownloadsAdapter.updateRoutefilmList(routefilms);
                productViewModel.getAllActiveDownloadStatus().observe(getViewLifecycleOwner(), standAloneDownloadStatusList -> {
                    if (standAloneDownloadStatusList != null) {
                        routefilmDownloadsAdapter.updateStandaloneDownloadStatusList(standAloneDownloadStatusList);
                        routeDownloadsOverview.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        });

        productViewModel.getMeasuredConnectionSpeed().observe(getViewLifecycleOwner(), measuredSpeed -> {
            if (measuredSpeed != null) {
                routeDownloadsOverview.getAdapter().notifyDataSetChanged();
                if (measuredSpeed.intValue()>0) {
                    downloadspeedInformation.setText(String.format(getString(R.string.download_speed_ok)));
                } else {
                    downloadspeedInformation.setText(R.string.download_speed_connection_issues);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        speedtestHandler.removeCallbacks(null);
    }

    private void performSpeedtest() {
        //Perform speed check
        thread = new HandlerThread("Speedtest",
                Process.THREAD_PRIORITY_MORE_FAVORABLE);
        thread.start();

        speedtestHandler = new Handler(thread.getLooper());
        Runnable runnableSpeedTest = new Runnable() {
            @Override
            public void run() {
                SpeedTestSocket speedTestSocket = new SpeedTestSocket();

                // add a listener to wait for speedtest completion and progress
                speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                    @Override
                    public void onCompletion(final SpeedTestReport report) {
                        // called when download/upload is finished
                        Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                        Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                        thread.getLooper().prepare();
                        productViewModel.setMeasuredConnectionSpeed(report.getTransferRateBit());
                    }

                    @Override
                    public void onError(SpeedTestError speedTestError, String errorMessage) {
                        // called when a download/upload error occur
                        Log.d(getClass().getSimpleName(), "ERROR :: "+errorMessage);
                        productViewModel.setMeasuredConnectionSpeed(new BigDecimal(-1));
                    }

                    @Override
                    public void onProgress(float percent, SpeedTestReport report) {
                        // called to notify download/upload progress
                        Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                        Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                        Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
                    }
                });
                speedTestSocket.startDownload(PRAXCLOUD_MEDIA_URL+"/1M.iso");
                speedtestHandler.postDelayed(this::run, 5*60*1000);//MINUTES * SECONDS * MILLISECONDS
            }
        };
        speedtestHandler.postDelayed(runnableSpeedTest,0);
    }
}
