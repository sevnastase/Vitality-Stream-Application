package com.videostreamtest.workers.webinterface;

import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.data.model.Profile;
import com.videostreamtest.data.model.response.Configuration;
import com.videostreamtest.data.model.response.Product;
import com.videostreamtest.data.model.response.ProductMovieRecord;
import com.videostreamtest.data.model.response.SoundItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface PraxCloud {
    //CONFIGURATION
    @GET("/api/users/current/configuration")
    Call<Configuration> getAccountConfiguration(@Header("api-key") String accountToken);

    //PRODUCTS
    @GET("/api/users/current/subscriptions")
    Call<List<Product>> getActiveProducts(@Header("api-key") String accountToken);

    //ACCOUNTS
    @GET("/api/users/current/profiles")
    Call<List<Profile>> getAccountProfiles(@Header("api-key") String accountToken);

    //MOVIES
    @GET("/api/route/movies")
    Call<List<Movie>> getRoutefilms(@Header("api-key") String accountToken);
    @GET("/api/productmovies")
    Call<List<ProductMovieRecord>> getAllProductMovies(@Header("api-key") String accountToken);
    @GET("/api/route/movieparts/{movie_id}")
    Call<List<MoviePart>> getRoutepartsOfMovieId(@Path (value = "movie_id", encoded = true) Integer movieId, @Header("api-key") String accountToken);

    //SOUND
    @GET("/api/sound/")
    Call<List<SoundItem>> getSounds(@Header("api-key") String accountToken);
    @GET("/api/sound/background/{movie_id}")
    Call<List<BackgroundSound>> getBackgroundSounds(@Path(value = "movie_id", encoded = true) Integer movieId, @Header("api-key") String accountToken);
    @GET("/api/sound/effects/{movie_id}")
    Call<List<EffectSound>> getEffectSounds(@Path(value = "movie_id", encoded = true) Integer movieId, @Header("api-key") String accountToken);

    //FLAGS
    @GET("/api/flags")
    Call<List<Flag>> getFlags(@Header("api-key") String accountToken);
    @GET("/api/route/movies/flags")
    Call<List<MovieFlag>> getMovieFlags(@Header("api-key") String accountToken);

}
