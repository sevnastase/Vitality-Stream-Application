package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.ServerStatus;

import java.util.List;

@Dao
public interface ServerStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ServerStatus serverStatus);

    @Update
    void update(ServerStatus serverStatus);

    @Delete
    void delete(ServerStatus serverStatus);

    @Query("SELECT * FROM serverstatus_table sst WHERE serverstatus_id = 1")
    LiveData<ServerStatus> getServerStatus();
}
