package com.fittrack.inventoryapp.controller;

import com.fittrack.inventoryapp.model.enums.AssetStatus;
import com.fittrack.inventoryapp.service.AssetService;
import com.fittrack.inventoryapp.service.MovementService;
import com.fittrack.inventoryapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AssetService assetService;
    private final UserService userService;
    private final MovementService movementService;

    public AdminController(AssetService assetService,
            UserService userService,
            MovementService movementService) {
        this.assetService = assetService;
        this.userService = userService;
        this.movementService = movementService;
    }

    @GetMapping
    public String adminPanel(Model model) {
        model.addAttribute("totalAssets", assetService.findAllAssets().size());
        model.addAttribute("availableAssets", assetService.filterByStatus(AssetStatus.AVAILABLE).size());
        model.addAttribute("inUseAssets", assetService.filterByStatus(AssetStatus.IN_USE).size());
        model.addAttribute("reservedAssets", assetService.filterByStatus(AssetStatus.RESERVED).size());
        model.addAttribute("maintenanceAssets", assetService.filterByStatus(AssetStatus.MAINTENANCE).size());
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("activeMovements",
                movementService.getAllMovements(Sort.by(Sort.Direction.DESC, "borrowedDate")).stream()
                        .filter(m -> m.getReturnedDate() == null).count());
        model.addAttribute("allMovements",
                movementService.getAllMovements(Sort.by(Sort.Direction.DESC, "borrowedDate")));
        model.addAttribute("pendingManagerRequests", userService.getPendingManagerRequests());
        return "admin/panel";
    }

    @org.springframework.web.bind.annotation.PostMapping("/users/{id}/role")
    public String changeUserRole(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam com.fittrack.inventoryapp.model.enums.Role role,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails currentUser,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            com.fittrack.inventoryapp.model.entity.User targetUser = userService.findAllUsers().stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

            if (targetUser.getUsername().equals(currentUser.getUsername())) {
                redirectAttributes.addFlashAttribute("errorMessage", "error.admin.role_self_change");
            } else {
                userService.changeUserRole(id, role);
                redirectAttributes.addFlashAttribute("successMessage", "alert.success.role_updated");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.admin.role_update_failed");
        }

        return "redirect:/admin";
    }

    @org.springframework.web.bind.annotation.PostMapping("/requests/{id}/approve")
    public String approveManagerRequest(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            userService.approveManagerRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.manager_approved");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.user.not_found");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin";
    }

    @org.springframework.web.bind.annotation.PostMapping("/requests/{id}/reject")
    public String rejectManagerRequest(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            userService.rejectManagerRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.manager_rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.movement.not_found");
        }
        return "redirect:/admin";
    }
}
