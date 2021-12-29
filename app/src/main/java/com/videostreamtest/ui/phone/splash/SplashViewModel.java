package com.videostreamtest.ui.phone.splash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.tracker.UsageTracker;
import com.videostreamtest.config.repository.BluetoothDefaultDeviceRepository;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.ProductRepository;
import com.videostreamtest.config.repository.ProfileRepository;
import com.videostreamtest.config.repository.UsageTrackerRepository;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.data.model.response.Product;

import java.util.List;

public class SplashViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ProductRepository productRepository;
    private ProfileRepository profileRepository;
    private BluetoothDefaultDeviceRepository bluetoothDefaultDeviceRepository;
    private UsageTrackerRepository usageTrackerRepository;

    private final LiveData<Configuration> currentConfig;
    private final LiveData<List<Configuration>> allConfigurations;

    private final LiveData<List<com.videostreamtest.config.entity.Profile>> accountProfiles;

    private final MutableLiveData<Integer> workerProgress;

    public SplashViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        productRepository = new ProductRepository(application);
        profileRepository = new ProfileRepository(application);
        bluetoothDefaultDeviceRepository = new BluetoothDefaultDeviceRepository(application);
        usageTrackerRepository = new UsageTrackerRepository(application);

        allConfigurations = configurationRepository.getConfigurations();
        currentConfig = configurationRepository.getCurrentConfiguration();

        accountProfiles = profileRepository.getAccountProfiles();

        workerProgress = new MutableLiveData<>();
    }

    public LiveData<List<com.videostreamtest.config.entity.Product>> getAccountProducts(final String accountToken, final boolean isStreamingAccount) {
        return productRepository.getAccountProducts(accountToken, isStreamingAccount);
    }

    public void insertProduct(final com.videostreamtest.config.entity.Product product) {
        productRepository.insert(product);
    }

    public void insertBluetoothDefaultDevice(final BluetoothDefaultDevice bluetoothDefaultDevice) {
        bluetoothDefaultDeviceRepository.insertBluetoothDefaultDevice(bluetoothDefaultDevice);
    }

    public LiveData<List<com.videostreamtest.config.entity.Profile>> getAccountProfiles() {
        return accountProfiles;
    }

    public void insertProfile(final com.videostreamtest.config.entity.Profile profile) {
        profileRepository.insert(profile);
    }

    public void insertAccountProfile(final Profile profile, final String accountToken) {
        final com.videostreamtest.config.entity.Profile dbProfile = new com.videostreamtest.config.entity.Profile();
        dbProfile.setAccountToken(accountToken);
        dbProfile.setProfileId(profile.getProfileId());
        dbProfile.setProfileImgPath(profile.getProfileImgPath());
        dbProfile.setProfileKey(profile.getProfileKey());
        dbProfile.setProfileName(profile.getProfileName());
        dbProfile.setBlocked(profile.getBlocked());

        profileRepository.insert(dbProfile);
    }

    public LiveData<Configuration> getCurrentConfig() {
        return currentConfig;
    }

    LiveData<List<Configuration>> getConfigurations() {
        return allConfigurations;
    }

    public void updateConfiguration(final Configuration configuration){
        configurationRepository.update(configuration);
    }

    public MutableLiveData<Integer> getWorkerProgress() {
        return workerProgress;
    }

    public void setWorkerProgress(final Integer wprogress){
        workerProgress.setValue(wprogress);
    }

    public LiveData<UsageTracker> getUsageTrackers(final String accounttoken) {
        return usageTrackerRepository.getUsageTrackers(accounttoken);
    }

    public void resetUsageTracker(final String accounttoken) {
        final UsageTracker usageTracker = new UsageTracker();
        usageTracker.setAccounttoken(accounttoken);
        usageTracker.setSelectedProduct(0);
        usageTracker.setSelectedMovie(0);
        usageTracker.setSelectedBackgroundSound(0);
        usageTracker.setSelectedProfile(0);
        usageTrackerRepository.insertNewUsageTrackerInformationObject(usageTracker);
    }
}
