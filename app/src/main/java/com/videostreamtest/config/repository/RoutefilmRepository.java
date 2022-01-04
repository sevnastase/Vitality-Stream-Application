package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.Routefilm;

import java.util.List;

public class RoutefilmRepository {
    private RoutefilmDao routefilmDao;
//    private LiveData<List<Routefilm>> allRoutefilms;

    public RoutefilmRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        routefilmDao = praxtourDatabase.routefilmDao();
    }

    public LiveData<List<Routefilm>> getAllRoutefilms(final String accountToken) {
        return routefilmDao.getRoutefilms(accountToken);
    }

    public LiveData<List<Routefilm>> getAllProductRoutefilms(final String accountToken) {
        return routefilmDao.getSelectedProductRoutefilms(accountToken);
    }

    public LiveData<Routefilm> getSelectedRoutefilm() {
        return routefilmDao.getSelectedRoutefilm();
    }
}
