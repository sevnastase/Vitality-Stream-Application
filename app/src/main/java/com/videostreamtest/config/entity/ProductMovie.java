package com.videostreamtest.config.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "productmovie_table")
public class ProductMovie {
    @PrimaryKey
    @ColumnInfo(name = "product_movie_id")
    @NonNull
    private Integer pmId;

    @ColumnInfo(name = "movie_id")
    @NonNull
    private Integer movieId;

    @ColumnInfo(name = "product_id")
    @NonNull
    private Integer productId;

    @NonNull
    public Integer getPmId() {
        return pmId;
    }

    public void setPmId(@NonNull Integer pmId) {
        this.pmId = pmId;
    }

    @NonNull
    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(@NonNull Integer movieId) {
        this.movieId = movieId;
    }

    @NonNull
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(@NonNull Integer productId) {
        this.productId = productId;
    }
}
