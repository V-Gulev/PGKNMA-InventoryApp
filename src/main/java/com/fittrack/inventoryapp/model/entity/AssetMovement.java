package com.fittrack.inventoryapp.model.entity;

import com.fittrack.inventoryapp.model.enums.MovementStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AssetMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull(message = "Asset is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Borrowed date is required")
    @Column(name = "borrowed_date", nullable = false)
    private LocalDate borrowedDate;

    @NotNull(message = "Expected return date is required")
    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "returned_date")
    private LocalDateTime returnedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @NotNull(message = "Movement status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MovementStatus status = MovementStatus.PENDING;
}
