package com.videostreamtest.service.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.data.ResultApiKey;

import java.io.IOException;

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
    private String url ="http://188.166.100.139:8080/api";

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

}
