package com.videostreamtest.ui.phone.productview.fragments.routeinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class RouteInfoFragment extends Fragment {
    private static final String TAG = RouteInfoFragment.class.getSimpleName();

    private ProductViewModel productViewModel;

    private TextView movieTitleView;
    private TextView movieDistanceView;
    private ImageView movieFlagView;
    private ImageView movieMapView;

    private Product selectedProduct;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_information_block, container, false);
        movieTitleView = view.findViewById(R.id.selected_route_title);
        movieDistanceView = view.findViewById(R.id.selected_route_distance);
        movieFlagView = view.findViewById(R.id.selected_route_country);
        movieMapView = view.findViewById(R.id.selected_route_infomap_two);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        if (view.getContext()==null) {
            return;
        }

        productViewModel.getSelectedProduct().observe(requireActivity(), selectedProduct->{
            if (selectedProduct != null) {
                this.selectedProduct = selectedProduct;
            }
        });

        productViewModel.getSelectedRoutefilm().observe(requireActivity(), selectedMovie -> {
            if (selectedMovie!= null) {
                setMovieTitle(selectedMovie.getMovieTitle());
                setMovieFlag();

                final Movie routefilmMovie = Movie.fromRoutefilm(selectedMovie);
                setMovieMap(routefilmMovie);
                setMovieDetailInfo(routefilmMovie);
            }
        });

    }

    private void setMovieTitle(final String movieTitle) {
        movieTitleView.setText(toString().format(getString(R.string.catalog_selected_route_title), movieTitle));
        movieTitleView.setVisibility(View.VISIBLE);
    }

    private void setMovieFlag() {
        // FIXME: Causes flickering flag at beginning.
        // CAUSE: In ProductActivity/Fragment we walk through every item to align the borders correctly
        productViewModel.getFlagOfMovie().observe(getViewLifecycleOwner(), flag -> {
            if (flag!=null) {
                if (movieFlagView.getVisibility() == View.GONE) {
                    movieFlagView.setVisibility(View.VISIBLE);
                }
                if (DownloadHelper.isFlagsLocalPresent(getContext().getApplicationContext())) {
                    Picasso.get()
                            .load(DownloadHelper.getLocalFlag(getContext().getApplicationContext(), flag))
                            .placeholder(R.drawable.flag_placeholder)
                            .error(R.drawable.flag_placeholder)
                            .resize(150, 100)
                            .into(movieFlagView);
                } else {
                    Picasso.get()
                            .load(flag.getFlagUrl())
                            .placeholder(R.drawable.flag_placeholder)
                            .error(R.drawable.flag_placeholder)
                            .resize(150, 100)
                            .into(movieFlagView);
                }
            } else {
                movieFlagView.setVisibility(View.GONE);
            }
        });
    }

    private void setMovieMap(final Movie selectedMovie){
        //Set Route Information map
        if (isAvailableForLocalLoading(selectedMovie)) {
            DownloadHelper.setLocalMedia(requireActivity(), selectedMovie);
            Picasso.get()
                    .load(new File(selectedMovie.getMovieRouteinfoPath()))
                    .fit()
                    .placeholder(R.drawable.placeholder_map)
                    .error(R.drawable.placeholder_map)
                    .into(movieMapView);
        } else {
            Picasso.get()
                    .load(selectedMovie.getMovieRouteinfoPath())
                    .fit()
                    .placeholder(R.drawable.placeholder_map)
                    .error(R.drawable.placeholder_map)
                    .into(movieMapView);
        }
    }

    private void setMovieDetailInfo(final Movie routefilmMovie) {
        if (selectedProduct == null) {
            return;
        }
        if (selectedProduct.getProductName().toLowerCase().contains("praxfilm")) {
            movieDistanceView.setText(String.format("Duration: %d minutes", ((routefilmMovie.getMovieLength()/routefilmMovie.getRecordedFps())/60)));
        } else {
            float meters = routefilmMovie.getMovieLength();
            int km = (int) (meters / 1000f);
            int hectometers = (int) ((meters - (km * 1000f)) / 100f);
            movieDistanceView.setText(toString().format(getString(R.string.catalog_screen_distance), km, hectometers));
        }
    }

    private boolean isAvailableForLocalLoading(final Movie selectedMovie) {
//        if (selectedMovie != null && selectedMovie.getMovieUrl().contains("http")) {
//            return false;
//        }
        // StandAlone only
        boolean isMovieOnDevice = DownloadHelper.isMoviePresent(requireActivity(), selectedMovie);
        boolean isSoundOnDevice = DownloadHelper.isSoundPresent(requireActivity());
        boolean isMovieSupportImagesOnDevice = DownloadHelper.isMovieImagesPresent(requireActivity(), selectedMovie);
        return (isMovieOnDevice&&isSoundOnDevice&&isMovieSupportImagesOnDevice);
    }
}
