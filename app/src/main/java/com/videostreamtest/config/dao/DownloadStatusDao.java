package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.StandAloneDownloadStatus;

@Dao
public interface DownloadStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StandAloneDownloadStatus standAloneDownloadStatus);

    @Update
    void update(StandAloneDownloadStatus standAloneDownloadStatus);

    @Delete
    void delete(StandAloneDownloadStatus standAloneDownloadStatus);

    @Query("SELECT * FROM download_table WHERE download_movie_id = :movieId")
    LiveData<StandAloneDownloadStatus> getDownloadStatus(final Integer movieId);
}
