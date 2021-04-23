package com.videostreamtest.ui.phone.catalog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Profile;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.workers.AvailableMediaServiceWorker;

import java.io.File;

public class CatalogActivity extends AppCompatActivity implements CatalogRecyclerViewClickListener {
    private final static String TAG = CatalogActivity.class.getSimpleName();
    private CatalogViewModel catalogViewModel;
    private TextView profileNameTextView;
    private RecyclerView availableMediaRecyclerView;
    private GridLayoutManager gridLayoutManager;

    private AvailableMediaAdapter availableMediaAdapter;

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);

        final String profileName = getIntent().getStringExtra("profileName");
        final Integer profileId = getIntent().getIntExtra("profileId", 0);
        final String profileKey = getIntent().getStringExtra("profileKey");

        final ImageView countryFlagView = findViewById(R.id.selected_route_country);
        Picasso.get()
                .load("http://188.166.100.139:8080/api/dist/img/flags/NL.jpg")
                .fit()
                .placeholder(R.drawable.flag_placeholder)
                .error(R.drawable.flag_placeholder)
                .into(countryFlagView);

        profileNameTextView = findViewById(R.id.current_loaded_profile);
        profileNameTextView.setTextSize(20);
        profileNameTextView.setTextColor(Color.WHITE);
        profileNameTextView.setText(profileName);

        availableMediaRecyclerView = findViewById(R.id.recyclerview_available_media);
        availableMediaRecyclerView.setHasFixedSize(true);

        //Maak lineaire layoutmanager en zet deze op horizontaal
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        gridLayoutManager = new GridLayoutManager(this,5);
        //Zet de layoutmanager erin
        availableMediaRecyclerView.setLayoutManager(gridLayoutManager);

        ViewTreeObserver viewTreeObserver = availableMediaRecyclerView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width  = gridLayoutManager.getWidth();
                int height = gridLayoutManager.getHeight();

                final LinearLayout routeInfoChart = findViewById(R.id.overlay_route_information);
                routeInfoChart.setMinimumWidth(width);
            }
        });

        catalogViewModel.getCurrentConfig().observe(this, currentConfiguration -> {
            if (currentConfiguration!= null) {
                Log.d(TAG, "ApiKey found! ");
                Log.d(TAG, "LocalPlay is set to --> "+currentConfiguration.isLocalPlay());

                //Observe profiles
                catalogViewModel.getProfile(profileId).observe(this, profile ->{
                    if (profile != null) {
                        Log.d(TAG, "Name: "+profile.getProfileName() + " Selected: "+profile.isSelected());
                    }
                });

                WorkManager.getInstance(this)
                    .getWorkInfosByTagLiveData("media-downloader")
                    .observe(this, workInfos -> {
                        Log.d(TAG, "WORKERINFOS FOUND :: "+workInfos.size());
                        if (workInfos.size()>0) {
                            for (WorkInfo workInfo: workInfos) {
                                Log.d(TAG, "Progress: "+workInfo.getProgress().getDouble("progress",0.0));
                                Log.d(TAG, "Workinfo :: "+workInfo.getTags().iterator().next()+" >> runAttemptCount :: "+workInfo.getRunAttemptCount() + " >> State :: "+workInfo.getState().name());
                                if (availableMediaAdapter != null && availableMediaRecyclerView != null) {
                                    availableMediaAdapter.updateDownloadProgress(workInfo.getProgress().getDouble("progress", 0.0), workInfo.getProgress().getInt("movie-id", 0));
                                    availableMediaRecyclerView.getAdapter().notifyDataSetChanged();
                                }
                            }
                        } else {
                            Log.d(TAG, "No workerInfos are found for downloading media :: << ");
                        }
                    });

                //Observe available routefilms in local Room database
                catalogViewModel.getRoutefilms(currentConfiguration.getAccountToken()).observe(this, routefilms -> {
                    if (currentConfiguration.isLocalPlay()) {
                        Log.d(TAG, "This account has localPlay activated.");
                    }

                    Log.d(TAG, "Routefilms based on accounttoken "+currentConfiguration.getAccountToken()+" => "+routefilms.size());
                    //Assertion block if the routefilms are 0 then return to profile page.
                    if (routefilms.size() < 1) {
                        Toast.makeText(getApplicationContext(), getString(R.string.catalog_no_movies_warning), Toast.LENGTH_LONG).show();
                        CatalogActivity.this.finish();
                    }

                    //pass profiles to adapter
                    availableMediaAdapter = new AvailableMediaAdapter(routefilms.toArray(new Routefilm[0]), currentConfiguration.isLocalPlay());

                    final ImageView imageView = findViewById(R.id.selected_route_infomap_two);
                    availableMediaAdapter.setRouteInfoImageView(imageView);
                    final LinearLayout selectedRouteTextLayoutBlock = findViewById(R.id.selected_route_text_information);
                    availableMediaAdapter.setRouteInfoTextView(selectedRouteTextLayoutBlock);

                    availableMediaAdapter.setCatalogRecyclerViewClickListener(this);

                    //set adapter to recyclerview
                    availableMediaRecyclerView.setAdapter(availableMediaAdapter);
                    //set recyclerview visible
                    availableMediaRecyclerView.setVisibility(View.VISIBLE);
                });
                //getAvailableMedia(currentConfiguration.getAccountToken());
            }
        });
    }

    public void playSelectedVideo(View view) {
        final Intent videoPlayer = new Intent(getApplicationContext(), VideoplayerActivity.class);
        startActivity(videoPlayer);
        finish();
    }

    public void getAvailableMedia(final String apikey) {
        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);

        OneTimeWorkRequest routeMoviesRequest = new OneTimeWorkRequest.Builder(AvailableMediaServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("available-movies")
                .build();

        WorkManager
                .getInstance(this)
                .enqueue(routeMoviesRequest);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(routeMoviesRequest.getId())
                .observe(this, workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("movie-list");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            Movie movieList[] = objectMapper.readValue(result, Movie[].class);

                            if (movieList.length >0) {
                                //pass profiles to adapter
                                AvailableMediaAdapter availableMediaAdapter = new AvailableMediaAdapter(movieList);

                                final ImageView imageView = findViewById(R.id.selected_route_infomap_two);
                                availableMediaAdapter.setRouteInfoImageView(imageView);
                                final LinearLayout selectedRouteTextLayoutBlock = findViewById(R.id.selected_route_text_information);
                                availableMediaAdapter.setRouteInfoTextView(selectedRouteTextLayoutBlock);

                                availableMediaAdapter.setCatalogRecyclerViewClickListener(this);

                                //set adapter to recyclerview
                                availableMediaRecyclerView.setAdapter(availableMediaAdapter);
                                //set recyclerview visible
                                availableMediaRecyclerView.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(this, getString(R.string.catalog_no_movies_warning), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } catch (JsonMappingException jsonMappingException) {
                            Log.e(TAG, jsonMappingException.getLocalizedMessage());
                        } catch (JsonProcessingException jsonProcessingException) {
                            Log.e(TAG, jsonProcessingException.getLocalizedMessage());
                        }
                    }
                });
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        availableMediaRecyclerView.getLayoutManager().scrollToPosition(position);
    }
}