package com.videostreamtest.config.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.videostreamtest.config.dao.ProductDao;
import com.videostreamtest.config.db.PraxtourDatabase;
import com.videostreamtest.data.model.response.Product;

import java.util.List;

public class ProductRepository {
    private ProductDao productDao;

    public ProductRepository(Application application) {
        PraxtourDatabase praxtourDatabase = PraxtourDatabase.getDatabase(application);
        productDao = praxtourDatabase.productDao();
    }

    public LiveData<List<com.videostreamtest.config.entity.Product>> getAccountProducts(final String accountToken, final boolean iStreamAccount) {
        return productDao.getAccountProducts(accountToken, iStreamAccount);
    }

    public LiveData<List<com.videostreamtest.config.entity.Product>> getAllAccountProducts(final String accountToken) {
        return productDao.getAllAccountProducts(accountToken);
    }

    public void insertMultipleProducts(final List<Product> productList, final String accountToken) {
        //Check if there are stored products already AND provided product list is filled
        if (productList.size() > 0) {
            for (Product product: productList) {
//                this.insert(product, accountToken);
            }
        }
    }

    //map response product model to database product model and insert in the database
    public void insert(final com.videostreamtest.config.entity.Product newProduct) {
//        com.videostreamtest.config.entity.Product dbProduct = new com.videostreamtest.config.entity.Product();
//        dbProduct.setAccountToken(accountToken);
//        dbProduct.setBlocked(responseProduct.getBlocked());
//        dbProduct.setProductLogoButtonPath(responseProduct.getProductLogoButtonPath());
//        dbProduct.setProductName(responseProduct.getProductName());
//        dbProduct.setSupportStreaming(responseProduct.getSupportStreaming());
//        dbProduct.setDefaultSettingsId(responseProduct.getDefaultSettingsId());

        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            productDao.insert(newProduct);
        });
    }

    public void update(com.videostreamtest.config.entity.Product updatedProduct) {
        PraxtourDatabase.databaseWriterExecutor.execute( () -> {
            productDao.update(updatedProduct);
        });
    }

    public LiveData<com.videostreamtest.config.entity.Product> getSelectedProduct() {
        return productDao.getSelectedProduct();
    }

}
