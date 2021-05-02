package com.videostreamtest.config.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.videostreamtest.config.entity.EffectSound;

import java.util.List;

@Dao
public interface EffectSoundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EffectSound effectSound);

    @Update
    void update(EffectSound effectSound);

    @Delete
    void delete(EffectSound effectSound);

    @Query("SELECT * FROM effectsound_table bt WHERE movie_id = :movieId")
    LiveData<List<EffectSound>> getEffectSounds(final Integer movieId);
}
