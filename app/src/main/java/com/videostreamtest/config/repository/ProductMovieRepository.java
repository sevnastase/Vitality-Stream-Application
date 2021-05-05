package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.ProductMovieDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.config.entity.ProductMovie;

import java.util.List;

public class ProductMovieRepository {
    private ProductMovieDao productMovieDao;

    public ProductMovieRepository(Application application) {
        productMovieDao = PraxtourDatabase.getDatabase(application).productMovieDao();
    }

    public LiveData<List<ProductMovie>> getProductMovies(final Integer productId) {
        return productMovieDao.getProductMovies(productId);
    }
}
