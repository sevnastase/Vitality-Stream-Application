package com.videostreamtest.config.dao.tracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.tracker.UsageTracker;

@Dao
public interface UsageTrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(final UsageTracker usageTracker);

    @Update
    void update(final UsageTracker usageTracker);

    @Delete
    void delete(final UsageTracker usageTracker);

    @Query("DELETE FROM usage_tracker_table")
    void nukeTable();

    @Query("SELECT * FROM usage_tracker_table WHERE tracker_accountoken = :accounttoken LIMIT 1")
    LiveData<UsageTracker> getCurrentUsageTrackingInformation(final String accounttoken);

    @Query("SELECT selected_product FROM usage_tracker_table WHERE tracker_accountoken = :accounttoken LIMIT 1")
    LiveData<Integer> getSelectedProduct(final String accounttoken);
    @Query("UPDATE usage_tracker_table SET selected_product = :productId WHERE tracker_accountoken = :accounttoken")
    void setSelectedProduct(final String accounttoken, final Integer productId);

    @Query("SELECT selected_movie FROM usage_tracker_table WHERE tracker_accountoken = :accounttoken LIMIT 1")
    LiveData<Integer> getSelectedMovie(final String accounttoken);
    @Query("UPDATE usage_tracker_table SET selected_movie = :movieId WHERE tracker_accountoken = :accounttoken")
    void setSelectedMovie(final String accounttoken, final Integer movieId);

    @Query("SELECT selected_background_sound FROM usage_tracker_table WHERE tracker_accountoken = :accounttoken LIMIT 1")
    LiveData<Integer> getSelectedBackgroundSound(final String accounttoken);
    @Query("UPDATE usage_tracker_table SET selected_background_sound = :backgroundSoundId WHERE tracker_accountoken = :accounttoken")
    void setSelectedBackgroundSound(final String accounttoken, final Integer backgroundSoundId);

    @Query("SELECT selected_profile FROM usage_tracker_table WHERE tracker_accountoken = :accounttoken LIMIT 1")
    LiveData<Integer> getSelectedProfile(final String accounttoken);
    @Query("UPDATE usage_tracker_table SET selected_profile = :profileId WHERE tracker_accountoken = :accounttoken")
    void setSelectedProfile(final String accounttoken, final Integer profileId);
}
