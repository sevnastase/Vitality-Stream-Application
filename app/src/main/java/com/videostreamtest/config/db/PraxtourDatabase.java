package com.videostreamtest.config.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.videostreamtest.config.dao.BackgroundSoundDao;
import com.videostreamtest.config.dao.BluetoothDefaultDeviceDao;
import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.dao.DownloadStatusDao;
import com.videostreamtest.config.dao.EffectSoundDao;
import com.videostreamtest.config.dao.FlagDao;
import com.videostreamtest.config.dao.MovieFlagDao;
import com.videostreamtest.config.dao.ProductDao;
import com.videostreamtest.config.dao.ProductMovieDao;
import com.videostreamtest.config.dao.ProfileDao;
import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.dao.RoutepartDao;
import com.videostreamtest.config.dao.ServerStatusDao;
import com.videostreamtest.config.dao.tracker.UsageTrackerDao;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.config.entity.Profile;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.config.entity.ServerStatus;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;
import com.videostreamtest.config.entity.tracker.UsageTracker;
import com.videostreamtest.config.entity.typeconverter.Converters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        Configuration.class,
        Product.class,
        Profile.class,
        Routefilm.class,
        StandAloneDownloadStatus.class,
        Routepart.class,
        BackgroundSound.class,
        EffectSound.class,
        ProductMovie.class,
        BluetoothDefaultDevice.class,
        ServerStatus.class,
        Flag.class,
        MovieFlag.class,
        UsageTracker.class
}, version = 6, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class PraxtourDatabase extends RoomDatabase {
    private final static String TAG = PraxtourDatabase.class.getSimpleName();
    private static volatile PraxtourDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriterExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract ConfigurationDao configurationDao();
    public abstract ServerStatusDao serverStatusDao();
    public abstract ProductDao productDao();
    public abstract MovieFlagDao movieFlagDao();
    public abstract FlagDao flagDao();
    public abstract ProfileDao profileDao();
    public abstract RoutefilmDao routefilmDao();
    public abstract DownloadStatusDao downloadStatusDao();
    public abstract RoutepartDao routepartDao();
    public abstract BackgroundSoundDao backgroundSoundDao();
    public abstract EffectSoundDao effectSoundDao();
    public abstract ProductMovieDao productMovieDao();
    public abstract UsageTrackerDao usageTrackerDao();
    public abstract BluetoothDefaultDeviceDao bluetoothDefaultDeviceDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `default_ble_device_table` ("
                    + "`ble_id` INTEGER, "
                    + "`ble_address` TEXT,"
                    + "`ble_name` TEXT,"
                    + "`ble_signal_strength` TEXT,"
                    + "`ble_battery_level` TEXT,"
                    + "`ble_sensor_type` TEXT,"
                    +" PRIMARY KEY(`ble_id`))");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `serverstatus_table` ("
                    + "`serverstatus_id` INTEGER NOT NULL, "
                    + "`server_online` INTEGER NOT NULL, "
                    + "`server_last_online_timestamp` INTEGER,"
                    +" PRIMARY KEY(`serverstatus_id`))");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `flags_table` ("
                    + "`flag_id` INTEGER, "
                    + "`country_iso` TEXT NOT NULL DEFAULT '', "
                    + "`country_name` TEXT NOT NULL DEFAULT '',"
                    + "`flag_filesize` INTEGER,"
                    + "`flag_url` TEXT,"
                    +" PRIMARY KEY(`flag_id`))");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `movieflags_table` ("
                    + "`movieflag_id` INTEGER, "
                    + "`movie_id` INTEGER, "
                    + "`flag_id` INTEGER,"
                    +" PRIMARY KEY(`movieflag_id`))");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `usage_tracker_table` ("
                    + "`tracker_accountoken` TEXT NOT NULL DEFAULT '', "
                    + "`selected_product` INTEGER NOT NULL DEFAULT 0, "
                    + "`selected_movie` INTEGER NOT NULL DEFAULT 0, "
                    + "`selected_background_sound` INTEGER NOT NULL DEFAULT 0,"
                    + "`selected_profile` INTEGER NOT NULL DEFAULT 0,"
                    +" PRIMARY KEY(`tracker_accountoken`))");
        }
    };


    public static PraxtourDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PraxtourDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PraxtourDatabase.class,
                            "configuration_database")
                            .addCallback(sRoomDatabaseCallback)
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    //Callback die bij de aanroep van de database bij het opstarten van de app wordt aangeroepen
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // If you want to keep data through app restarts,
            // comment out the following block
//            databaseWriterExecutor.execute(() -> {
                // Populate the database in the background.
                // If you want to start with more words, just add them.
//                CharacterDao dao = INSTANCE.characterDao();
//                dao.deleteAll(); //Empty database
//                //Populate with new static data
//                Character character = new Character(
//                        "Name",
//                        "Height",
//                        "Mass",
//                        "HairColor",
//                        "EyeColor",
//                        "BirthYear",
//                        "Gender");
//                dao.insert(character);
//            });
        }
    };
}
