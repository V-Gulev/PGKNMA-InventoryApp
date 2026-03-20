package com.fittrack.inventoryapp.service.impl;

import com.fittrack.inventoryapp.exception.DuplicateResourceException;
import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;
import com.fittrack.inventoryapp.repository.AssetRepository;
import com.fittrack.inventoryapp.service.AssetService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetServiceImpl implements AssetService {
    private final AssetRepository assetRepository;

    public AssetServiceImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public List<Asset> findAllAssets() {
        return assetRepository.findAll();
    }

    @Override
    public Optional<Asset> findAssetById(Long id) {
        return assetRepository.findById(id);
    }

    @Override
    public Asset saveAsset(Asset asset) {
        // Check for duplicate inventory number
        assetRepository.findByInventoryNumber(asset.getInventoryNumber())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(asset.getId())) {
                        throw new DuplicateResourceException("error.asset.inventory_taken");
                    }
                });

        if (asset.getId() == null && asset.getStatus() == null) {
            asset.setStatus(AssetStatus.AVAILABLE);
        }
        return assetRepository.save(asset);
    }

    @Override
    public void deleteAsset(Long id) {
        assetRepository.deleteById(id);
    }

    @Override
    public List<Asset> searchAssets(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return assetRepository.searchByKeyword(keyword.trim());
        }
        return assetRepository.findAll();
    }

    @Override
    public List<Asset> filterByStatus(AssetStatus status) {
        return assetRepository.findByStatus(status);
    }

    @Override
    public List<Asset> filterByCondition(AssetCondition condition) {
        return assetRepository.findByItemCondition(condition);
    }

    @Override
    public List<Asset> findBorrowableAssets() {
        return assetRepository.findByStatusIn(List.of(AssetStatus.AVAILABLE, AssetStatus.RESERVED));
    }
}