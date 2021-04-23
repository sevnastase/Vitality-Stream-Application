package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Routefilm;

import java.util.List;

@Dao
public interface RoutefilmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Routefilm routefilm);

    @Update
    void update(Routefilm routefilm);

    @Delete
    void delete(Routefilm routefilm);

    @Query("SELECT * FROM routefilm_table WHERE account_token = :accountToken ORDER BY movie_title ASC")
    LiveData<List<Routefilm>> getRoutefilms(final String accountToken);
}
