package com.videostreamtest.ui.phone.routefilmpicker;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.repository.RoutefilmRepository;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.ConfigurationHelper;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.helpers.ViewHelper;
import com.videostreamtest.ui.phone.login.LoginActivity;

import java.io.File;
import java.util.List;

public class RoutefilmPickerActivity extends AppCompatActivity {
    private static final String TAG = RoutefilmPickerActivity.class.getSimpleName();

    /** The number of routefilms displayed per row in {@code this#routefilmsRecyclerView}. */
    private final static int ITEMS_PER_ROW = 4;
    final static int DEFAULT_SELECTED_POSITION = 0;
    private boolean pageLoading = true;

    // DATA
    private Product selectedProduct;
    private Routefilm[] availableRoutefilms;
    private TextView appAndAccountInfoTextView;
    private String apikey;

    // DATA FLOW
    private final RoutefilmRepository routefilmRepository = new RoutefilmRepository(getApplication());

    // VIEWS
    private ImageView productLogoView;
    private Button backToProductPickerButton;
    private RecyclerView routefilmsRecyclerView;
    private RoutefilmAdapter routefilmAdapter;
    private ConstraintLayout navigationPad;
    private ImageButton navigationUpArrow;
    private ImageButton navigationLeftArrow;
    private ImageButton navigationRightArrow;
    private ImageButton navigationDownArrow;

    // SELECTED ROUTEFILM INFO
    private Routefilm selectedRoutefilm;
    private int selectedRoutefilmPosition;
    private TextView selectedRoutefilmTitleTextView;

    /** For displaying duration/distance. */
    private ConstraintLayout selectedRoutefilmInformationBoxLayout;
    private TextView selectedRoutefilmInfoTextView;
    private ImageView selectedRoutefilmMapImageView;
    private ImageView selectedRoutefilmCountryFlagImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routefilm_picker);
        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        selectedProduct = new GsonBuilder().create().fromJson(getIntent().getExtras().getString("product_object", "{}"), Product.class);
        apikey = AccountHelper.getAccountToken(this);
        if (apikey == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            Toast.makeText(this, "There was an error with your account", Toast.LENGTH_LONG).show();
            startActivity(loginIntent);
            return;
        }

        initViews();
    }

    private void initViews() {
        productLogoView = findViewById(R.id.product_logo_view);
        Picasso.get()
                .load(selectedProduct.getProductLogoButtonPath())
                .resize(300, 225)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(productLogoView);

        backToProductPickerButton = findViewById(R.id.back_to_productpicker_button);
        backToProductPickerButton.setOnClickListener(view -> finish());

        appAndAccountInfoTextView = findViewById(R.id.app_account_info_textview);
        String appVersionNumber = ConfigurationHelper.getVersionNumber(this);
        String appAndAccountInfo = String.format("%s:%s", appVersionNumber, apikey);
        appAndAccountInfoTextView.setText(appAndAccountInfo);

        navigationPad = findViewById(R.id.navigation_pad);
        navigationUpArrow = findViewById(R.id.navigation_up_arrow);
        navigationLeftArrow = findViewById(R.id.navigation_left_arrow);
        navigationRightArrow = findViewById(R.id.navigation_right_arrow);
        navigationDownArrow = findViewById(R.id.navigation_down_arrow);

        if (ViewHelper.isTouchScreen(this)) {
            initNavigationArrow(navigationUpArrow, -1 * ITEMS_PER_ROW);
            initNavigationArrow(navigationLeftArrow, -1);
            initNavigationArrow(navigationRightArrow, 1);
            initNavigationArrow(navigationDownArrow, ITEMS_PER_ROW);
            navigationPad.setVisibility(View.VISIBLE);
        } else {
            navigationPad.setVisibility(View.GONE);
        }

        selectedRoutefilmInformationBoxLayout = findViewById(R.id.selected_routefilm_information_layout);
        selectedRoutefilmInformationBoxLayout.setVisibility(View.GONE);
        selectedRoutefilmTitleTextView = findViewById(R.id.selected_routefilm_title_textview);
        selectedRoutefilmInfoTextView = findViewById(R.id.selected_routefilm_info_textview);
        selectedRoutefilmCountryFlagImageView = findViewById(R.id.selected_routefilm_flag_imageview);
        if (!isProductDistanceBased(selectedProduct)) {
            selectedRoutefilmCountryFlagImageView.setVisibility(View.GONE);
        }
        selectedRoutefilmMapImageView = findViewById(R.id.selected_routefilm_map_imageview);

        loadProductMovies();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (ViewHelper.isTouchScreen(this)) return false;

        // NB: The OK/SELECT button has to be handled in RoutefilmAdapter. If defined here,
        // it will not work as expected.
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                int position = routefilmAdapter.getSelectedRoutefilmPosition();

                if (position % ITEMS_PER_ROW == 0) {
                    backToProductPickerButton.requestFocus();
                    selectedRoutefilmPosition = position;
                    routefilmAdapter.setSelectedRoutefilmPosition(RecyclerView.NO_POSITION);
                    routefilmAdapter.notifyItemChanged(position);
                } else {
                    jump(-1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (backToProductPickerButton.hasFocus()) {
                    routefilmsRecyclerView.requestFocus();
                    routefilmsRecyclerView.scrollToPosition(selectedRoutefilmPosition);
                    routefilmAdapter.setSelectedRoutefilmPosition(selectedRoutefilmPosition);
                    routefilmAdapter.notifyItemChanged(selectedRoutefilmPosition);
                } else {
                    jump(1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                jump(-4);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                jump(4);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void loadProductMovies() {
        if (!apikey.isEmpty()) {
            String accountType = AccountHelper.getAccountType(this);
            if (accountType == null) {
                return;
            }

            LiveData<List<Routefilm>> routefilmsLiveData;

            switch (accountType.toLowerCase()) {
                case "standalone":
                case "hybrid":
                    routefilmsLiveData = routefilmRepository.getAllStandaloneProductRoutefilms(apikey);
                    break;
                case "streaming":
                    routefilmsLiveData = routefilmRepository.getAllProductRoutefilms(apikey);
                    break;
                default:
                    Toast.makeText(this, "Could not load movies", Toast.LENGTH_LONG).show();
                    Log.d(TAG, String.format("Retrieved account type was %s, could not load movies", accountType));
                    return;
            }

            Observer<List<Routefilm>> oneTimeObserver = new Observer<>() {
                @Override
                public void onChanged(List<Routefilm> routefilms) {
                    if (routefilms != null) {
                        availableRoutefilms = new Routefilm[routefilms.size()];
                        availableRoutefilms = routefilms.toArray(availableRoutefilms);
                        preLoadImages(routefilms);
                        initRecyclerView();
                        routefilmsLiveData.removeObserver(this);
                    }
                }
            };
            routefilmsLiveData.observe(this, oneTimeObserver);
        }

//        new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
//        new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
//        new Handler().postDelayed(this::arrangeMoviesByFavorites, 100);
    }

    private void initRecyclerView() {
        routefilmsRecyclerView = findViewById(R.id.routefilms_recyclerview);
        routefilmsRecyclerView.setLayoutManager(new GridLayoutManager(this, ITEMS_PER_ROW));

        RoutefilmAdapter.SelectedRoutefilmListener listener = createListener();

        routefilmAdapter = new RoutefilmAdapter(availableRoutefilms, selectedProduct, listener, this);
        routefilmAdapter.sortByFavorites();
        routefilmsRecyclerView.setAdapter(routefilmAdapter);

        new Handler().postDelayed(() -> {
            routefilmsRecyclerView.scrollToPosition(DEFAULT_SELECTED_POSITION);
            routefilmAdapter.setSelectedRoutefilmPosition(DEFAULT_SELECTED_POSITION);
            if (availableRoutefilms != null && availableRoutefilms.length > 0) {
                selectedRoutefilmInformationBoxLayout.setVisibility(View.VISIBLE);
            }
        }, 200);
    }

    /** Returns an interface implementation {@link RoutefilmAdapter.SelectedRoutefilmListener}
     * that listens to changes in selection and updates the information box
     * at the top of the screen. */
    private RoutefilmAdapter.SelectedRoutefilmListener createListener() {
        return routefilm -> {
            selectedRoutefilm = routefilm;
            Movie movie = Movie.fromRoutefilm(routefilm);
            // Flag
            if (movie.getMovieFlagUrl() != null && !movie.getMovieFlagUrl().isEmpty()) {
                selectedRoutefilmCountryFlagImageView.setVisibility(View.VISIBLE);
                if (DownloadHelper.isFlagsLocalPresent(getApplicationContext())) {
                    Picasso.get()
                            .load(new File(movie.getMovieFlagUrl()))
                            .placeholder(R.drawable.flag_placeholder)
                            .error(R.drawable.flag_placeholder)
                            .resize(150, 100)
                            .into(selectedRoutefilmCountryFlagImageView);
                } else {
                    Picasso.get()
                            .load(movie.getMovieFlagUrl())
                            .placeholder(R.drawable.flag_placeholder)
                            .error(R.drawable.flag_placeholder)
                            .resize(150, 100)
                            .into(selectedRoutefilmCountryFlagImageView);
                }
            } else {
                selectedRoutefilmCountryFlagImageView.setVisibility(View.GONE);
            }

            // Route map
            if (selectedProduct.getSupportStreaming()==0 && performStaticChecks(movie)) {
                Picasso.get()
                        .load(new File(movie.getMovieRouteinfoPath()))
                        .fit()
                        .placeholder(R.drawable.placeholder_map)
                        .error(R.drawable.placeholder_map)
                        .into(selectedRoutefilmMapImageView);
            } else {
                Picasso.get()
                        .load(movie.getMovieRouteinfoPath())
                        .fit()
                        .placeholder(R.drawable.placeholder_map)
                        .error(R.drawable.placeholder_map)
                        .into(selectedRoutefilmMapImageView);
            }

            // Title
            selectedRoutefilmTitleTextView.setText(routefilm.getMovieTitle());

            // Duration/Distance
            String info;
            if (isProductDistanceBased(selectedProduct)) {
                float meters = routefilm.getMovieLength();
                int km = (int) (meters / 1000f);
                int hectometers = (int) ((meters - (km * 1000f)) / 100f);
                info = String.format(getApplicationContext().getString(R.string.catalog_screen_distance), km, hectometers);
            } else {
                int duration = (routefilm.getMovieLength() / routefilm.getRecordedFps()) / 60;
                info = String.format("Duration: %d minutes", duration);
            }
            selectedRoutefilmInfoTextView.setText(info);
        };
    }

    private void initNavigationArrow(ImageButton arrowView, int jumpCount) {
        arrowView.setOnClickListener(view -> jump(jumpCount));
    }

    private void jump(int jumpCount) {
        final int prevPosition = routefilmAdapter.getSelectedRoutefilmPosition();
        final int nextPosition;
        if (jumpCount < 0) {
            nextPosition = Math.max(prevPosition + jumpCount, 0);
        } else {
            nextPosition = Math.min(prevPosition + jumpCount, routefilmAdapter.getItemCount() - 1);
        }

        if (prevPosition != nextPosition && routefilmAdapter != null) {
            routefilmAdapter.setSelectedRoutefilmPosition(nextPosition);
            routefilmAdapter.notifyItemChanged(prevPosition);
            routefilmAdapter.notifyItemChanged(nextPosition);
            routefilmsRecyclerView.getLayoutManager().scrollToPosition(nextPosition);
        }
    }

    private boolean isProductDistanceBased(Product product) {
        String productName = product.getProductName().toLowerCase();
        return productName.contains("praxspin") || productName.contains("praxfit");
    }

    private boolean performStaticChecks(Movie movie) {
        Context appContext = getApplicationContext();
        // StandAlone
        if (selectedProduct.getSupportStreaming()==0) {
            boolean isMovieOnDevice = DownloadHelper.isMoviePresent(appContext, movie);
            boolean isSoundOnDevice = DownloadHelper.isSoundPresent(appContext);
            boolean isMovieSupportImagesOnDevice = DownloadHelper.isMovieImagesPresent(appContext, movie);
            return (isMovieOnDevice&&isSoundOnDevice&&isMovieSupportImagesOnDevice);
        }
        // Streaming
        else {
            return true;
        }
    }

    private void preLoadImages(List<Routefilm> routefilmList) {
        for (final Routefilm routefilm: routefilmList) {
            //Load local paths if standalone
            if (selectedProduct!= null && selectedProduct.getSupportStreaming()==0) {
                DownloadHelper.setLocalMedia(getApplicationContext(), routefilm);
            }
            //preload scenery
            if (routefilm.getMovieRouteinfoPath().startsWith("/")) {
                Picasso.get().load(new File(routefilm.getMovieRouteinfoPath())).fetch();
            } else {
                Picasso.get().load(routefilm.getMovieRouteinfoPath()).fetch();
            }
            //preload maps
            if (routefilm.getMovieImagepath().startsWith("/")) {
                Picasso.get().load(new File(routefilm.getMovieImagepath())).fetch();
            } else {
                Picasso.get().load(routefilm.getMovieImagepath()).fetch();
            }
            //preload flags
            if (routefilm.getMovieFlagUrl() != null && !routefilm.getMovieFlagUrl().isEmpty()) {
                if (routefilm.getMovieFlagUrl().startsWith("/")) {
                    Picasso.get().load(new File(routefilm.getMovieFlagUrl())).fetch();
                } else {
                    Picasso.get().load(routefilm.getMovieFlagUrl()).fetch();
                }
            }
        }
    }
}