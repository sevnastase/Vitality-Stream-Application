package com.videostreamtest.service.database;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.data.ResultApiKey;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DatabaseRestService {
    public static final String TAG = "DatabaseRestService";
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private String url = ApplicationSettings.PRAXCLOUD_API_URL +"/api";

    private String accountKey;
    private String profileKey;

    public DatabaseRestService() {
        client = new OkHttpClient();
    }

    public String loginUser(final String username, final String password) {
        final String loginUrl = url + "/login";
        final String json = "{\n" +
                "    \"username\" : \""+username+"\",\n" +
                "    \"password\" : \""+password+"\"\n" +
                "}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(loginUrl)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final ResultApiKey resultApiKey = objectMapper.readValue(response.body().string(), ResultApiKey.class);
            return resultApiKey.getApiKey();
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getLocalizedMessage());
            return "Failed";
        }
    }

    public String getProfiles(final String apikey) {
        final String profilesUrl = url + "/users/current/profiles";
        final Request request = new Request.Builder()
                .url(profilesUrl)
                .addHeader("api-key", apikey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException exception) {
            return "failed";
        }
    }

    public String getAvailableMovies(final String apikey) {
        final String routeMoviesUrl = url+"/route/movies";
        final Request request = new Request.Builder()
                .url(routeMoviesUrl)
                .addHeader("api-key", apikey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException exception) {
            return "failed";
        }
    }

    public String getAvailableMovieParts(final String apikey, final Integer movieId) {
        final String routeMoviesUrl = url+"/route/movieparts/"+movieId;
        final Request request = new Request.Builder()
                .url(routeMoviesUrl)
                .addHeader("api-key", apikey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException exception) {
            return "failed";
        }
    }

    public String writeLog(final String apikey, final String logRule, final String logType, final String profileName) {
        final String writeLogUrl = url+"/log/add";
        final String json = "{\n" +
                "    \"profileName\" : \""+profileName+"\",\n" +
                "    \"logRule\" : \""+logRule+"\",\n" +
                "    \"logType\" : \""+logType+"\"\n" +
                "}";
        RequestBody body = RequestBody.create(json, JSON);
        final Request request = new Request.Builder()
                .url(writeLogUrl)
                .addHeader("api-key", apikey)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException exception) {
            return "failed";
        }
    }

    public String addProfile(final String apikey, final String profileName, final String imgPath) {
        final String writeLogUrl = url+"/users/current/profile/add";
        final String json = "{\n" +
                "    \"profilename\" : \""+profileName+"\",\n" +
                "    \"imgpath\" : \""+imgPath+"\"\n" +
                "}";
        RequestBody body = RequestBody.create(json, JSON);
        final Request request = new Request.Builder()
                .url(writeLogUrl)
                .addHeader("api-key", apikey)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException exception) {
            return "failed";
        }
    }

    public String getCustomerProducts(final String apikey) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        final String getActiveProductsUrl = url+"/users/current/subscriptions";
        final Request request = new Request.Builder()
                .url(getActiveProductsUrl)
                .addHeader("api-key", apikey)
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            Log.d("DatabaseService","Response got filled in :: "+body);
            return body;
        } catch (IOException exception) {
            return "failed";
        }
    }

    public String downloadMovie(final String apiKey, final Integer movieId) {
        return "failed";
    }

    public boolean isOnline() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        final String getActiveProductsUrl = url+"/server/status";
        final Request request = new Request.Builder()
                .url(getActiveProductsUrl)
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            Log.d("DatabaseService","Response got filled in :: "+body);
            return (!body.isEmpty() && body.contains("streamserver"));
        } catch (IOException exception) {
            Log.e(TAG, exception.getLocalizedMessage());
            return false;
        }
    }

}
