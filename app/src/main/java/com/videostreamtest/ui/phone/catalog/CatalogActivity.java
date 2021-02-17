package com.videostreamtest.ui.phone.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videostreamtest.R;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.videoplayer.VideoplayerActivity;
import com.videostreamtest.workers.AvailableMediaServiceWorker;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class CatalogActivity extends AppCompatActivity implements CatalogRecyclerViewClickListener {
     private CatalogViewModel catalogViewModel;
     private RecyclerView availableMediaRecyclerView;
     private GridLayoutManager gridLayoutManager;

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
        catalogViewModel.getApiKey().observe(this, observer -> {
            Log.d(this.getClass().getSimpleName(), "ApiKey found! ");//+catalogViewModel.getApiKey().getValue());
        });

        final TextView profileName = findViewById(R.id.current_loaded_profile);
        SharedPreferences myPreferences = getSharedPreferences("app",0);
        profileName.setTextSize(20);
        profileName.setTextColor(Color.WHITE);
        profileName.setText(myPreferences.getString("profileName", ""));

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

        getAvailableMedia(catalogViewModel.getApiKey().getValue());
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
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        availableMediaRecyclerView.getLayoutManager().scrollToPosition(position);
    }
}