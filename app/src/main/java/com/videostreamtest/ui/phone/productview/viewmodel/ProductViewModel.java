package com.videostreamtest.ui.phone.productview.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.videostreamtest.config.entity.BackgroundSound;
import com.videostreamtest.config.entity.BluetoothDefaultDevice;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.config.entity.EffectSound;
import com.videostreamtest.config.entity.Flag;
import com.videostreamtest.config.entity.MovieFlag;
import com.videostreamtest.config.entity.Product;
import com.videostreamtest.config.entity.ProductMovie;
import com.videostreamtest.config.entity.Routefilm;
import com.videostreamtest.config.entity.LocalMoviesDownloadTable;
import com.videostreamtest.config.repository.BackgroundSoundRepository;
import com.videostreamtest.config.repository.BluetoothDefaultDeviceRepository;
import com.videostreamtest.config.repository.ConfigurationRepository;
import com.videostreamtest.config.repository.DownloadStatusRepository;
import com.videostreamtest.config.repository.EffectSoundRepository;
import com.videostreamtest.config.repository.FlagRepository;
import com.videostreamtest.config.repository.ProductMovieRepository;
import com.videostreamtest.config.repository.ProductRepository;
import com.videostreamtest.config.repository.RoutefilmRepository;
import com.videostreamtest.config.repository.UsageTrackerRepository;

import java.math.BigDecimal;
import java.util.List;

public class ProductViewModel extends AndroidViewModel {
    private ConfigurationRepository configurationRepository;
    private RoutefilmRepository routefilmRepository;
    private ProductMovieRepository productMovieRepository;
    private DownloadStatusRepository downloadStatusRepository;
    private BackgroundSoundRepository backgroundSoundRepository;
    private EffectSoundRepository effectSoundRepository;
    private BluetoothDefaultDeviceRepository bluetoothDefaultDeviceRepository;
    private FlagRepository flagRepository;
    private UsageTrackerRepository usageTrackerRepository;
    private ProductRepository productRepository;

    private MutableLiveData<BigDecimal> measuredConnectionSpeed = new MutableLiveData<>();

    public ProductViewModel(@NonNull Application application) {
        super(application);
        configurationRepository = new ConfigurationRepository(application);
        routefilmRepository = new RoutefilmRepository(application);
        downloadStatusRepository = new DownloadStatusRepository(application);
        backgroundSoundRepository = new BackgroundSoundRepository(application);
        effectSoundRepository = new EffectSoundRepository(application);
        productMovieRepository = new ProductMovieRepository(application);
        bluetoothDefaultDeviceRepository = new BluetoothDefaultDeviceRepository(application);
        flagRepository = new FlagRepository(application);
        usageTrackerRepository = new UsageTrackerRepository(application);
        productRepository = new ProductRepository(application);
    }

    public LiveData<Configuration> getCurrentConfig() {
        return configurationRepository.getCurrentConfiguration();
    }

    public LiveData<Flag> getFlagOfMovie() {
        return flagRepository.getFlagOfMovie();
    }
    public LiveData<List<MovieFlag>> getMovieFlags() {
        return flagRepository.getMovieFlags();
    }

    public LiveData<List<Flag>> getAllFlags() {
        return flagRepository.getAllFlags();
    }

    public LiveData<List<Routefilm>> getRoutefilms(final String accountToken) {
        return routefilmRepository.getAllRoutefilms(accountToken);
    }

    public LiveData<List<Routefilm>> getStandaloneProductMovies(final String accountToken) {
        return routefilmRepository.getAllStandaloneProductRoutefilms(accountToken);
    }

    public LiveData<List<Routefilm>> getProductMovies(final String accountToken) {
        return routefilmRepository.getAllProductRoutefilms(accountToken);
    }

    public LiveData<List<ProductMovie>> getPMS(final Integer productId) {
        return productMovieRepository.getProductMovies(productId);
    }

    public void signoutCurrentAccount(Configuration configuration){
        configuration.setCurrent(false);
        configurationRepository.saveCurrentConfiguration(configuration);
    }

    public LiveData<LocalMoviesDownloadTable> getDownloadStatus(final Integer movieId) {
        return downloadStatusRepository.getDownloadStatus(movieId);
    }

    public LiveData<List<LocalMoviesDownloadTable>> getAllDownloadStatus() {
        return downloadStatusRepository.getAllDownloadStatus();
    }

    public LiveData<List<LocalMoviesDownloadTable>> getAllActiveDownloadStatus() {
        return downloadStatusRepository.getAllActiveDownloadStatus();
    }

    public LiveData<List<BackgroundSound>> getBackgroundSounds(final Integer movieId) {
        return backgroundSoundRepository.getBackgroundSounds(movieId);
    }

    public LiveData<List<EffectSound>> getEffectSounds(final Integer movieId) {
        return effectSoundRepository.getEffectSounds(movieId);
    }

    public LiveData<List<BluetoothDefaultDevice>> getBluetoothDefaultDevices() {
        return bluetoothDefaultDeviceRepository.getBluetoothDefaultDevice();
    }

    public void insertBluetoothDefaultDevice(final BluetoothDefaultDevice bluetoothDefaultDevice) {
        bluetoothDefaultDeviceRepository.insertBluetoothDefaultDevice(bluetoothDefaultDevice);
    }

    public LiveData<Product> getSelectedProduct() {
        return productRepository.getSelectedProduct();
    }

    public LiveData<Routefilm> getSelectedRoutefilm() {
        return routefilmRepository.getSelectedRoutefilm();
    }

    public LiveData<BigDecimal> getMeasuredConnectionSpeed() {
        return measuredConnectionSpeed;
    }

    public void setMeasuredConnectionSpeed(BigDecimal measuredConnectionSpeed) {
        this.measuredConnectionSpeed.postValue(measuredConnectionSpeed);
    }
}
