package com.fittrack.inventoryapp.controller;

import com.fittrack.inventoryapp.exception.DuplicateResourceException;
import com.fittrack.inventoryapp.model.entity.Asset;
import com.fittrack.inventoryapp.model.entity.AssetMovement;
import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.AssetStatus;
import com.fittrack.inventoryapp.model.enums.Role;
import com.fittrack.inventoryapp.service.AssetService;
import com.fittrack.inventoryapp.service.MovementService;
import com.fittrack.inventoryapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/assets")
public class AssetController {

    private final AssetService assetService;
    private final MovementService movementService;
    private final UserService userService;

    public AssetController(AssetService assetService, MovementService movementService, UserService userService) {
        this.assetService = assetService;
        this.movementService = movementService;
        this.userService = userService;
    }

    @GetMapping
    public String listAssets(@RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) AssetStatus status,
            @RequestParam(value = "condition", required = false) AssetCondition condition,
            Model model) {
        List<Asset> assets;

        if (status != null && condition != null) {
            assets = assetService.filterByStatus(status).stream()
                    .filter(a -> a.getItemCondition() == condition)
                    .toList();
        } else if (status != null) {
            assets = assetService.filterByStatus(status);
        } else if (condition != null) {
            assets = assetService.filterByCondition(condition);
        } else if (keyword != null && !keyword.isBlank()) {
            assets = assetService.searchAssets(keyword);
        } else {
            assets = assetService.findAllAssets();
        }

        model.addAttribute("assets", assets);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCondition", condition);
        model.addAttribute("statuses", AssetStatus.values());
        model.addAttribute("conditions", AssetCondition.values());
        return "assets/asset-list";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("asset", new Asset());
        model.addAttribute("conditions", AssetCondition.values());
        model.addAttribute("statuses", AssetStatus.values());
        return "assets/asset-form";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        return assetService.findAssetById(id)
                .map(asset -> {
                    model.addAttribute("asset", asset);
                    model.addAttribute("conditions", AssetCondition.values());
                    model.addAttribute("statuses", AssetStatus.values());
                    return "assets/asset-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Asset not found");
                    return "redirect:/assets";
                });
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/save")
    public String saveAsset(@Valid @ModelAttribute("asset") Asset asset,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("conditions", AssetCondition.values());
            model.addAttribute("statuses", AssetStatus.values());
            return "assets/asset-form";
        }

        try {
            if (asset.getId() == null) {
                // New asset
                if (currentUser != null) {
                    userService.findByUsername(currentUser.getUsername())
                            .ifPresent(asset::setAddedBy);
                }
            } else {
                // Existing asset, preserve addedBy and createdAt
                assetService.findAssetById(asset.getId()).ifPresent(existing -> {
                    asset.setAddedBy(existing.getAddedBy());
                    asset.setCreatedAt(existing.getCreatedAt());
                });
            }
            assetService.saveAsset(asset);
        } catch (DuplicateResourceException e) {
            model.addAttribute("duplicateError", e.getMessage());
            model.addAttribute("conditions", AssetCondition.values());
            model.addAttribute("statuses", AssetStatus.values());
            return "assets/asset-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "alert.success.asset_saved");
        return "redirect:/assets";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/{id}/delete")
    public String deleteAsset(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        assetService.deleteAsset(id);
        redirectAttributes.addFlashAttribute("successMessage", "alert.success.asset_deleted");
        return "redirect:/assets";
    }

    @GetMapping("/{id}/history")
    public String showAssetHistory(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        return assetService.findAssetById(id)
                .map(asset -> {
                    model.addAttribute("asset", asset);
                    List<AssetMovement> movements = movementService.getHistoryByAsset(id);
                    if (currentUser != null) {
                        User loggedIn = userService.findByUsername(currentUser.getUsername()).orElseThrow();
                        if (loggedIn.getRole() == Role.STUDENT) {
                            movements = movements.stream()
                                    .filter(m -> m.getUser().getUsername().equals(loggedIn.getUsername()))
                                    .toList();
                        }
                    }
                    model.addAttribute("movements", movements);
                    return "movements/movement-list";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Asset not found");
                    return "redirect:/assets";
                });
    }

    @GetMapping("/{id}")
    public String showAssetDetails(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        return assetService.findAssetById(id)
                .map(asset -> {
                    model.addAttribute("asset", asset);
                    List<AssetMovement> movements = movementService.getHistoryByAsset(id);
                    if (currentUser != null) {
                        User loggedIn = userService.findByUsername(currentUser.getUsername()).orElseThrow();
                        if (loggedIn.getRole() == Role.STUDENT) {
                            movements = movements.stream()
                                    .filter(m -> m.getUser().getUsername().equals(loggedIn.getUsername()))
                                    .toList();
                        }
                    }
                    model.addAttribute("recentMovements",
                            movements.stream().limit(5).toList());
                    return "assets/asset-details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Asset not found");
                    return "redirect:/assets";
                });
    }
}
