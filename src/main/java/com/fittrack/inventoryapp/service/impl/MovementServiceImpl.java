package com.fittrack.inventoryapp.service.impl;

import com.fittrack.inventoryapp.exception.AssetNotAvailableException;
import com.fittrack.inventoryapp.exception.ResourceNotFoundException;
import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.entity.AssetMovement;
import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;
import com.fittrack.inventoryapp.model.enums.MovementStatus;
import com.fittrack.inventoryapp.repository.AssetMovementRepository;
import com.fittrack.inventoryapp.repository.AssetRepository;
import com.fittrack.inventoryapp.repository.UserRepository;
import com.fittrack.inventoryapp.service.MovementService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovementServiceImpl implements MovementService {
    private final AssetMovementRepository movementRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    public MovementServiceImpl(AssetMovementRepository movementRepository, AssetRepository assetRepository,
            UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void borrowAsset(Long assetId, String username, LocalDate borrowedDate) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("error.asset.not_found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.not_found"));

        if (asset.getStatus() != AssetStatus.AVAILABLE && asset.getStatus() != AssetStatus.RESERVED) {
            throw new AssetNotAvailableException("error.asset.in_use");
        }

        // If the asset is RESERVED (scheduled for future), cancel the existing
        // scheduled borrow
        if (asset.getStatus() == AssetStatus.RESERVED) {
            movementRepository.findByAssetIdAndReturnedDateIsNull(assetId)
                    .ifPresent(existingMovement -> {
                        existingMovement.setReturnedDate(LocalDateTime.now());
                        existingMovement.setNotes("Cancelled — asset re-borrowed by " + username);
                        movementRepository.save(existingMovement);
                    });
        }

        // Determine status based on the *initiator's* Role, not the target user's Role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManagerOrAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        MovementStatus initialStatus = MovementStatus.PENDING;
        if (isManagerOrAdmin) {
            initialStatus = MovementStatus.APPROVED;
        }

        AssetMovement movement = AssetMovement.builder()
                .asset(asset)
                .user(user)
                .borrowedDate(borrowedDate)
                .status(initialStatus)
                .build();

        movementRepository.save(movement);

        if (initialStatus == MovementStatus.PENDING) {
            asset.setStatus(AssetStatus.PENDING_APPROVAL);
        } else {
            if (borrowedDate.isAfter(LocalDate.now())) {
                asset.setStatus(AssetStatus.RESERVED);
            } else {
                asset.setStatus(AssetStatus.IN_USE);
            }
        }
        assetRepository.save(asset);
    }

    @Override
    @Transactional
    public void approveBorrow(Long movementId) {
        AssetMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("error.movement.not_found"));

        if (movement.getStatus() != MovementStatus.PENDING) {
            throw new IllegalStateException("error.movement.only_pending_approve");
        }

        movement.setStatus(MovementStatus.APPROVED);
        movementRepository.save(movement);

        Asset asset = movement.getAsset();
        if (movement.getBorrowedDate().isAfter(LocalDate.now())) {
            asset.setStatus(AssetStatus.RESERVED);
        } else {
            asset.setStatus(AssetStatus.IN_USE);
        }
        assetRepository.save(asset);
    }

    @Override
    @Transactional
    public void rejectBorrow(Long movementId, String notes) {
        AssetMovement movement = movementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("error.movement.not_found"));

        if (movement.getStatus() != MovementStatus.PENDING) {
            throw new IllegalStateException("error.movement.only_pending_reject");
        }

        movement.setStatus(MovementStatus.REJECTED);
        movement.setReturnedDate(LocalDateTime.now());
        movement.setNotes(notes != null ? notes : "Rejected by manager");
        movementRepository.save(movement);

        Asset asset = movement.getAsset();
        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);
    }

    @Override
    @Transactional
    public void returnAsset(Long assetId, String notes, AssetCondition newCondition) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("error.asset.not_found"));

        AssetMovement movement = movementRepository.findByAssetIdAndReturnedDateIsNull(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("error.movement.no_active_borrow"));

        if (movement.getBorrowedDate().isAfter(LocalDate.now())) {
            throw new AssetNotAvailableException("error.asset.return_scheduled_not_started");
        }

        movement.setReturnedDate(LocalDateTime.now());
        movement.setNotes(notes);
        movementRepository.save(movement);

        if (newCondition != null) {
            asset.setItemCondition(newCondition);
        }
        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);
    }

    @Override
    @Transactional
    public void cancelBorrow(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("error.asset.not_found"));

        AssetMovement movement = movementRepository.findByAssetIdAndReturnedDateIsNull(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("error.movement.no_active_borrow"));

        if (!movement.getBorrowedDate().isAfter(LocalDate.now())) {
            throw new AssetNotAvailableException("error.asset.cancel_only_scheduled");
        }

        movement.setReturnedDate(LocalDateTime.now());
        movement.setNotes("Cancelled");
        movementRepository.save(movement);

        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);
    }

    @Override
    public List<AssetMovement> getHistoryByAsset(Long assetId) {
        return movementRepository.findByAssetIdOrderByBorrowedDateDesc(assetId);
    }

    @Override
    public List<AssetMovement> getMyActiveItems(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.not_found"));

        // Show approved + pending movements (not REJECTED)
        return movementRepository.findByUserIdAndReturnedDateIsNullAndStatusIn(
                user.getId(), List.of(MovementStatus.APPROVED, MovementStatus.PENDING));
    }

    @Override
    public List<AssetMovement> getPendingMovements() {
        return movementRepository.findAll().stream()
                .filter(m -> m.getStatus() == MovementStatus.PENDING)
                .toList();
    }

    @Override
    public List<AssetMovement> getAllMovements(Sort sort) {
        return movementRepository.findAll(sort);
    }

    @Override
    public List<AssetMovement> searchMovements(String keyword, String status, Sort sort) {
        Boolean isReturned = null;
        if ("returned".equalsIgnoreCase(status)) {
            isReturned = true;
        } else if ("active".equalsIgnoreCase(status)) {
            isReturned = false;
        }

        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

        return movementRepository.searchAndFilterMovements(searchKeyword, isReturned, sort);
    }
}