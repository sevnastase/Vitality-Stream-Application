package com.videostreamtest.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.videostreamtest.config.dao.ServerStatusDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.ServerStatus;
import com.videostreamtest.service.database.DatabaseRestService;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class ServerStatusServiceWorker extends AbstractPraxtourWorker {

    public ServerStatusServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    protected Result doActualWork() {
        final DatabaseRestService databaseRestService = new DatabaseRestService();
        boolean serverOnlineStatus = databaseRestService.isOnline();
        ServerStatusDao serverStatusDao = PraxtourDatabase.getDatabase(getApplicationContext()).serverStatusDao();
        ServerStatus serverStatus = new ServerStatus();
        serverStatus.setServerstatusId(1);
        serverStatus.setServerOnline(serverOnlineStatus);
        serverStatus.setServerLastOnline(new Date(System.currentTimeMillis()));
        serverStatusDao.insert(serverStatus);

//        final Data outputData = new Data.Builder()
//                .putBoolean("server-online-status", serverOnlineStatus)
//                .build();
        return Result.success();
    }
}
