package com.videostreamtest.ui.phone.profiles;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Profile;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.ProfileRepository;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ProfileRepository profileRepository;
    private final MutableLiveData<String> profileName = new MutableLiveData<>();
    private final MutableLiveData<String> imgPath = new MutableLiveData<>();

    private final LiveData<Configuration> currentConfig;
    private final LiveData<List<Profile>> accountProfiles;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        profileRepository = new ProfileRepository(application);

        currentConfig = configurationRepository.getCurrentConfiguration();
        //CurrentConfig is loaded after query is done and then assigns the value, so 'currentConfig.getValue().getAccountToken()' is basically null
        accountProfiles = profileRepository.getAccountProfiles();
    }

    public void signoutCurrentAccount(){
        currentConfig.getValue().setCurrent(false);
        updateConfiguration(currentConfig.getValue());
    }

    public void updateConfiguration(Configuration configuration) {
        configurationRepository.saveCurrentConfiguration(configuration);
    }

    public MutableLiveData<String> getProfileName() {
        return profileName;
    }

    public MutableLiveData<String> getImgPath() {
        return imgPath;
    }

    public LiveData<Configuration> getCurrentConfig() {
        return currentConfig;
    }

    public LiveData<List<Profile>> getAccountProfiles() {
        return accountProfiles;
    }
}
