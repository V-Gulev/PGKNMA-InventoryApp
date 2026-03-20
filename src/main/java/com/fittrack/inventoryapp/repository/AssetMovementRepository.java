package com.fittrack.inventoryapp.repository;

import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.entity.AssetMovement;
import com.fittrack.inventoryapp.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetMovementRepository extends JpaRepository<AssetMovement, Long> {

        List<AssetMovement> findByAsset(Asset asset);

        List<AssetMovement> findByUser(User user);

        List<AssetMovement> findByReturnedDateIsNull();

        List<AssetMovement> findByAssetOrderByBorrowedDateDesc(Asset asset);

        List<AssetMovement> findByAssetIdOrderByBorrowedDateDesc(Long assetId);

        Optional<AssetMovement> findByAssetIdAndReturnedDateIsNull(Long assetId);

        List<AssetMovement> findByUserIdAndReturnedDateIsNull(Long userId);

        List<AssetMovement> findByUserIdAndReturnedDateIsNullAndStatusIn(Long userId,
                        java.util.Collection<com.fittrack.inventoryapp.model.enums.MovementStatus> statuses);

        @org.springframework.data.jpa.repository.Query("""
                            SELECT m FROM AssetMovement m
                            WHERE (:keyword IS NULL OR LOWER(m.asset.modelName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(m.asset.inventoryNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(m.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
                              AND (:isReturned IS NULL OR
                                   (:isReturned = true AND m.returnedDate IS NOT NULL) OR
                                   (:isReturned = false AND m.returnedDate IS NULL))
                        """)
        List<AssetMovement> searchAndFilterMovements(
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        @org.springframework.data.repository.query.Param("isReturned") Boolean isReturned,
                        org.springframework.data.domain.Sort sort);

}
