package com.videostreamtest.config.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.videostreamtest.config.dao.BackgroundSoundDao;
import com.videostreamtest.config.dao.BluetoothDefaultDeviceDao;
import com.videostreamtest.config.dao.ConfigurationDao;
import com.videostreamtest.config.dao.DownloadStatusDao;
import com.videostreamtest.config.dao.EffectSoundDao;
import com.videostreamtest.config.dao.ProductDao;
import com.videostreamtest.config.dao.ProductMovieDao;
import com.videostreamtest.config.dao.ProfileDao;
import com.videostreamtest.config.dao.RoutefilmDao;
import com.videostreamtest.config.dao.RoutepartDao;
import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.config.entity.Profile;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.Routepart;
import com.videostreamtest.config.entity.StandAloneDownloadStatus;

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
        BluetoothDefaultDevice.class
}, version = 2, exportSchema = true)
public abstract class PraxtourDatabase extends RoomDatabase {
    private final static String TAG = PraxtourDatabase.class.getSimpleName();
    private static volatile PraxtourDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriterExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract ConfigurationDao configurationDao();
    public abstract ProductDao productDao();
    public abstract ProfileDao profileDao();
    public abstract RoutefilmDao routefilmDao();
    public abstract DownloadStatusDao downloadStatusDao();
    public abstract RoutepartDao routepartDao();
    public abstract BackgroundSoundDao backgroundSoundDao();
    public abstract EffectSoundDao effectSoundDao();
    public abstract ProductMovieDao productMovieDao();
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


    public static PraxtourDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PraxtourDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PraxtourDatabase.class,
                            "configuration_database")
                            .addCallback(sRoomDatabaseCallback)
                            .addMigrations(MIGRATION_1_2)
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
