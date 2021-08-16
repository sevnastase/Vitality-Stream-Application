package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Product;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("DELETE FROM product_table")
    public void nukeTable();

    @Query("SELECT * FROM product_table pt WHERE pt.account_token = :accountToken AND product_support_streaming = :isStreamAccount AND pt.product_blocked = 0 ")
    LiveData<List<Product>> getAccountProducts(final String accountToken, final boolean isStreamAccount);
}
