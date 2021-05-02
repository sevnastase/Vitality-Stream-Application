package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.Configuration;

import java.util.List;

@Dao
public interface ConfigurationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Configuration configuration);

    @Update
    void update(Configuration configuration);

    @Delete
    void delete(Configuration configuration);

    @Query("SELECT * FROM configuration_table ct")
    LiveData<List<Configuration>> getConfigurations();

    @Query("SELECT * FROM configuration_table ct WHERE ct.account_token = :accountToken LIMIT 1")
    LiveData<Configuration> getConfiguration(final String accountToken);

    @Query("SELECT * FROM configuration_table ct WHERE ct.is_current LIMIT 1")
    LiveData<Configuration> getCurrentConfiguration();

    @Query("UPDATE configuration_table SET product_count = :productCount WHERE is_current")
    int updateProductCountCurrentConfiguration(int productCount);

    @Query("UPDATE configuration_table SET local_play = :localPlay, boot_on_start = :bootOnStart, communication_device = :communicationDevice, update_praxcloud = :updateFromPraxCloud, praxcloud_mediaserver_url = :mediaServerUrl, praxcloud_mediaserver_local_url = :localMediaServerUrl  WHERE is_current")
    int updateCurrentConfiguration(boolean localPlay, boolean bootOnStart, String communicationDevice, boolean updateFromPraxCloud, String mediaServerUrl, String localMediaServerUrl);
}
