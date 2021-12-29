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
public interface FlagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(final Flag flag);

    @Update
    void update(final Flag flag);

    @Delete
    void delete(final Flag flag);

    @Query("DELETE FROM flags_table")
    void nukeTable();

    @Query("SELECT * FROM flags_table ft")
    LiveData<List<Flag>> getAllFlags();

    @Query("SELECT * FROM flags_table ft WHERE ft.flag_id = :flagId LIMIT 1")
    LiveData<Flag> getFlag(final Integer flagId);

    @Query("SELECT ft.flag_id, country_iso, country_name, flag_filesize, flag_url FROM flags_table ft INNER JOIN movieflags_table mft ON ft.flag_id = mft.flag_id INNER JOIN usage_tracker_table utt ON mft.movie_id = utt.selected_movie LIMIT 1")
    LiveData<Flag> getFlagFromSelectedMovie();
}
