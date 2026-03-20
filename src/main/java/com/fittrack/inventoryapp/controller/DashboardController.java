package com.fittrack.inventoryapp.controller;

import com.fittrack.inventoryapp.model.enums.AssetStatus;
import com.fittrack.inventoryapp.service.AssetService;
import com.fittrack.inventoryapp.service.MovementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
public class DashboardController {

    private final AssetService assetService;
    private final MovementService movementService;
    private final com.fittrack.inventoryapp.service.UserService userService;

    public DashboardController(AssetService assetService, MovementService movementService,
            com.fittrack.inventoryapp.service.UserService userService) {
        this.assetService = assetService;
        this.movementService = movementService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String showDashboard(Model model,
            @AuthenticationPrincipal UserDetails currentUser) {

        long totalAssets = assetService.findAllAssets().size();
        long availableAssets = assetService.filterByStatus(AssetStatus.AVAILABLE).size();
        long inUseAssets = assetService.filterByStatus(AssetStatus.IN_USE).size();
        long reservedAssets = assetService.filterByStatus(AssetStatus.RESERVED).size();

        List<com.fittrack.inventoryapp.model.entity.Asset> maintenanceItems = assetService
                .filterByStatus(AssetStatus.MAINTENANCE);
        long maintenanceAssets = maintenanceItems.size();

        model.addAttribute("totalAssets", totalAssets);
        model.addAttribute("availableAssets", availableAssets);
        model.addAttribute("inUseAssets", inUseAssets);
        model.addAttribute("reservedAssets", reservedAssets);
        model.addAttribute("maintenanceAssets", maintenanceAssets);

        if (currentUser != null) {
            com.fittrack.inventoryapp.model.entity.User loggedIn = userService.findByUsername(currentUser.getUsername())
                    .orElseThrow();
            model.addAttribute("currentUser", loggedIn);
            model.addAttribute("currentUserRole", loggedIn.getRole().name());

            model.addAttribute("myBorrowedItems",
                    movementService.getMyActiveItems(currentUser.getUsername()).size());

            if (loggedIn.getRole() == com.fittrack.inventoryapp.model.enums.Role.STUDENT) {
                // Students see their own recent movements
                List<com.fittrack.inventoryapp.model.entity.AssetMovement> myMovements = movementService
                        .searchMovements(loggedIn.getUsername(), "all", Sort.by(Sort.Direction.DESC, "borrowedDate"));
                model.addAttribute("recentMovements", myMovements.stream().limit(5).toList());
            } else {
                // Admins and Teachers see all recent movements
                List<com.fittrack.inventoryapp.model.entity.AssetMovement> allMovements = movementService
                        .getAllMovements(Sort.by(Sort.Direction.DESC, "borrowedDate"));
                model.addAttribute("recentMovements", allMovements.stream().limit(5).toList());
                model.addAttribute("maintenanceItemsList", maintenanceItems);
            }
        }

        return "dashboard";
    }
}
