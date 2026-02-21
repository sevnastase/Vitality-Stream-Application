package com.videostreamtest.ui.phone.downloads;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.ServerStatus;
import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;
import com.videostreamtest.config.entity.tracker.UsageTracker;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.GeneralDownloadTrackerRepository;
import com.videostreamtest.config.repository.RoutefilmRepository;
import com.videostreamtest.config.repository.ServerStatusRepository;
import com.videostreamtest.config.repository.UsageTrackerRepository;

import java.util.List;

public class DownloadsViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ServerStatusRepository serverStatusRepository;
    private UsageTrackerRepository usageTrackerRepository;
    private GeneralDownloadTrackerRepository generalDownloadTrackerRepository;
    private RoutefilmRepository routefilmRepository;

    private final LiveData<Configuration> currentConfig;
    private final LiveData<List<Configuration>> allConfigurations;

    private final LiveData<ServerStatus> serverStatusLiveData;

    private MutableLiveData<String> username = new MutableLiveData<>();
    private MutableLiveData<String> password = new MutableLiveData<>();
    private MutableLiveData<Integer> installationSteps = new MutableLiveData<>();
    private MutableLiveData<Integer> currentInstallationStep = new MutableLiveData<>();

    public DownloadsViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        serverStatusRepository = new ServerStatusRepository(application);
        usageTrackerRepository = new UsageTrackerRepository(application);
        generalDownloadTrackerRepository = new GeneralDownloadTrackerRepository(application);
        routefilmRepository = new RoutefilmRepository(application);

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

    public MutableLiveData<Integer> getInstallationSteps() {
        return installationSteps;
    }

    public void setInstallationSteps(Integer installationSteps) {
        this.installationSteps.setValue(installationSteps);
    }

    public MutableLiveData<Integer> getCurrentInstallationStep() {
        return currentInstallationStep;
    }

    public void setCurrentInstallationStep(Integer currentInstallationStep) {
        this.currentInstallationStep.setValue( currentInstallationStep);
    }

    public void addInstallationStep() {
        this.currentInstallationStep.setValue(this.currentInstallationStep.getValue()+1);
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
        usageTracker.setSelectedProduct(0);
        usageTracker.setSelectedProfile(0);
        usageTracker.setSelectedMovie(0);
        usageTracker.setSelectedBackgroundSound(0);
        usageTrackerRepository.insertNewUsageTrackerInformationObject(usageTracker);
    }

    public LiveData<GeneralDownloadTracker> getCurrentDownloadTypeInformation(final String downloadType) {
        return generalDownloadTrackerRepository.getSelectedDownloadTypeInformation(downloadType);
    }

    public LiveData<List<Routefilm>> getRoutefilms() {
        final String apikey = getApplication().getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("apikey","");
        return routefilmRepository.getAllRoutefilms(apikey);
    }
}
