package com.fittrack.inventoryapp.model.entity;

import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Inventory number is required")
    @Column(name = "inventory_number", nullable = false, unique = true)
    private String inventoryNumber;

    @Column(name = "serial_number")
    private String serialNumber;

    @NotBlank(message = "Model name is required")
    @Column(name = "model_name", nullable = false)
    private String modelName;

    private String category;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @NotNull(message = "Item condition is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private AssetCondition itemCondition;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "asset_images", joinColumns = @JoinColumn(name = "asset_id"))
    @Column(name = "image_url")
    @Builder.Default
    private java.util.List<String> imageUrls = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;
}
