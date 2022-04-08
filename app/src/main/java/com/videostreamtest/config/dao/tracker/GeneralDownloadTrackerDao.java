package com.videostreamtest.config.dao.tracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.tracker.GeneralDownloadTracker;

@Dao
public interface GeneralDownloadTrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(final GeneralDownloadTracker generalDownloadTracker);

    @Update
    void update(final GeneralDownloadTracker generalDownloadTracker);

    @Delete
    void delete(final GeneralDownloadTracker generalDownloadTracker);

    @Query("DELETE FROM general_download_table")
    void nukeTable();

    @Query("SELECT * FROM general_download_table WHERE download_type = :downloadType LIMIT 1")
    LiveData<GeneralDownloadTracker> getCurrentDownloadTrackingInformation(final String downloadType);

}
