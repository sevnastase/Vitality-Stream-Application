package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;

import java.util.List;

@Dao
public interface MovieFlagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(final MovieFlag movieFlag);

    @Update
    void update(final MovieFlag movieFlag);

    @Delete
    void delete(final MovieFlag movieFlag);

    @Query("DELETE FROM movieflags_table")
    void nukeTable();

    @Query("SELECT * FROM movieflags_table mft")
    LiveData<List<MovieFlag>> getAllMovieFlags();

    @Query("SELECT * FROM movieflags_table mft")
    List<MovieFlag> getAllRawMovieFlags();

    @Query("SELECT * FROM movieflags_table mft WHERE mft.movie_id = :movieId LIMIT 1")
    LiveData<MovieFlag> getMovieFlagFromMovie(final Integer movieId);

}
