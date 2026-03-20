package com.fittrack.inventoryapp.service;

import com.fittrack.inventoryapp.model.entity.AssetMovement;
import com.fittrack.inventoryapp.model.enums.AssetCondition;

import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

public interface MovementService {
    void borrowAsset(Long assetId, String username, LocalDate borrowedDate);

    void approveBorrow(Long movementId);

    void rejectBorrow(Long movementId, String notes);

    void returnAsset(Long assetId, String notes, AssetCondition newCondition);

    void cancelBorrow(Long assetId);

    List<AssetMovement> getHistoryByAsset(Long assetId);

    List<AssetMovement> getMyActiveItems(String username);

    List<AssetMovement> getPendingMovements();

    List<AssetMovement> getAllMovements(Sort sort);

    List<AssetMovement> searchMovements(String keyword, String status, Sort sort);
}
