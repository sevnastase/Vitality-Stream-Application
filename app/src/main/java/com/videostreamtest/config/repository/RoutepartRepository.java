package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.RoutepartDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routepart;

import java.util.List;


public class RoutepartRepository {
    private RoutepartDao routepartDao;

    public RoutepartRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        this.routepartDao = praxtourDatabase.routepartDao();
    }

    public LiveData<List<Routepart>> getRoutePartsOfMovieWithId(final Integer movieId) {
        return routepartDao.getRoutepartsOfMovieId(movieId);
    }
}
