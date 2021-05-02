package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.BackgroundSound;

import java.util.List;

@Dao
public interface BackgroundSoundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BackgroundSound backgroundSound);

    @Update
    void update(BackgroundSound backgroundSound);

    @Delete
    void delete(BackgroundSound backgroundSound);

    @Query("SELECT * FROM backgroundsound_table bt WHERE movie_id = :movieId")
    LiveData<List<BackgroundSound>> getBackgroundSounds(final Integer movieId);
}
