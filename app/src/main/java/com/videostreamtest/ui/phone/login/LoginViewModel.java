package com.videostreamtest.ui.phone.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.repository.ConfigurationRepository;

import java.util.List;

public class LoginViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private final LiveData<Configuration> currentConfig;
    private final LiveData<List<Configuration>> allConfigurations;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        currentConfig = configurationRepository.getCurrentConfiguration();
        allConfigurations = configurationRepository.getConfigurations();
    }

    public LiveData<Configuration> getCurrentConfig() {
        return currentConfig;
    }

    public void setCurrentConfig(final String accountToken) {
            if (allConfigurations.getValue() != null && allConfigurations.getValue().size()>0) {
                for (Configuration configuration: allConfigurations.getValue()) {
                    if (accountToken.equals(configuration.getAccountToken())) {
                        configuration.setCurrent(true);
                        update(configuration);
                    }
                }
            }
    }

    public LiveData<List<Configuration>> getAllConfigurations() {
        return allConfigurations;
    }

    public void update(final Configuration configuration) {
        configurationRepository.update(configuration);
    }

    public void insert(final Configuration configuration) {
        configurationRepository.insert(configuration);
    }
}
