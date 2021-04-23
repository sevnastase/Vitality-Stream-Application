package com.videostreamtest.ui.phone.productview.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.DownloadStatusRepository;
import com.videostreamtest.config.repository.RoutefilmRepository;

import java.util.List;

public class ProductViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private RoutefilmRepository routefilmRepository;
    private DownloadStatusRepository downloadStatusRepository;

    public ProductViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        routefilmRepository = new RoutefilmRepository(application);
        downloadStatusRepository = new DownloadStatusRepository(application);
    }

    public LiveData<Configuration> getCurrentConfig() {
        return configurationRepository.getCurrentConfiguration();
    }

    public LiveData<List<Routefilm>> getRoutefilms(final String accountToken) {
        return routefilmRepository.getAllRoutefilms(accountToken);
    }

    public void signoutCurrentAccount(Configuration configuration){
        configuration.setCurrent(false);
        configurationRepository.saveCurrentConfiguration(configuration);
    }

    public LiveData<StandAloneDownloadStatus> getDownloadStatus(final Integer movieId) {
        return downloadStatusRepository.getDownloadStatus(movieId);
    }
}
