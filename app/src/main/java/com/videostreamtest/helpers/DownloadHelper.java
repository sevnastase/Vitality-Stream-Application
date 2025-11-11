package com.videostreamtest.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.data.model.Movie;
import com.videostreamtest.data.model.MoviePart;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
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

        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/" + movie.getId().intValue();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles() != null && possibleMovieLocation.listFiles().length>0) {
                long totalSizeOnDisk = 0;

                for (File file: possibleMovieLocation.listFiles()) {
                    totalSizeOnDisk += file.length();
                }

                long totalEstimatedSize = movie.getSceneryFileSize()+movie.getMovieFileSize();

                if (totalSizeOnDisk >= totalEstimatedSize) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if movie folder contains any routeparts media
     * @param context
     * @param movie
     * @return boolean
     */
    public static boolean isMovieImagesPresent(final Context context, final Movie movie){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/"+movie.getId().intValue();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                int foundMovieImage = 0;
                for (File file: possibleMovieLocation.listFiles()) {
                    if (file.getName().equals(new File(movie.getMovieRouteinfoPath()).getName())) {
                        foundMovieImage++;
                    }
                    if (file.getName().equals(new File(movie.getMovieImagepath()).getName())) {
                        foundMovieImage++;
                    }
                }
                if (foundMovieImage >= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if file is present on harddisk
     * @param context
     * @param filepath
     * @return boolean
     */
    public static boolean isFileOnHarddisk(final Context context, final String filepath){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            List<String> praxtourFilePaths = new ArrayList<>();
            praxtourFilePaths.add(externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/");
            praxtourFilePaths.add(externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER+"/");
            praxtourFilePaths.add(externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER+"/");

            for (final String storagePath:praxtourFilePaths) {
                if (searchDirectory(new File(storagePath), filepath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean searchDirectory(final File file, final String filepath) {
        if (file.isDirectory() && file.listFiles().length>0) {
            for (final File f: file.listFiles()) {
                if (f.isDirectory()) {
                    searchDirectory(f, filepath);
                }
                if (f.getName().equalsIgnoreCase(new File(filepath).getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getFileFromHarddisk(final Context context, final String filepath) {
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            List<String> praxtourFilePaths = new ArrayList<>();
            praxtourFilePaths.add(externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER+"/");
            praxtourFilePaths.add(externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_SOUND_STORAGE_FOLDER+"/");
            praxtourFilePaths.add(externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER+"/");

            for (final String storagePath:praxtourFilePaths) {
                String found = searchFileInDirectory(new File(storagePath), filepath);
                if (found  != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String searchFileInDirectory(final File file, final String filepath) {
        if (file.isDirectory() && file.listFiles().length>0) {
            for (final File f: file.listFiles()) {
                if (f.isDirectory()) {
                    searchDirectory(f, filepath);
                }
                if (f.getName().equalsIgnoreCase(new File(filepath).getName())) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
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
     * Check if flags folder with content is located on any (connected) local storage device within provided context
     * @param context
     * @return boolean
     */
    public static boolean isFlagsLocalPresent(final Context context){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER;
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>1) {
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
     * Check if flag file is located on any (connected) local storage device within provided context
     * @param context
     * @return boolean
     */
    public static boolean isFlagLocalPresent(final Context context, final String flagFilename){
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath()+ ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER;
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>1) {
                for (File file: possibleMovieLocation.listFiles()) {
                   if (file.getName().equalsIgnoreCase(flagFilename)) {
                       return true;
                   }
                }
            }
        }
        return false;
    }

    public static List<Movie> getLocalAvailableMovies(final Context context, final List<Movie> movieList) {
        return new ArrayList<>();
    }

    /**
     * Adjust object url's to local storage paths
     * @param context
     * @param movie
     */
    public static void setLocalMedia(final Context context, final Movie movie) {
        String movieFileName = setLocalFilePath(context, movie.getMovieUrl());
        String sceneryFileName = setLocalFilePath(context, movie.getMovieImagepath());
        String mapFilename = setLocalFilePath(context, movie.getMovieRouteinfoPath());
        String flagFilename = setLocalFilePath(context, movie.getMovieFlagUrl());

        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            //Routefilm files
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + movie.getId();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                if (!movieFileName.contains("/")) {
                    movie.setMovieUrl(pathname + "/" + movieFileName);
                }
                if (!sceneryFileName.contains("/")) {
                    movie.setMovieImagepath(pathname + "/" + sceneryFileName);
                }
                if (!mapFilename.contains("/")) {
                    movie.setMovieRouteinfoPath(pathname + "/" + mapFilename);
                }
            }

            //Flag files
            String pathnameFlags = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER;
            File possibleFlagLocation = new File(pathnameFlags);
            if (possibleFlagLocation.exists() && possibleFlagLocation.listFiles().length>0) {
                if (!flagFilename.contains("/") && !flagFilename.isEmpty()) {
                    movie.setMovieFlagUrl(pathnameFlags+"/"+flagFilename);
                }
            }
        }
    }

    /**
     * Adjust object url's to local storage paths
     * @param context
     * @param routefilm
     */
    public static void setLocalMedia(final Context context, final Routefilm routefilm) {
        String movieFileName = setLocalFilePath(context, routefilm.getMovieUrl());
        String sceneryFileName = setLocalFilePath(context, routefilm.getMovieImagepath());
        String mapFilename = setLocalFilePath(context, routefilm.getMovieRouteinfoPath());

        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_MOVIE_STORAGE_FOLDER + "/" + routefilm.getMovieId().intValue();
            File possibleMovieLocation = new File(pathname);
            if (possibleMovieLocation.exists() && possibleMovieLocation.listFiles().length>0) {
                if (!movieFileName.contains("/")) {
                    routefilm.setMovieUrl(pathname + "/" + movieFileName);
                }
                if (!sceneryFileName.contains("/")) {
                    routefilm.setMovieImagepath(pathname + "/" + sceneryFileName);
                }
                if (!mapFilename.contains("/")) {
                    routefilm.setMovieRouteinfoPath(pathname + "/" + mapFilename);
                }
            }
        }
    }

    private static String setLocalFilePath(final Context context, final String url) {
        try {
            if(!isLocalFilePath(url) && !url.isEmpty()) {
                URL movieUrl = new URL(url);
                return new File(movieUrl.getFile()).getName();
            }
        }
        catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
            String apikey = AccountHelper.getAccountToken(context);
            LogHelper.WriteLogRule(context, apikey, "ERROR ROUTEFILM: [SET LOCAL MEDIA] [URL:" + url + "] " + e.getLocalizedMessage(), "ERROR", "");
        }
        return url;
    }

    private static boolean isLocalFilePath(final String filePath) {
        return (filePath.startsWith("/") && !filePath.contains("http://") && !filePath.contains("https://"));
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
            SharedPreferences myPreferences = context.getSharedPreferences("app",0);
            final String accounttoken = myPreferences.getString("apikey", "unauthorized");
            LogHelper.WriteLogRule(context, accounttoken, "Routepart-url: "+moviePart.getMoviepartImagepath()+" can't be downloaded.", "ERROR",  "");
            return null;
        }
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
     * Get local Flag File object
     * @param context
     * @param flagItem
     * @return
     */
    public static File getLocalFlag(final Context context, final Flag flagItem) {
        String flagFileName = "";
        try {
            URL flagUrl = new URL(flagItem.getFlagUrl());
            flagFileName = new File(flagUrl.getFile()).getName();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getLocalizedMessage());
            SharedPreferences myPreferences = context.getSharedPreferences("app",0);
            final String accounttoken = myPreferences.getString("apikey", "unauthorized");
            LogHelper.WriteLogRule(context, accounttoken, "Flag-url: "+flagItem.getFlagUrl()+" can't be downloaded.", "ERROR",  "");
            return null;
        }
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null);
        for (File externalStorageVolume: externalStorageVolumes) {
            String pathname = externalStorageVolume.getAbsolutePath() + ApplicationSettings.DEFAULT_LOCAL_FLAGS_STORAGE_FOLDER;
            File possibleFlagLocation = new File(pathname);
            if (possibleFlagLocation.exists() && possibleFlagLocation.listFiles().length>0) {
                return new File(pathname+"/"+flagFileName);
            }
        }
        return null;
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
            Log.d(DownloadHelper.class.getSimpleName(), externalStorageVolume.getAbsolutePath() + " >> Total Space ::  "+externalStorageVolume.getTotalSpace());
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
    public static boolean canFileBeCopiedToLargestVolume(Context context, long fileSize) {
        return (selectLargestStorageVolume(context).getFreeSpace() > fileSize);
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

    public static void deleteMovieFolder(final File movieFullPathLocation) {
        File files[] = movieFullPathLocation.listFiles();
        if (files.length>0) {
            for (File entry: files) {
                entry.delete();
            }
        }
        movieFullPathLocation.delete();
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

    public static String getLocalIpV4Address() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            System.err.println(ex.getLocalizedMessage());
        }
        return null;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address) ) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            System.err.println(ex.getLocalizedMessage());
        }
        return null;
    }

    public static boolean isLocalMediaServerInSameNetwork(final String localMediaServerIp) {
        final String localip = getLocalIpV4Address();
        if (localip!=null) {
            final String localDeviceSubNetAddress = localip.substring(0, localip.lastIndexOf("."));
            return localMediaServerIp.contains(localDeviceSubNetAddress);
        }
        return false;
    }

    public static boolean isWebserverReachable(final String ipAddress) {
        boolean isReachable = false;
        try {
            SocketAddress sockaddr = new InetSocketAddress(ipAddress, 80);
            // Create an unbound socket
            Socket sock = new Socket();

            // This method will block no more than timeoutMs.
            // If the timeout occurs, SocketTimeoutException is thrown.
            int timeoutMs = 2000;   // 2 seconds
            sock.connect(sockaddr, timeoutMs);
                isReachable = true;
            sock.close();
            sockaddr = null;
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return isReachable;
    }

    public static String calculateMD5(final File file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        if (!file.exists()) {
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }
}
