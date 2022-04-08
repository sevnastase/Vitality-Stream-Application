package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.dao.ServerStatusDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.ServerStatus;

import java.util.List;

public class ServerStatusRepository {
    private ServerStatusDao serverStatusDao;
    private LiveData<ServerStatus> currentServerStatus;

    public ServerStatusRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        serverStatusDao = praxtourDatabase.serverStatusDao();
        currentServerStatus = serverStatusDao.getServerStatus();
    }

    public LiveData<ServerStatus> getCurrentServerStatus() {
        return currentServerStatus;
    }
}
