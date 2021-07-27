package com.videostreamtest.ui.phone.helpers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.utils.ApplicationSettings;
import com.videostreamtest.workers.DownloadMovieServiceWorker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DownloadHelper {
    private static final String TAG = DownloadHelper.class.getSimpleName();

    private static final String ROUTEFILM_FOLDER = "routefilms";
    private static final String BOX_FOLDER = "box";
    private static final String MOVIE_INFO_FILE = "parcours.dat";

    private static DownloadHelper thisInstance;

    public static DownloadHelper getInstance() {
        if (thisInstance == null) {
            thisInstance = new DownloadHelper();
        }
        return thisInstance;
    }

    /**
     * Check if movie folder with content is located on any (connected) local storage device within provided context
     * @param context
     * @param movie
     * @return boolean
     */
    public static boolean isMoviePresent(final Context context, final Movie movie){
        /*
            First correct the movie from full movie name to movie id folder.
         */
        correctMoviefolder(context, movie);

        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/" + movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                long totalSizeOnDisk = 0;

                for (File file: possibleMovieLocation.listFiles()) {
                    totalSizeOnDisk += file.length();
                }

                long totalEstimatedSize = movie.getMapFileSize()+movie.getSceneryFileSize()+movie.getMovieFileSize();

                if (totalSizeOnDisk >= totalEstimatedSize) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check and correct when a full named folder is copied instead of a id named folder
     * @param context
     * @param movie
     */
    private static void correctMoviefolder(Context context, Movie movie) {
        //Get movie folder full name
        String extractionpath = movie.getMovieUrl();
        String pathParts[] = extractionpath.split("/");
        String moviefolderName = "movie_title";
        if (pathParts.length>0) {
            moviefolderName = pathParts[pathParts.length-2];
        }

        //Check if full movie folder exists and rename to movie id
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {

            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + moviefolderName;
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length > 0) {

                /*
                   Check if folder name with movie id exists.
                   If so, delete the id folder and then rename moviefullname folder
                 */

                String existingPathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId();
                File existingMovieLocation = new File(existingPathname);

                //Delete folder before flatten en renaming the other
                if (existingMovieLocation.exists()) {
                    deleteMovieFolder(existingMovieLocation);
                }

                //put pictures folder in root folder of moviefolder
                flattenMovieFolder(possibleMovieLocation);

                //The actual renaming of the folder to the id folder
                possibleMovieLocation.renameTo(
                  new File(
                          externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId()
                  )
                );
            }
        }
    }

    private static void flattenMovieFolder(File newMovieLocation) {
        File root = newMovieLocation;
        if (root.listFiles().length>0) {
            for (File file: root.listFiles()) {
                if (file.isDirectory() && file.getName().contains("pictures") && file.listFiles().length>0) {
                    for (File pictureFile: file.listFiles()) {
                        moveFile(file.getAbsolutePath(), "/"+pictureFile.getName(), root.getAbsolutePath());
                    }
                    file.delete();
                }
            }
        }
    }

    private static void runFileChecksForConsistency(final File movieFolderByName) {
        //TODO: Run check in for loop of root folder of proved File object
    }

    private static void moveFile(String inputPath, String inputFile, String outputPath) {
        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();


        } catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }

    private static void deleteMovieFolder(File movieFullPathLocation) {
        File files[] = movieFullPathLocation.listFiles();
        if (files.length>0) {
            for (File entry: files) {
                entry.delete();
            }
        }
        movieFullPathLocation.delete();
    }

    /**
     * Check if movie folder contains any routeparts media
     * @param context
     * @param movieId
     * @return boolean
     */
    public static boolean isMovieImagesPresent(final Context context, final int movieId){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieId;
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                int foundMovieImage = 0;
                for (File file: possibleMovieLocation.listFiles()) {
                    if (file.getName().toLowerCase().equals("map.jpg") && file.getName().toLowerCase().endsWith(".jpg")) {
                        foundMovieImage++;
                    }
                    if (file.getName().toLowerCase().equals("scenery.jpg") && file.getName().toLowerCase().endsWith(".jpg")) {
                        foundMovieImage++;
                    }
                }
                if (foundMovieImage == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if movie folder contains any routeparts media
     * @param context
     * @param movieId
     * @return boolean
     */
    public static boolean isMovieMediaPresent(final Context context, final int movieId){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movieId;
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {

                for (File file: possibleMovieLocation.listFiles()) {
                    if (file.getName().startsWith("T") && file.getName().endsWith(".jpg")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if sound folder with content is located on any (connected) local storage device within provided context
     * @param context
     * @return boolean
     */
    public static boolean isSoundPresent(final Context context){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER;
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length==15) {
                long totalSizeOnDisk = 0;

                for (File file: possibleMovieLocation.listFiles()) {
                    totalSizeOnDisk += file.length();
                }

                if (totalSizeOnDisk > 0L) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adjust object url's to local storage paths
     * @param context
     * @param movie
     */
    public static void setLocalMedia(final Context context, final Movie movie) {
        String movieFileName = "";
        String sceneryFileName = "";
        String mapFilename = "";
        try {
            URL movieUrl = new URL(movie.getMovieUrl());
            movieFileName = new File(movieUrl.getFile()).getName();
            URL sceneryUrl = new URL(movie.getMovieImagepath());
            sceneryFileName = new File(sceneryUrl.getFile()).getName();
            URL mapUrl = new URL(movie.getMovieRouteinfoPath());
            mapFilename = new File(mapUrl.getFile()).getName();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return;
        }
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                movie.setMovieUrl(pathname+"/"+movieFileName);
                movie.setMovieImagepath(pathname+"/"+sceneryFileName);
                movie.setMovieRouteinfoPath(pathname+"/"+mapFilename);
            }
        }
    }

    /**
     * Adjust moviepart object url's to local storage paths
     * @param context
     * @param moviePart
     */
    public static File getLocalMediaRoutepart(final Context context, final MoviePart moviePart) {
        String moviePartFileName = "";
        try {
            URL moviePartUrl = new URL(moviePart.getMoviepartImagepath());
            moviePartFileName = new File(moviePartUrl.getFile()).getName();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
        File file;
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + moviePart.getMovieId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                return new File(pathname+"/"+moviePartFileName);
            }
        }
        return null;
    }

    /**
     * Get local sound URI
     * @param context
     * @param soundItemUri
     * @return
     */
    public static Uri getLocalSound(final Context context, final Uri soundItemUri) {
        String localFileName = soundItemUri.getPath().substring(soundItemUri.getPath().lastIndexOf('/'), soundItemUri.getPath().length());
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER + localFileName;
            File possibleSoundItem = new File(pathname);
            if (possibleSoundItem.exists() && possibleSoundItem.isFile()) {
                return Uri.parse(pathname);
            }
        }
        return soundItemUri;
    }

    /**
     * Get local update apk URI
     * @param context
     * @param updateFilename
     * @return
     */
    public static Uri getLocalUpdateFileUri(final Context context, final String updateFilename) {
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_UPDATE_STORAGE_FOLDER + "/"+ updateFilename;
            File possibleUpdateFile = new File(pathname);
            if (possibleUpdateFile.exists() && possibleUpdateFile.isFile()) {
                return Uri.parse(pathname);
            }
        }
        return Uri.parse(updateFilename);
    }

    /**
     * Select volume with the largest free space as recommended storage.
     * @param context
     * @return
     */
    public static File selectStorageVolumeWithLargestFreeSpace(Context context) {
        File selectedVolume = null;
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        long freeSpace = 0;
        for (File externalStorageVolume: externalStorageVolumes) {
            if (externalStorageVolume.getFreeSpace() > freeSpace) {
                freeSpace = externalStorageVolume.getFreeSpace();
                selectedVolume = externalStorageVolume;
            }
        }
        return selectedVolume;
    }

    /**
     * Check which volume has the largest disk and return the location as File object.
     * @return
     */
    public static File selectLargestStorageVolume(Context context) {
        File selectedVolume = null;
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context, null);
        long space = 0;
        for (File externalStorageVolume: externalStorageVolumes) {
            Log.d(DownloadMovieServiceWorker.class.getSimpleName(), externalStorageVolume.getAbsolutePath() + " >> Total Space ::  "+externalStorageVolume.getTotalSpace());
            if (externalStorageVolume.getTotalSpace() > space) {
                space = externalStorageVolume.getTotalSpace();
                selectedVolume = externalStorageVolume;
            }
        }
        return selectedVolume;
    }

    /**
     * Returns if there is enough free space to download the entire media needed for the movie.
     * @param context
     * @param fileSize
     * @return boolean
     */
    public static boolean canFileBeCopied(Context context, long fileSize) {
        return (selectStorageVolumeWithLargestFreeSpace(context).getFreeSpace() > fileSize);
    }

    public static File searchMoviefolder(File boxFolder, Movie selectedMovie) {
        if (boxFolder.exists() && boxFolder.isDirectory()
            && boxFolder.listFiles().length>0) {
            for (File movieFolder : boxFolder.listFiles()) {
                if (isMoviefolderBasedOnTitle(movieFolder, selectedMovie.getMovieTitle())) {
                    return movieFolder;
                }
            }
        }
        return null;
    }

    public static boolean isMoviefolderBasedOnTitle(File movieFolder, String movieTitle) {
        if (movieFolder.isDirectory() && movieFolder.listFiles().length>0) {
            for (File file : movieFolder.listFiles()) {
                if (file.getPath().toLowerCase().equalsIgnoreCase(MOVIE_INFO_FILE)){
                    String titleFromFile = readMovieTitleFromMovieInformationFile(file);
                    if (titleFromFile.equalsIgnoreCase(movieTitle)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String readMovieTitleFromMovieInformationFile(final File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null)
            {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        }
        catch (IOException ioException)
        {
            Log.e(TAG, ioException.getLocalizedMessage());
        }

        String[] splitLinesInformation = contentBuilder.toString().split("\n");
        if (splitLinesInformation.length>0) {
            return splitLinesInformation[0];
        }
        return "";
    }

    public static List<File> getBoxFolders(File storageDirectory) {
        List<File> listBoxDirs = new ArrayList<>();
        File routefilmsDir = null;

        //ASSERT
        if (storageDirectory == null) {
            return listBoxDirs;
        }
        //GET ROUTEFILMS MAIN FOLDER
        if (storageDirectory.listFiles().length>0) {
            for (File item: storageDirectory.listFiles()) {
                if (item.isDirectory() && item.getName().equalsIgnoreCase(ROUTEFILM_FOLDER)) {
                    routefilmsDir = item;
                }
            }
        }
        //WALK THROUGH BOX FOLDERS
        if (routefilmsDir != null) {
            if (routefilmsDir.listFiles().length>0) {
                for (File directory : routefilmsDir.listFiles()) {
                    if (directory.isDirectory() && directory.getName().toLowerCase().contains(BOX_FOLDER)) {
                        listBoxDirs.add(directory);
                    }
                }
            }
        }
        return listBoxDirs;
    }

    public static File getStorageDirectoryContainingRouteFilms(Context context) {
        int size = context.getExternalFilesDirs(null).length;
        if (size>0) {
            for (File storage: context.getExternalFilesDirs(null)) {
                if (isRoutefilmFolderOnStorage(storage)) {
                    return storage;
                }
            }
        }
        return null;
    }

    private static boolean isRoutefilmFolderOnStorage(File storageDirectory) {
        if (storageDirectory.listFiles().length>0) {
            for (File item: storageDirectory.listFiles()) {
                if (item.isDirectory() && item.getName().equalsIgnoreCase(ROUTEFILM_FOLDER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
