package com.videostreamtest.config.dao.download;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.GeneralDownloadStatusModel;
@Dao
public interface DownloadStatusGeneralOverviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(final GeneralDownloadStatusModel generalDownloadStatusModel);

    @Update
    void update(final GeneralDownloadStatusModel generalDownloadStatusModel);

    @Delete
    void delete(final GeneralDownloadStatusModel generalDownloadStatusModel);

    @Query("DELETE FROM general_download_status_table")
    void nukeTable();

    @Query("SELECT * FROM general_download_status_table WHERE download_subject LIKE '%'+:subject+'%' ORDER BY download_current_count DESC LIMIT 1")
    LiveData<GeneralDownloadStatusModel> getLatestGeneralDownloadInformation(final Integer currentCount, final String subject);

}
