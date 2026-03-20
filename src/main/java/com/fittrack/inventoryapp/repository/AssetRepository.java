package com.fittrack.inventoryapp.repository;

import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByInventoryNumber(String inventoryNumber);

    boolean existsByInventoryNumber(String inventoryNumber);

    List<Asset> findByStatus(AssetStatus status);

    List<Asset> findByStatusIn(List<AssetStatus> statuses);

    List<Asset> findByItemCondition(AssetCondition itemCondition);

    List<Asset> findByModelNameContainingIgnoreCase(String modelName);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Asset a WHERE " +
            "LOWER(a.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.inventoryNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Asset> searchByKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword);
}
