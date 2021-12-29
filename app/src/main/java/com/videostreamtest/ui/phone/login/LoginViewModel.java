package com.videostreamtest.ui.phone.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.ServerStatus;
import com.videostreamtest.config.entity.tracker.UsageTracker;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.ServerStatusRepository;
import com.videostreamtest.config.repository.UsageTrackerRepository;

import java.util.List;

public class LoginViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ServerStatusRepository serverStatusRepository;
    private UsageTrackerRepository usageTrackerRepository;

    private final LiveData<Configuration> currentConfig;
    private final LiveData<List<Configuration>> allConfigurations;

    private final LiveData<ServerStatus> serverStatusLiveData;

    private MutableLiveData<String> username = new MutableLiveData<>();
    private MutableLiveData<String> password = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        serverStatusRepository = new ServerStatusRepository(application);
        usageTrackerRepository = new UsageTrackerRepository(application);

        currentConfig = configurationRepository.getCurrentConfiguration();
        allConfigurations = configurationRepository.getConfigurations();
        serverStatusLiveData = serverStatusRepository.getCurrentServerStatus();
    }

    public void setUsername(final String inputUsername) {
        username.setValue(inputUsername);
    }
    public LiveData<String> getUsername() {
        return username;
    }

    public MutableLiveData<String> getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password.setValue(password);
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

    public LiveData<ServerStatus> getServerStatusLiveData() {
        return serverStatusLiveData;
    }

    public void deleteAllKnownConfigurations() {
        configurationRepository.deleteAllConfigurations();
    }

    public void insertUsageTracker(final String accounttoken) {
        final UsageTracker usageTracker = new UsageTracker();
        usageTracker.setAccounttoken(accounttoken);
        usageTrackerRepository.insertNewUsageTrackerInformationObject(usageTracker);
    }
}
