package com.videostreamtest.ui.phone.productpicker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.DownloadStatusRepository;
import com.videostreamtest.config.repository.ProductRepository;
import com.videostreamtest.config.repository.RoutefilmRepository;
import com.videostreamtest.config.repository.UsageTrackerRepository;

import java.util.List;

public class ProductPickerViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ProductRepository productRepository;
    private UsageTrackerRepository usageTrackerRepository;
    private RoutefilmRepository routefilmRepository;
    private DownloadStatusRepository downloadStatusRepository;

    private final LiveData<Configuration> currentConfig;

    public ProductPickerViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        productRepository = new ProductRepository(application);
        routefilmRepository = new RoutefilmRepository(application);
        usageTrackerRepository = new UsageTrackerRepository(application);
        downloadStatusRepository = new DownloadStatusRepository(application);

        currentConfig = configurationRepository.getCurrentConfiguration();
    }

    public LiveData<Configuration> getCurrentConfig() {
        return currentConfig;
    }

    public LiveData<List<Product>> getAccountProducts(final String accountToken, final boolean isStreamAccount) {
        return productRepository.getAccountProducts(accountToken, isStreamAccount);
    }

    public void signoutCurrentAccount(){
        currentConfig.getValue().setCurrent(false);
        configurationRepository.update(currentConfig.getValue());
    }

    public void updateConfiguration(Configuration configuration) {
        configurationRepository.update(configuration);
    }

    public void setSelectedProductId(final Integer productId) {
        usageTrackerRepository.setSelectedProduct(productId);
    }

    public LiveData<List<Routefilm>> getRoutefilms(final String accountToken) {
        return routefilmRepository.getAllRoutefilms(accountToken);
    }

    public LiveData<List<StandAloneDownloadStatus>> getDownloads() {
        return downloadStatusRepository.getAllActiveDownloadStatus();
    }

}
