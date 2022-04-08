package com.videostreamtest.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.videostreamtest.data.model.Movie;
import com.videostreamtest.service.database.DatabaseRestService;
import com.videostreamtest.ui.phone.helpers.DownloadHelper;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.webinterface.PraxCloud;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.videostreamtest.utils.ApplicationSettings.PRAXCLOUD_API_URL;

public class DataIntegrityCheckServiceWorker extends Worker {
    private static final String TAG = DataIntegrityCheckServiceWorker.class.getSimpleName();
    private static final String CHECKSUM_DIGEST_MD5_FILENAME = "checksum_digest.md5";

    private DatabaseRestService databaseRestService;

    public DataIntegrityCheckServiceWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        SharedPreferences myPreferences = getApplicationContext().getSharedPreferences("app",0);
        final String apikey = myPreferences.getString("apikey", "");

        databaseRestService = new DatabaseRestService();

        HandlerThread thread = new HandlerThread("DataIntegrityServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper dataIntegrityLooper = thread.getLooper();
        Handler dataIntegrityHandler = new Handler(dataIntegrityLooper);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(PRAXCLOUD_API_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                PraxCloud praxCloud = retrofit.create(PraxCloud.class);
                Call<List<Movie>> call = praxCloud.getRoutefilms(apikey);
                List<Movie> accountMovies = new ArrayList<>();

                try {
                    accountMovies = call.execute().body();
                } catch (IOException ioException) {
                    Log.e(TAG, ioException.getLocalizedMessage());
                }

                if (accountMovies!= null && accountMovies.size()>0) {
                    for (final Movie movie: accountMovies) {
                        final String  cloudChecksum = getExternalChecksumDigest(movie);
                        final String localChecksum = calculateCurrentChecksum(movie);
                        Log.d(TAG, "local: "+localChecksum);
//                        Log.d(TAG, "CHECKSUM RESULT: ["+movie.getMovieTitle()+"] "+localChecksum.equals(cloudChecksum));
                        databaseRestService.writeLog(apikey, "DataIntegrityCheckServiceWorker: CHECKSUM RESULT["+movie.getMovieTitle()+"] "+localChecksum.equals(cloudChecksum),"DEBUG", "");
                        if (!cloudChecksum.equals("") && !localChecksum.equals(cloudChecksum)) {
                            databaseRestService.writeLog(apikey, "DataIntegrityCheckServiceWorker: ["+movie.getMovieTitle()+"] Deleted bad movie, disk or file may be corrupted!","ERROR", "");
                            databaseRestService.writeLog(apikey, "DataIntegrityCheckServiceWorker: Cloud checksum: "+cloudChecksum+" : Local Checksum: "+localChecksum,"DEBUG", "");
                            deleteMovieFile(movie);
                            //TODO: INITIATE NEW DOWNLOAD
                            /**
                             * in productActivity().onResume() the method downloadMovies() is called, which may act to resolve
                             */
                        }
                    }
                }
                databaseRestService.writeLog(apikey, "DataIntegrityCheckServiceWorker: Done!","DEBUG", "");
            }
        };

        databaseRestService.writeLog(apikey, "DataIntegrityCheckServiceWorker: Started!","DEBUG", "");
        dataIntegrityHandler.postDelayed(runnable, 5000);

        return Result.success();
    }

    private String getExternalChecksumDigest(final Movie movie) {
        String checksum = "";
        try {
            // Create a URL for the desired page
            URL url = new URL(getBaseUrl(movie.getMovieUrl())+CHECKSUM_DIGEST_MD5_FILENAME);

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            int linenumber = 0;
            while ((str = in.readLine()) != null) {
                // str is one line of text; readLine() strips the newline character(s)
                Log.d(TAG, str);
                if (linenumber ==0) {
                    checksum = str;
                }
                linenumber++;
            }
            in.close();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG,e.getLocalizedMessage());
        }

        return checksum;
    }

    private String calculateCurrentChecksum(final Movie movie) {
        return DownloadHelper.calculateMD5(getMovieFile(movie));
    }

    private boolean deleteMovieFile(final Movie movie) {
        return getMovieFile(movie).delete();
    }

    private String getBaseUrl(final String url) {
        return url.substring(0, url.lastIndexOf(File.separator)+1);
    }

    private File getMovieFile(final Movie movie) {
        final String movieFilename = movie.getMovieUrl().substring(movie.getMovieUrl().lastIndexOf(File.separator), movie.getMovieUrl().length());
        final File selectedVolume = DownloadHelper.selectLargestStorageVolume(getApplicationContext());
        return new File(selectedVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+String.valueOf(movie.getId())+"/"+movieFilename);
    }
}
