package com.fittrack.inventoryapp.repository;

import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.entity.AssetMovement;
import com.fittrack.inventoryapp.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {

    List<AssetMovement> findByAsset(Asset asset);

    List<AssetMovement> findByUser(User user);

    // Find active movements (where the asset is currently borrowed)
    List<AssetMovement> findByReturnedDateIsNull();

    // Find history of an asset ordered by date descending
    List<AssetMovement> findByAssetOrderByBorrowedDateDesc(Asset asset);
}
