package com.fittrack.inventoryapp.service;

import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;

import java.util.List;
import java.util.Optional;

public interface AssetService {
    List<Asset> findAllAssets();

    Optional<Asset> findAssetById(Long id);

    Asset saveAsset(Asset asset);

    void deleteAsset(Long id);

    List<Asset> searchAssets(String keyword);

    List<Asset> filterByStatus(AssetStatus status);

    List<Asset> filterByCondition(AssetCondition condition);

    List<Asset> findBorrowableAssets();
}
