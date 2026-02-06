package com.fittrack.inventoryapp.model.entity;

import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

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

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

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
}
