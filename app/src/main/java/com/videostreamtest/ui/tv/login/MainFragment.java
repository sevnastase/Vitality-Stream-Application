package com.videostreamtest.ui.tv.login;

import android.graphics.Movie;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.videostreamtest.R;

import java.util.HashMap;
import java.util.List;

public class MainFragment extends BrowseSupportFragment implements
        LoaderManager.LoaderCallbacks<HashMap<String, List<AnimatedImageDrawable>>> {

    private BackgroundManager backgroundManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //loadVideoData();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prepareBackgroundManager();
        setupUIElements();
//        setupEventListeners();
    }

    @NonNull
    @Override
    public Loader<HashMap<String, List<AnimatedImageDrawable>>> onCreateLoader(int id, @Nullable Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<HashMap<String,
                            List<AnimatedImageDrawable>>> loader,
                            HashMap<String, List<AnimatedImageDrawable>> data) {

    }

    @Override
    public void onLoaderReset(@NonNull Loader<HashMap<String, List<AnimatedImageDrawable>>> loader) {

    }

    private void prepareBackgroundManager() {
        backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        Drawable defaultBackground = ResourcesCompat.getDrawable(getResources(),
                R.drawable.default_tv_background,
                null);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    private void setupUIElements() {
//        setBadgeDrawable(getActivity().getResources()
//                .getDrawable(R.drawable.videos_by_google_banner));
        // Badge, when set, takes precedent over title
        setTitle(getString(R.string.app_name));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        // set headers background color
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.light_blue_900));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark));
    }
}