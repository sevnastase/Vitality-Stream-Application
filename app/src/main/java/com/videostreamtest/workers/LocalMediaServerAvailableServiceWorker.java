package com.videostreamtest.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.LogHelper;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class LocalMediaServerAvailableServiceWorker extends Worker {
    private static final String INPUT_ROUTEFILM_JSON_STRING = "INPUT_ROUTEFILM_JSON_STRING";

    private Movie routefilm;
    private String localMediaServerUrl;
    private String apikey;

    public LocalMediaServerAvailableServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String inputDataString = inputData.getString(INPUT_ROUTEFILM_JSON_STRING); // Movie object json
        localMediaServerUrl = inputData.getString("localMediaServer");
        apikey = inputData.getString("apikey");

        LogHelper.WriteLogRule(getApplicationContext(), apikey, routefilm.getMovieTitle()+":LocalMediaCheck Started","DEBUG", "");

        routefilm = new Gson().fromJson(inputDataString, Movie.class);

        Data output = new Data.Builder().putString("INPUT_ROUTEFILM_JSON_STRING",  new GsonBuilder().create().toJson(routefilm, Movie.class)).build();

        if (isLocalServerAvailable(localMediaServerUrl)) {
            String movieUrl = replaceBaseUrlWithLocalMediaServerUrl(routefilm.getMovieUrl());
            String movieMapUrl = replaceBaseUrlWithLocalMediaServerUrl(routefilm.getMovieRouteinfoPath());
            String movieSceneryUrl = replaceBaseUrlWithLocalMediaServerUrl(routefilm.getMovieImagepath());

            routefilm.setMovieUrl(movieUrl);
            routefilm.setMovieRouteinfoPath(movieMapUrl);
            routefilm.setMovieImagepath(movieSceneryUrl);

            output = new Data.Builder()
                    .putString("INPUT_ROUTEFILM_JSON_STRING",  new GsonBuilder().create().toJson(routefilm, Movie.class))
                    .build();
        }
        return Result.success(output);
    }

    private boolean isLocalServerAvailable(final String localMediaServerUrl) {
        try {
            InetAddress ipAddr = InetAddress.getByName(localMediaServerUrl);
            boolean isActive = ipAddr.isReachable(200);
            //You can replace it with your name
            return isActive;
        } catch (Exception e) {
            return false;
        }
    }

    private String replaceBaseUrlWithLocalMediaServerUrl(final String url) {
        try {
            URL movieUrl = new URL(url);
            String file = movieUrl.getFile().substring(movieUrl.getFile().lastIndexOf('/'), movieUrl.getFile().length());
            String path = movieUrl.getFile().substring(0, movieUrl.getFile().lastIndexOf('/'));
            String base = "";
            if (localMediaServerUrl!= null) {
                base = movieUrl.getProtocol() + "://" + localMediaServerUrl + path + "/" + file;
            } else {
                base = movieUrl.getProtocol() + "://" + movieUrl.getHost() + path + "/" + file;
            }
            return base;
        } catch (MalformedURLException e) {
            return url;
        }
    }
}
