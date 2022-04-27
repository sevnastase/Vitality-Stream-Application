package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.videostreamtest.config.entity.StandAloneDownloadStatus;

import java.util.List;

@Dao
public interface DownloadStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StandAloneDownloadStatus standAloneDownloadStatus);

    @Update
    void update(StandAloneDownloadStatus standAloneDownloadStatus);

    @Delete
    void delete(StandAloneDownloadStatus standAloneDownloadStatus);

    @Query("DELETE FROM download_table WHERE download_movie_id = :movieId")
    void deleteDownloadStatus(final Integer movieId);

    @Query("SELECT * FROM download_table WHERE download_movie_id = :movieId")
    LiveData<StandAloneDownloadStatus> getDownloadStatus(final Integer movieId);

    @Query("SELECT * FROM download_table WHERE download_status < 0")
    List<StandAloneDownloadStatus> getPendingDownloadStatus();

    @Query("UPDATE download_table SET download_status = -1 WHERE download_status < 100")
    void resetInterruptedDownloads();

    @Query("SELECT * FROM download_table")
    LiveData<List<StandAloneDownloadStatus>> getAllDownloadStatus();

    @Query("SELECT * FROM download_table")
    List<StandAloneDownloadStatus> getAllRawDownloadStatus();

    @Transaction
    @Query("SELECT * FROM download_table dt ORDER BY dt.download_status DESC")
    LiveData<List<StandAloneDownloadStatus>> getAllActiveDownloadStatus();
}
