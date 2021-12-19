package com.videostreamtest.config.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Configuration;

import java.util.List;

public class ConfigurationRepository {
    private ConfigurationDao configurationDao;
    private LiveData<List<Configuration>> mConfigurations;
    private LiveData<Configuration> currentConfig;

    public ConfigurationRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        configurationDao = praxtourDatabase.configurationDao();
        mConfigurations = configurationDao.getConfigurations();
        currentConfig = configurationDao.getCurrentConfiguration();
    }

    public void signoutCurrentAccount() {
        if (currentConfig.getValue() != null ) {
            currentConfig.getValue().setCurrent(false);
            this.update(currentConfig.getValue());
        }
    }

    public LiveData<Configuration> getCurrentConfiguration() {
        return configurationDao.getCurrentConfiguration();
    }

    public LiveData<List<Configuration>> getConfigurations() {
        return mConfigurations;
    }

    public void deleteAllConfigurations(){
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            configurationDao.nukeTable();
        });
    }

    public boolean isLocalAccountValid() {
        Configuration currentConfig = this.currentConfig.getValue();
        if (currentConfig == null) {
            return false;
        }
        String accountToken = currentConfig.getAccountToken();
        return ( accountToken == null || !accountToken.isEmpty() || accountToken.equalsIgnoreCase("unauthorized"));
    }

    public boolean saveCurrentConfiguration(final Configuration adjustedConfiguration) {
        this.update(adjustedConfiguration);
        return true;
    }

    public void insert(Configuration newConfiguration) {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            configurationDao.insert(newConfiguration);
        });
    }

    public void update(Configuration updateConfiguration) {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            configurationDao.update(updateConfiguration);
        });
    }

}
