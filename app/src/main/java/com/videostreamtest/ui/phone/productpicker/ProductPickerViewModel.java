package com.videostreamtest.ui.phone.productpicker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.ProductRepository;

import java.util.List;

public class ProductPickerViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ProductRepository productRepository;

    private final LiveData<Configuration> currentConfig;

    public ProductPickerViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        productRepository = new ProductRepository(application);

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

}
