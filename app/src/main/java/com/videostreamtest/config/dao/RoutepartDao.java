package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Routepart;

import java.util.List;

@Dao
public interface RoutepartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Routepart routepart);

    @Update
    void update(Routepart routepart);

    @Delete
    void delete(Routepart routepart);

    @Query("SELECT * FROM routepart_table WHERE movie_id = :movieId ORDER BY moviepart_framenumber ASC")
    LiveData<List<Routepart>> getRoutepartsOfMovieId(Integer movieId);
}
