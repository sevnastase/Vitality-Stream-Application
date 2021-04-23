package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Profile;

import java.util.List;

@Dao
public interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Profile profile);

    @Update
    void update(Profile profile);

    @Delete
    void delete(Profile profile);

    @Query("DELETE FROM profile_table")
    void deleteAllProfiles();

    @Query("SELECT * FROM profile_table ORDER BY profile_name ASC")
    LiveData<List<Profile>> getAccountProfiles();

    @Query("SELECT * FROM profile_table WHERE profile_id = :profileId")
    LiveData<Profile> getProfile(Integer profileId);
}
