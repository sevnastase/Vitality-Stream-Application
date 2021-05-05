package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.ProductMovie;

import java.util.List;

@Dao
public interface ProductMovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductMovie productMovie);

    @Update
    void update(ProductMovie productMovie);

    @Delete
    void delete(ProductMovie productMovie);

    @Query("SELECT * FROM productmovie_table pmt WHERE pmt.product_id = :productId ")
    LiveData<List<ProductMovie>> getProductMovies(final Integer productId);
}
