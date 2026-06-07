package com.videostreamtest.config.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.MovieLocalInfo;

import java.util.List;

@Dao
public interface MovieLocalInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MovieLocalInfo info);

    @Update
    void update(MovieLocalInfo info);

    @Delete
    void delete(MovieLocalInfo info);

    @Query("DELETE FROM movie_local_info_table")
    void deleteAll();

    @Query("SELECT * FROM movie_local_info_table WHERE movie_id = :movieId")
    MovieLocalInfo get(Integer movieId);

    @Query("SELECT * FROM movie_local_info_table")
    MovieLocalInfo[] getAll();

    @Query("SELECT * FROM movie_local_info_table mi WHERE mi.movie_id IN (:movieIds)")
    MovieLocalInfo[] getAllFor(List<Integer> movieIds);
}
