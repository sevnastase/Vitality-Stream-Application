package com.videostreamtest.ui.phone.catalog;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.Profile;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.ProfileRepository;
import com.videostreamtest.config.repository.RoutefilmRepository;

import java.util.List;

public class CatalogViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private ProfileRepository profileRepository;
    private RoutefilmRepository routefilmRepository;

    private final LiveData<Configuration> currentConfig;
    private final LiveData<List<Profile>> accountProfiles;
//    private final LiveData<List<Routefilm>> routefilms;

    public CatalogViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        profileRepository = new ProfileRepository(application);
        routefilmRepository = new RoutefilmRepository(application);

        currentConfig = configurationRepository.getCurrentConfiguration();
        accountProfiles = profileRepository.getAccountProfiles();
//        routefilms = routefilmRepository.getAllRoutefilms();
    }

    public LiveData<Configuration> getCurrentConfig() {
        return currentConfig;
    }
    public LiveData<Profile> getProfile(final Integer profileId) {
        return profileRepository.getProfile(profileId);
    }

    public LiveData<List<Routefilm>> getRoutefilms(final String accountToken) {
        return routefilmRepository.getAllRoutefilms(accountToken);
    }

}
